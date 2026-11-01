package com.nadlanitica.leads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** ליד נשמר רק עם נייד ישראלי ואימייל תקינים; הנייד נשמר בצורה אחידה שנוח לחייג אליה. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeadValidationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;
    @Autowired
    MortgageLeadRepository leads;

    @Test
    void validLeadIsSavedWithANormalizedMobile() throws Exception {
        var res = post("+972 52-765 4321", "valid.lead@walla.co.il", "10.0.0.1");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(leads.findAll()).anySatisfy(l -> assertThat(l.getPhone()).isEqualTo("0527654321"));
    }

    @Test
    void landlineOrShortNumberIsRejected() throws Exception {
        assertBad(post("03-1234567", "a@b.com", "10.0.0.2"), "מספר נייד לא תקין");
        assertBad(post("050-12345", "a@b.com", "10.0.0.3"), "מספר נייד לא תקין");
    }

    @Test
    void missingOrMalformedEmailIsRejected() throws Exception {
        assertBad(post("050-1234567", "", "10.0.0.4"), "כתובת אימייל לא תקינה");
        assertBad(post("050-1234567", "name@gmail", "10.0.0.5"), "כתובת אימייל לא תקינה");
        assertBad(post("050-1234567", "name@gmail..com", "10.0.0.6"), "כתובת אימייל לא תקינה");
    }

    private void assertBad(HttpResponse<String> res, String message) {
        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(res.body()).contains(message);
    }

    private HttpResponse<String> post(String phone, String email, String clientIp) throws Exception {
        String json = "{\"fullName\":\"בדיקה\",\"phone\":\"" + phone + "\",\"email\":\"" + email + "\"}";
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/leads"))
                        .header("Content-Type", "application/json")
                        .header("X-Forwarded-For", clientIp)
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
