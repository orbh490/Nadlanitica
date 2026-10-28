package com.nadlanitica.lottery;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * לקוח ל-API הציבורי (לא רשמי, אך פתוח וללא אימות) של משרד הבינוי והשיכון לפרויקטי "דירה בהנחה" (מחיר למשתכן
 * ומסלולים דומים) - אתר ההרשמה להגרלות. נמצא ואומת ידנית מול הרשת החיה של https://dira.moch.gov.il/ProjectsList,
 * באותה שיטה שבה נמצא ה-API של רמ"י למכרזי קרקע.
 * RestClient רגיל עובד כאן ישירות.
 * חשוב: פרמטר ProjectStatus בקריאה עם IsInit=true מתעלם בפועל (נבדק - status=2 ו-status=4 מחזירים בדיוק אותו
 * מספר בייטים) - כנראה שסינון אמיתי דורש session/captcha שאין לנו כלקוח לא-מחובר. לכן אנחנו שולפים את הרשימה
 * המלאה וקובעים בעצמנו אם הרשמה פתוחה/עתידית/נסגרה לפי ApplicationStartDate/ApplicationEndDate בפועל
 * (ראו LotteryLiveService) - לא סומכים על שדה סטטוס מהשרת שהוכח כלא אמין.
 */
@Component
class MochLotteryApiClient {

    private static final String BASE_URL = "https://dira.moch.gov.il/api";

    // בלי timeout, שרת שלא עונה תוקע את הבקשה (ואת המשתמש שמחכה לרשימת ההגרלות) בלי הגבלה.
    // זמן הקריאה נדיב כי התשובה היא ~2.3MB.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .requestFactory(requestFactory())
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .build();

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    record MochProjectItem(
            @JsonProperty("LotteryNumber") String lotteryNumber,
            @JsonProperty("ApplicationStartDate") String applicationStartDate,
            @JsonProperty("ApplicationEndDate") String applicationEndDate,
            @JsonProperty("ProcessName") String processName,
            @JsonProperty("ProjectNumber") String projectNumber,
            @JsonProperty("CityDescription") String cityDescription,
            @JsonProperty("ProjectName") String projectName,
            @JsonProperty("NeighborhoodName") String neighborhoodName,
            @JsonProperty("ContractorDescription") String contractorDescription,
            @JsonProperty("HousingUnits") Integer housingUnits,
            @JsonProperty("TargetHousingUnits") Integer targetHousingUnits,
            @JsonProperty("PricePerUnit") Double pricePerSqm,
            @JsonProperty("TenderName") String tenderName,
            @JsonProperty("EntitlementDescription") String entitlementDescription,
            @JsonProperty("TotalSubscribers") Integer totalSubscribers,
            @JsonProperty("TotalLocalSubscribers") Integer totalLocalSubscribers) {
    }

    private record MochProjectsResponse(@JsonProperty("ProjectItems") List<MochProjectItem> projectItems) {
    }

    /** שולף את "לוח" הפרויקטים המלא כפי שהאתר עצמו מציג בטעינה ראשונית (עד 500 רשומות). */
    List<MochProjectItem> fetchProjectsBoard() {
        String param = "?firstApplicantIdentityNumber=&secondApplicantIdentityNumber=&ProjectStatus="
                + "1&Entitlement=1&PageNumber=1&PageSize=500&IsInit=true&";
        MochProjectsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/Invoker").queryParam("method", "Projects")
                        .queryParam("param", param).build())
                .retrieve()
                .body(MochProjectsResponse.class);
        return response == null || response.projectItems() == null ? List.of() : response.projectItems();
    }
}
