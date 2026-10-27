package com.nadlanitica.landtender;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * לקוח ל-API הציבורי (לא רשמי, אך פתוח וללא אימות) של רשות מקרקעי ישראל למכרזי קרקע.
 * מקור: https://apps.land.gov.il/MichrazimSite/ - נבדק ואומת ידנית מול הנתונים החיים.
 * הגוף נשלח מחוצץ (עם Content-Length) ולא ב-chunked - שרת ה-IIS של רמ"י מחזיר "ארעה תקלה" לגוף chunked.
 * זמן קריאה ארוך כי חוברות מכרז הן PDF של ~9MB שלוקחות מעל 20 שניות להורדה.
 */
@Component
class LandTenderApiClient {

    private static final String BASE_URL = "https://apps.land.gov.il/MichrazimSite/api";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .requestFactory(requestFactory())
            .defaultHeader("User-Agent", "datagov-external-client")
            .defaultHeader("Origin", "https://apps.land.gov.il")
            .defaultHeader("Referer", "https://apps.land.gov.il/MichrazimSite/")
            .build();

    record RmiTenderSummary(
            @JsonProperty("MichrazID") long michrazId,
            @JsonProperty("MichrazName") String michrazName,
            @JsonProperty("KodYeudMichraz") Integer kodYeudMichraz,
            @JsonProperty("KodYeshuv") Integer kodYeshuv,
            @JsonProperty("YechidotDiur") Integer yechidotDiur,
            @JsonProperty("Shchuna") String shchuna,
            @JsonProperty("PirsumDate") String pirsumDate,
            @JsonProperty("PtichaDate") String ptichaDate,
            @JsonProperty("SgiraDate") String sgiraDate,
            @JsonProperty("VaadaDate") String vaadaDate,
            @JsonProperty("PublishedChoveret") Boolean publishedChoveret) {
    }

    record RmiTik(
            @JsonProperty("TikID") String tikId,
            @JsonProperty("MitchamName") String mitchamName,
            @JsonProperty("Shetach") Double shetach,
            @JsonProperty("mechirShuma") Double mechirShuma,
            @JsonProperty("MechirSaf") Double mechirSaf,
            @JsonProperty("SchumArvut") Double schumArvut,
            @JsonProperty("HotzaotPituach") Double hotzaotPituach,
            @JsonProperty("ShemZoche") String shemZoche,
            @JsonProperty("SchumZchiya") Double schumZchiya) {
    }

    record RmiDocument(
            @JsonProperty("MichrazID") long michrazId,
            @JsonProperty("Size") Long size,
            @JsonProperty("FileType") String fileType,
            @JsonProperty("RowID") long rowId,
            @JsonProperty("Teur") String teur,
            @JsonProperty("DocName") String docName,
            @JsonProperty("UpdateDate") String updateDate,
            @JsonProperty("Url") String url,
            @JsonProperty("MahutMismachID") long mahutMismachId,
            @JsonProperty("PirsumType") int pirsumType) {
    }

    record RmiTenderDetail(
            @JsonProperty("MichrazID") long michrazId,
            @JsonProperty("KodYeudMichraz") Integer kodYeudMichraz,
            @JsonProperty("KodYeshuv") Integer kodYeshuv,
            @JsonProperty("Shchuna") String shchuna,
            @JsonProperty("PtichaDate") String ptichaDate,
            @JsonProperty("SgiraDate") String sgiraDate,
            @JsonProperty("Uchlusiyot") List<Integer> uchlusiyot,
            @JsonProperty("Tik") List<RmiTik> tik,
            @JsonProperty("MichrazDocList") List<RmiDocument> michrazDocList) {
    }

    List<RmiTenderSummary> fetchActiveTenders() {
        return searchTenders(true);
    }

    /** מכרזים סגורים (StatusMichraz שאינו "פעיל") - חלקם עם תוצאות (זוכה+מחיר סופי), חלקם עדיין ללא. */
    List<RmiTenderSummary> fetchClosedTenders() {
        return searchTenders(false);
    }

    private List<RmiTenderSummary> searchTenders(boolean active) {
        RmiTenderSummary[] result = restClient.post()
                .uri("/SearchApi/Search")
                .body(Map.of("ActiveQuickSearch", false, "ActiveMichraz", active))
                .retrieve()
                .body(RmiTenderSummary[].class);
        return result == null ? List.of() : List.of(result);
    }

    RmiTenderDetail fetchTenderDetail(long michrazId) {
        return restClient.get()
                .uri("/MichrazDetailsApi/Get?michrazID={id}", michrazId)
                .retrieve()
                .body(RmiTenderDetail.class);
    }

    byte[] fetchDocumentContent(RmiDocument document) {
        return restClient.post()
                .uri("/MichrazDetailsApi/GetFileContent")
                .body(document)
                .retrieve()
                .body(byte[].class);
    }

    private static BufferingClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory jdk = new JdkClientHttpRequestFactory(httpClient);
        jdk.setReadTimeout(READ_TIMEOUT);
        return new BufferingClientHttpRequestFactory(jdk);
    }
}
