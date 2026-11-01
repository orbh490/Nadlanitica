package com.nadlanitica.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * ה"אדמין" היחיד באפליקציה הזו הוא בעל האתר עצמו - אין מערכת הרשאות תפקידים.
 * לכן במקום מנגנון תפקידים מלא, נקודות קצה ניהוליות (רשימת לידים וכו') מוגנות במפתח סוד יחיד
 * שמוגדר בסביבה (ADMIN_KEY) ולא בקוד - כדי שנתוני לקוחות לא ייחשפו לכל מי שנרשם באתר.
 * נגד ניחוש: אחרי MAX_FAILURES ניסיונות שגויים בתוך WINDOW, כל הבדיקות נחסמות עד סוף החלון - גם עם המפתח הנכון.
 * המונה משותף לכל הכתובות ולא לפי IP, כי יש בעל אתר אחד: מי שמנחש מהרבה כתובות לא יקבל יותר ניסיונות.
 * המחיר: מי שמנחש יכול לנעול את בעל האתר מחוץ לעמוד הלידים ל-15 דקות - עדיף על פני חשיפת הלידים.
 */
@Component
public class AdminGuard {

    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final String adminKey;
    private final Clock clock;
    private Instant windowStart = Instant.EPOCH;
    private int failures;

    @Autowired
    public AdminGuard(@Value("${app.admin.key:}") String adminKey) {
        this(adminKey, Clock.systemUTC());
    }

    AdminGuard(String adminKey, Clock clock) {
        this.adminKey = adminKey;
        this.clock = clock;
    }

    public void check(String providedKey) {
        Instant now = clock.instant();
        synchronized (this) {
            if (now.isAfter(windowStart.plus(WINDOW))) {
                windowStart = now;
                failures = 0;
            }
            if (failures >= MAX_FAILURES) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "יותר מדי ניסיונות שגויים - נסו שוב בעוד כ-15 דקות");
            }
        }
        // MessageDigest.isEqual במקום String.equals - משווה בזמן קבוע כדי לא לדלוף מידע על המפתח
        // דרך הבדל בזמן תגובה (timing attack).
        if (adminKey.isBlank() || providedKey == null
                || !MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            synchronized (this) {
                failures++;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "גישה נדחתה");
        }
    }
}
