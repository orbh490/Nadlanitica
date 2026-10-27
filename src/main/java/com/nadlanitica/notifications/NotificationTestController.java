package com.nadlanitica.notifications;

import com.nadlanitica.common.AdminGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * בקר לבדיקה ידנית בלבד - לוודא שהגדרות ה-SMTP תקינות בלי לחכות להרצה המתוזמנת היומית.
 * המפתח ב-header (X-Admin-Key) ולא ב-URL, כדי שלא יישמר בלוגים של השרת וה-proxy.
 */
@RestController
public class NotificationTestController {

    private final NotificationEmailService emailService;
    private final RegulatoryFreshnessWatchdog watchdog;
    private final AdminGuard adminGuard;

    public NotificationTestController(NotificationEmailService emailService, RegulatoryFreshnessWatchdog watchdog, AdminGuard adminGuard) {
        this.emailService = emailService;
        this.watchdog = watchdog;
        this.adminGuard = adminGuard;
    }

    @PostMapping("/api/admin/test-email")
    String testEmail(@RequestHeader("X-Admin-Key") String key) {
        adminGuard.check(key);
        emailService.sendAlert("בדיקת מייל", "אם קיבלת את המייל הזה, הגדרות ה-SMTP תקינות.");
        return "נשלח (בדוק את התיבה שלך תוך דקה, ואת הלוג אם לא הגיע)";
    }

    @PostMapping("/api/admin/run-freshness-check")
    String runFreshnessCheck(@RequestHeader("X-Admin-Key") String key) {
        adminGuard.check(key);
        watchdog.runNow();
        return "הבדיקה רצה - מייל יישלח רק אם נמצא משהו שדורש תשומת לב";
    }
}
