package com.nadlanitica.landtender;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.AbstractBufferingClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * עוקף את java.net.http.HttpClient לקריאות ל-apps.land.gov.il בלבד: שרת ה-IIS שמאחורי ה-API
 * מחזיר שגיאה כללית ("ארעה תקלה") לבקשות שמגיעות מ-Java (ככל הנראה זיהוי לפי טביעת אצבע TLS/HTTP) -
 * אומת שאותה בקשה בדיוק מצליחה עם curl ועם PowerShell/.NET, ולכן מנותב דרך curl.exe המובנה בווינדוס.
 * ponytail: workaround ספציפי למקור נתונים אחד, לא פתרון TLS גנרי - אם ה-API ישתנה או curl לא יהיה זמין,
 * זה יזרוק IOException ברור.
 */
class CurlClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
        return new CurlRequest(uri, httpMethod);
    }

    private static class CurlRequest extends AbstractBufferingClientHttpRequest {
        private final URI uri;
        private final HttpMethod method;

        CurlRequest(URI uri, HttpMethod method) {
            this.uri = uri;
            this.method = method;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
            // --max-time/--connect-timeout הם ההגנה האמיתית מפני תקיעה - בלעדיהם, שרת שלא עונה תוקע את
            // ה-readAllBytes() החוסם למטה לנצח, וה-waitFor(20s) בסוף אף פעם לא נבדק כי אין EOF על stdout.
            List<String> command = new ArrayList<>(List.of("curl.exe", "-s", "-S", "--connect-timeout", "10", "--max-time", "20",
                    "-X", method.name(), "-w", "\nHTTP_STATUS:%{http_code}"));
            headers.forEach((name, values) -> values.forEach(value -> {
                command.add("-H");
                command.add(name + ": " + value);
            }));
            if (bufferedOutput.length > 0) {
                command.add("--data-binary");
                command.add("@-");
            }
            command.add(uri.toString());

            Process process = new ProcessBuilder(command).start();
            if (bufferedOutput.length > 0) {
                process.getOutputStream().write(bufferedOutput);
            }
            process.getOutputStream().close();
            byte[] stdout = process.getInputStream().readAllBytes();
            byte[] stderr = process.getErrorStream().readAllBytes();
            boolean finished;
            try {
                finished = process.waitFor(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("curl הופרע באמצע הריצה", e);
            }
            if (!finished || process.exitValue() != 0) {
                throw new IOException("curl נכשל: " + new String(stderr, java.nio.charset.StandardCharsets.UTF_8));
            }

            // חיפוש ברמת בייטים, לא כמחרוזת UTF-8 - הגוף עצמו עשוי להיות בינארי (למשל PDF),
            // ופענוח/קידוד UTF-8 הלוך-חזור על בייטים לא-תקינים הורס אותם באופן בלתי הפיך.
            byte[] marker = "HTTP_STATUS:".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            int markerIndex = lastIndexOf(stdout, marker);
            if (markerIndex < 0) {
                throw new IOException("curl לא החזיר קוד סטטוס");
            }
            String statusText = new String(stdout, markerIndex + marker.length, stdout.length - markerIndex - marker.length,
                    java.nio.charset.StandardCharsets.US_ASCII).trim();
            int statusCode = Integer.parseInt(statusText);
            int bodyEnd = markerIndex;
            if (bodyEnd > 0 && stdout[bodyEnd - 1] == '\n') {
                bodyEnd--;
            }
            byte[] bodyBytes = java.util.Arrays.copyOfRange(stdout, 0, bodyEnd);
            return new CurlResponse(HttpStatusCode.valueOf(statusCode), bodyBytes);
        }

        private static int lastIndexOf(byte[] haystack, byte[] needle) {
            outer:
            for (int i = haystack.length - needle.length; i >= 0; i--) {
                for (int j = 0; j < needle.length; j++) {
                    if (haystack[i + j] != needle[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
    }

    private static class CurlResponse implements ClientHttpResponse {
        private final HttpStatusCode statusCode;
        private final byte[] body;

        CurlResponse(HttpStatusCode statusCode, byte[] body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return statusCode;
        }

        @Override
        public String getStatusText() {
            return "";
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            return headers;
        }
    }
}
