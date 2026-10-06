package com.realestate.analytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
        if (adminKey.isBlank() || providedKey == null || !adminKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "גישה נדחתה");
        }
    }
}
