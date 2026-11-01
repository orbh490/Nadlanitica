package com.nadlanitica.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** עמודים מקבלים CSP וכותרות הגנה; תשובות API מקבלות את הכותרות בלי CSP (כדי לא לשבור את צופה ה-PDF). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHeadersTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Test
    void pagesGetCspAndProtectionHeaders() throws Exception {
        var headers = get("/home.html").headers();
        assertThat(headers.firstValue("Content-Security-Policy")).hasValue(SecurityHeadersFilter.CSP);
        assertThat(headers.firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(headers.firstValue("X-Frame-Options")).hasValue("DENY");
        assertThat(headers.firstValue("Referrer-Policy")).isPresent();
        // HSTS רק מעל https - כאן הבקשה ב-http
        assertThat(headers.firstValue("Strict-Transport-Security")).isEmpty();
    }

    @Test
    void httpsBehindTheProxyGetsHsts() throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/home.html"))
                .header("X-Forwarded-Proto", "https").build();
        var res = client.send(req, HttpResponse.BodyHandlers.discarding());
        assertThat(res.headers().firstValue("Strict-Transport-Security")).hasValue("max-age=31536000");
    }

    @Test
    void apiResponsesSkipCsp() throws Exception {
        var headers = get("/api/lottery/projects").headers();
        assertThat(headers.firstValue("Content-Security-Policy")).isEmpty();
        assertThat(headers.firstValue("X-Content-Type-Options")).hasValue("nosniff");
    }

    private HttpResponse<Void> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.discarding());
    }
}
