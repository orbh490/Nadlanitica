package com.realestate.analytics.service;

import java.util.Map;
import java.util.Optional;

/**
 * קודי "אוכלוסיות עדיפות" של רשות מקרקעי ישראל במכרזי קרקע (למי שמורה עדיפות/מכסה נפרדת במכרז).
 * האחוז/גודל ההטבה בפועל אינו חלק מה-API - הוא משתנה לפי מכרז ומופיע רק בחוברת המכרז עצמה.
 */
final class PriorityPopulation {

    private static final Map<Integer, String> LABELS = Map.ofEntries(
            Map.entry(1, "אנשים עם מוגבלות"),
            Map.entry(3, "חסרי דיור"),
            Map.entry(4, "בני מיעוטים מומלצי כוחות הביטחון"),
            Map.entry(6, "חיילי מילואים"),
            Map.entry(7, "חיילי מילואים לוחמים"),
            Map.entry(8, "חיילי מילואים לוחמים בני מקום תושבי היישוב"),
            Map.entry(9, "חיילי מילואים פעילים בני מקום תושבי היישוב"),
            Map.entry(10, "חיילי מילואים לוחמים בני מקום תושבי המועצה"),
            Map.entry(11, "חיילי מילואים לוחמים בני מקום"),
            Map.entry(12, "חיילי מילואים פעילים בני מקום תושבי המועצה"),
            Map.entry(13, "חיילי מילואים פעילים בני מקום"),
            Map.entry(14, "בני מקום תושבי היישוב"),
            Map.entry(15, "בני מקום תושבי המועצה"),
            Map.entry(16, "בני מקום")
    );

    // מילת החיפוש בפועל בטקסט חוברת המכרז - המינוח המשפטי שם שונה מהתווית הרשמית (למשל "נכות" ולא "מוגבלות")
    private static final Map<Integer, String> SEARCH_KEYWORDS = Map.ofEntries(
            Map.entry(1, "נכות"),
            Map.entry(3, "חסר"),
            Map.entry(4, "מיעוטים"),
            Map.entry(6, "מילואים"),
            Map.entry(7, "מילואים"),
            Map.entry(8, "מילואים"),
            Map.entry(9, "מילואים"),
            Map.entry(10, "מילואים"),
            Map.entry(11, "מילואים"),
            Map.entry(12, "מילואים"),
            Map.entry(13, "מילואים"),
            Map.entry(14, "בן מקום"),
            Map.entry(15, "בן מקום"),
            Map.entry(16, "בן מקום")
    );

    private PriorityPopulation() {
    }

    static Optional<String> labelFor(int code) {
        return Optional.ofNullable(LABELS.get(code));
    }

    static Optional<String> searchKeywordFor(int code) {
        return Optional.ofNullable(SEARCH_KEYWORDS.get(code));
    }
}
