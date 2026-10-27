package com.nadlanitica.leads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * רץ מול Tomcat אמיתי (לא MockMvc) כי RemoteIpValve הוא חלק מ-Tomcat. הבקשות מגיעות מ-127.0.0.1,
 * שנחשב proxy פנימי - בדיוק כמו ה-proxy של שרת האחסון בפרודקשן.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeadRateLimitBehindProxyTest {

    private static final int LIMIT = 5;
    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Test
    void eachForwardedClientGetsItsOwnLimit() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            assertThat(submitLeadAs("203.0.113.10")).isEqualTo(200);
        }
        assertThat(submitLeadAs("203.0.113.10")).isEqualTo(429);
        assertThat(submitLeadAs("203.0.113.20")).isEqualTo(200);
    }

    @Test
    void clientCannotBypassLimitBySpoofingForwardedFor() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            assertThat(submitLeadAs("198.51.100." + i + ", 203.0.113.30")).isEqualTo(200);
        }
        // הערך הימני הוא מה שה-proxy הוסיף בעצמו - שינוי הערכים משמאל (בשליטת הלקוח) לא מאפס את המונה
        assertThat(submitLeadAs("198.51.100.99, 203.0.113.30")).isEqualTo(429);
    }

    private int submitLeadAs(String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/leads"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString("{\"fullName\":\"בדיקה\",\"phone\":\"0500000000\"}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
