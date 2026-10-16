package com.realestate.analytics.service;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * ישובי "עדיפות לאומית" הזכאים למענק מותנה (50,000-60,000 ש"ח, מוכר ע"י בנק ישראל כחלק מההון העצמי,
 * מותנה במגורים בדירה 5-7 שנים). הסכום המדויק משתנה לפי ישוב ואינו ממומש כאן - זו רק דגל מידע.
 * ponytail: אין מקור רשמי יחיד עם הסכום המדויק לכל ישוב - מוצג כטווח, לא כמספר מדויק.
 */
@Service
public class PeripheryGrantCities {

    private static final Set<String> ELIGIBLE_CITIES = Set.of(
            "אופקים", "אל-סייד", "בועיינה-נוג'ידאת", "בית שאן", "דימונה", "חורה", "חצור הגלילית",
            "טבריה", "ירוחם", "כפר מנדא", "כרמיאל", "יפיע", "מגדל העמק", "מעלות-תרשיחא", "מצפה רמון",
            "נהריה", "נחף", "נצרת", "נוף הגליל", "נתיבות", "סח'נין", "עכו", "ערד", "צפת",
            "קרית שמונה", "רהט", "שדרות", "שלומי"
    );

    public boolean isEligible(String city) {
        return city != null && ELIGIBLE_CITIES.contains(city.trim());
    }
}
