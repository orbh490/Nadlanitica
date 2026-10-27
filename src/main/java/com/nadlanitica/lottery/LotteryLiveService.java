package com.nadlanitica.lottery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * פרויקטי "דירה בהנחה" (מחיר למשתכן וכו') הפעילים על הלוח כרגע - נשלף חי מ-MochLotteryApiClient.
 * הסטטוס (פתוח/טרם נפתח/נסגר) מחושב כאן מתאריכי ההרשמה בפועל, כי שדה הסטטוס של השרת עצמו לא אמין (ראו הערה
 * ב-MochLotteryApiClient) - כל עוד תאריכי ה-API אמיתיים, זה חישוב יציב ולא תלוי בפרמטר שרת שלא עובד.
 */
@Service
public class LotteryLiveService {

    private static final Logger log = LoggerFactory.getLogger(LotteryLiveService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final MochLotteryApiClient apiClient;
    private volatile List<LiveLotteryProject> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public enum RegistrationStatus { OPEN, NOT_YET_OPEN, CLOSED }

    public record LiveLotteryProject(String projectId, String lotteryNumber, String tenderName, String city,
            String neighborhood, String projectName, String processName, String contractorName,
            Integer housingUnits, Integer targetHousingUnits, Double pricePerSqm, String entitlement,
            Integer totalSubscribers, Integer totalLocalSubscribers, LocalDate applicationStartDate,
            LocalDate applicationEndDate, RegistrationStatus status) {
    }

    public LotteryLiveService(MochLotteryApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * מחפש התאמה חיה לפרויקט היסטורי לפי מזהה הפרויקט של משרד השיכון (ProjectId/ProjectNumber) - אותו מזהה
     * מדויק קיים בשני המקורות (נבדק ואומת: 343 מזהים חופפים מתוך 500 פרויקטים על הלוח החי), בשונה מהתאמה
     * לפי שם פרויקט שמתעדת אחרת בין המערכות ולא מוצאת כלום. אף פעם לא מחזיר פרויקט שכבר נסגר.
     */
    public Optional<LiveLotteryProject> findForProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return Optional.empty();
        }
        String normId = projectId.trim();
        return allProjects().stream()
                .filter(p -> p.status() != RegistrationStatus.CLOSED)
                .filter(p -> normId.equals(normalize(p.projectId())))
                .min(Comparator.comparingInt(p -> p.status() == RegistrationStatus.OPEN ? 0 : 1));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private synchronized List<LiveLotteryProject> allProjects() {
        if (!cached.isEmpty() && Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cached;
        }
        try {
            LocalDate today = LocalDate.now();
            List<LiveLotteryProject> fresh = apiClient.fetchProjectsBoard().stream()
                    .map(raw -> toProject(raw, today))
                    .sorted(Comparator.comparing(LiveLotteryProject::applicationEndDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            cached = fresh;
            cachedAt = Instant.now();
        } catch (RestClientException e) {
            log.warn("שליפת לוח פרויקטי \"דירה בהנחה\" ממשרד הבינוי והשיכון נכשלה: {}", e.getMessage());
            if (cached.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "לא הצלחנו לקבל את לוח ההגרלות ממשרד הבינוי והשיכון כרגע. נסו שוב בעוד רגע.");
            }
        }
        return cached;
    }

    private static LiveLotteryProject toProject(MochLotteryApiClient.MochProjectItem raw, LocalDate today) {
        LocalDate start = parseDate(raw.applicationStartDate());
        LocalDate end = parseDate(raw.applicationEndDate());
        RegistrationStatus status;
        if (start != null && today.isBefore(start)) {
            status = RegistrationStatus.NOT_YET_OPEN;
        } else if (end != null && today.isAfter(end)) {
            status = RegistrationStatus.CLOSED;
        } else {
            status = RegistrationStatus.OPEN;
        }
        return new LiveLotteryProject(raw.projectNumber(), raw.lotteryNumber(), raw.tenderName(), raw.cityDescription(),
                raw.neighborhoodName(), raw.projectName(), raw.processName(), raw.contractorDescription(),
                raw.housingUnits(), raw.targetHousingUnits(), raw.pricePerSqm(), raw.entitlementDescription(),
                raw.totalSubscribers(), raw.totalLocalSubscribers(), start, end, status);
    }

    private static LocalDate parseDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDateTime.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
