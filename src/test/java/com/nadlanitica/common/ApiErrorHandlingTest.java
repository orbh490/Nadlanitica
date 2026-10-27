package com.nadlanitica.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** כל קלט שגוי ל-API מחזיר 400 עם הודעה בעברית - לא 500, לא 200 עם Infinity, ולא גוף שגיאה גנרי באנגלית. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiErrorHandlingTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Test
    void validCalculationStillWorks() throws Exception {
        var res = post("/api/equity/calculate", """
                {"availableEquity":500000,"monthlyIncome":20000,"category":"SINGLE","marketValue":2000000,
                 "purchasePrice":2000000,"subsidized":false,"usedBroker":false}""");
        assertThat(res.statusCode()).isEqualTo(200);
    }

    @Test
    void missingFieldReturnsFieldMessage() throws Exception {
        var res = post("/api/equity/calculate", """
                {"availableEquity":500000,"monthlyIncome":20000,"marketValue":2000000,
                 "purchasePrice":2000000,"subsidized":false,"usedBroker":false}""");
        assertBadRequest(res, "יש לבחור סוג רכישה");
    }

    @Test
    void missingNumericFieldIsStillAHebrewBadRequest() throws Exception {
        // Jackson 3 דוחה שדה מספרי חסר עוד לפני הוולידציה - עדיין 400 בעברית, רק עם ההודעה הכללית
        var res = post("/api/equity/calculate", "{\"category\":\"SINGLE\",\"purchasePrice\":2000000}");
        assertBadRequest(res, "הבקשה אינה תקינה");
    }

    @Test
    void negativeAmountIsRejected() throws Exception {
        var res = post("/api/costs/waiting-period-projection",
                "{\"purchasePrice\":-5,\"assumedAnnualIndexRatePercent\":3,\"waitYears\":2}");
        assertBadRequest(res, "מחיר הדירה לא יכול להיות שלילי");
    }

    @Test
    void trackMixWithZeroTermIsRejectedInsteadOfInfinity() throws Exception {
        var res = post("/api/mortgage/track-mix", """
                {"loanAmount":1000000,"termYears":0,"fixedNonIndexedPercent":50,"cpiLinkedPercent":50,"primePercent":0}""");
        assertBadRequest(res, "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה");
    }

    @Test
    void trackMixPercentagesMustSumTo100() throws Exception {
        var res = post("/api/mortgage/track-mix", """
                {"loanAmount":1000000,"termYears":25,"fixedNonIndexedPercent":100,"cpiLinkedPercent":100,"primePercent":100}""");
        assertBadRequest(res, "האחוזים של המסלולים חייבים להסתכם ל-100%");
    }

    @Test
    void malformedJsonAndUnknownCategoryAreBadRequests() throws Exception {
        assertBadRequest(post("/api/equity/calculate", "{bad"), "הבקשה אינה תקינה");
        assertBadRequest(post("/api/dashboard", "{\"category\":\"PALACE\",\"purchasePrice\":1}"), "הבקשה אינה תקינה");
    }

    @Test
    void missingTokensNoLongerCauseServerErrors() throws Exception {
        assertBadRequest(post("/api/auth/verify-confirm", "{}"), "הקישור אינו תקין");
        assertBadRequest(post("/api/auth/reset-password", "{\"newPassword\":\"abcdefg\"}"), "הקישור אינו תקין");
    }

    @Test
    void overlongPasswordIsRejected() throws Exception {
        var res = post("/api/auth/register",
                "{\"email\":\"a@example.test\",\"password\":\"" + "x".repeat(100) + "\",\"consent\":true}");
        assertBadRequest(res, "הסיסמה ארוכה מדי");
    }

    @Test
    void wrongHttpMethodKeepsItsStatus() throws Exception {
        // 405 נזרק לפני שנבחר controller, ולכן לא עובר ב-ApiExceptionHandler (שמוגבל ל-@RestController) -
        // חשוב רק שהסטטוס נשמר ולא הופך ל-500
        var res = client.send(HttpRequest.newBuilder(uri("/api/equity/calculate")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(405);
    }

    @Test
    void missingPageIsNotTurnedIntoJson() throws Exception {
        var res = client.send(HttpRequest.newBuilder(uri("/no-such-page.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(404);
        assertThat(res.body()).doesNotContain("\"message\"");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static void assertBadRequest(HttpResponse<String> res, String expectedMessage) {
        assertThat(res.statusCode()).as(res.body()).isEqualTo(400);
        assertThat(res.body()).contains("\"message\"").contains(expectedMessage);
    }
}
