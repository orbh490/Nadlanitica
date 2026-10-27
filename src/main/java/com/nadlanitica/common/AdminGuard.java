package com.nadlanitica.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * ה"אדמין" היחיד באפליקציה הזו הוא בעל האתר עצמו - אין מערכת הרשאות תפקידים.
 * לכן במקום מנגנון תפקידים מלא, נקודות קצה ניהוליות (רשימת לידים וכו') מוגנות במפתח סוד יחיד
 * שמוגדר בסביבה (ADMIN_KEY) ולא בקוד - כדי שנתוני לקוחות לא ייחשפו לכל מי שנרשם באתר.
 */
@Component
public class AdminGuard {

    private final String adminKey;

    public AdminGuard(@Value("${app.admin.key:}") String adminKey) {
        this.adminKey = adminKey;
    }

    public void check(String providedKey) {
        // MessageDigest.isEqual במקום String.equals - משווה בזמן קבוע כדי לא לדלוף מידע על המפתח
        // דרך הבדל בזמן תגובה (timing attack).
        if (adminKey.isBlank() || providedKey == null
                || !MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "גישה נדחתה");
        }
    }
}
