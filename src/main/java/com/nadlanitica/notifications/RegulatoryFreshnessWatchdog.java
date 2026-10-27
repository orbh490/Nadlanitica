package com.nadlanitica.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * לא כל קבוע רגולטורי בפרויקט הזה ניתן לשליפה חיה (למשל הוראה 329 או טבלת ריבית המשכנתאות הממוצעת -
 * אין להם API ציבורי שמצאנו, בשונה מריבית בנק ישראל שכן יש לה - ראו BoiRateService). לכן במקום להעמיד
 * פנים שאפשר "לעדכן אוטומטית" משהו שאין לו מקור חי, הפתרון כאן הוא תזכורת קולנית ביומן השרת בתאריך הנכון -
 * כדי שאף אחד לא "שוכח" לבדוק מול המקור הרשמי בזמן, לא כדי לשלוף את הנתון בעצמו.
 */
@Service
public class RegulatoryFreshnessWatchdog {

    private static final Logger log = LoggerFactory.getLogger(RegulatoryFreshnessWatchdog.class);
    private static final int WARNING_WINDOW_DAYS = 30;

    private final NotificationEmailService emailService;

    public RegulatoryFreshnessWatchdog(NotificationEmailService emailService) {
        this.emailService = emailService;
    }

    private record ExpiringConstant(String name, LocalDate expiryDate, String note) {
    }

    private record PeriodicReviewConstant(String name, int reviewIntervalDays, LocalDate lastVerified, String note) {
    }

    // קבועים עם תאריך פקיעה ידוע במדויק (קפאון רגולטורי עם תאריך סיום מפורסם)
    private static final List<ExpiringConstant> EXPIRING = List.of(
            new ExpiringConstant("מדרגות מס רכישה - דירה להשקעה", LocalDate.of(2026, 12, 31),
                    "PurchaseTaxCalculator.INVESTMENT_BRACKETS - יש לבדוק את אתר רשות המסים לגבי מדרגות חדשות"),
            new ExpiringConstant("מדרגות מס רכישה - דירה יחידה/חליפית", LocalDate.of(2028, 1, 15),
                    "PurchaseTaxCalculator.SINGLE_OR_REPLACEMENT_BRACKETS - יש לבדוק את אתר רשות המסים לגבי מדרגות חדשות")
    );

    // קבועים בלי תאריך פקיעה ידוע, אבל בלי מקור חי - נבדקים מחדש כל פרק זמן קבוע ללא קשר לתאריך ספציפי
    private static final List<PeriodicReviewConstant> PERIODIC_REVIEW = List.of(
            new PeriodicReviewConstant("טבלת ריבית משכנתא ממוצעת (MortgageInterestRates)", 90, LocalDate.of(2026, 9, 1),
                    "לא נמצא API חי - יש לבדוק ידנית מול boi.org.il/information/interestrates/mortgage/"),
            new PeriodicReviewConstant("קבועי הוראה 329 (LTV/DTI/66.66%/30 שנה)", 180, LocalDate.of(2026, 9, 1),
                    "לא נמצא API חי - יש לבדוק אם פורסם תיקון להוראה 329 באתר בנק ישראל"),
            new PeriodicReviewConstant("רשימת ישובי עדיפות לאומית (PeripheryGrantCities)", 365, LocalDate.of(2026, 9, 1),
                    "רשימה מבוססת מחקר, לא API - יש לבדוק אם התעדכנה רשימת יישובי העדיפות הלאומית")
    );

    /** להרצה ידנית מ-NotificationTestController, כדי לוודא שהגדרות המייל תקינות בלי לחכות ל-06:00. */
    public void runNow() {
        checkFreshness();
    }

    @Scheduled(cron = "0 0 6 * * *")
    void checkFreshness() {
        LocalDate today = LocalDate.now();
        StringBuilder emailBody = new StringBuilder();

        for (ExpiringConstant c : EXPIRING) {
            long daysUntil = ChronoUnit.DAYS.between(today, c.expiryDate());
            if (daysUntil < 0) {
                String line = "⚠️ קבוע רגולטורי פג תוקף לפני %d ימים: %s (תאריך יעד היה %s) - %s"
                        .formatted(-daysUntil, c.name(), c.expiryDate(), c.note());
                log.warn(line);
                emailBody.append(line).append("\n\n");
            } else if (daysUntil <= WARNING_WINDOW_DAYS) {
                String line = "🔔 קבוע רגולטורי עומד לפוג בעוד %d ימים: %s (תאריך יעד %s) - %s"
                        .formatted(daysUntil, c.name(), c.expiryDate(), c.note());
                log.warn(line);
                emailBody.append(line).append("\n\n");
            }
        }

        for (PeriodicReviewConstant c : PERIODIC_REVIEW) {
            long daysSinceVerified = ChronoUnit.DAYS.between(c.lastVerified(), today);
            if (daysSinceVerified >= c.reviewIntervalDays()) {
                String line = "🔔 קבוע דורש בדיקה חוזרת (לא נבדק %d ימים, מחזור בדיקה %d ימים): %s - %s"
                        .formatted(daysSinceVerified, c.reviewIntervalDays(), c.name(), c.note());
                log.warn(line);
                emailBody.append(line).append("\n\n");
            }
        }

        if (!emailBody.isEmpty()) {
            emailService.sendAlert("קבועים רגולטוריים דורשים בדיקה", emailBody.toString());
        }
    }
}
