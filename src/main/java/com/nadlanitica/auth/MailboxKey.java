package com.nadlanitica.auth;

import java.util.Locale;

/**
 * התיבה האמיתית שמאחורי כתובת אימייל - כדי שאי אפשר יהיה לפתוח כמה חשבונות על אותה תיבה:
 * - כל מה שאחרי + בחלק המקומי הוא כינוי לאותה תיבה (orbh+1@x.com = orbh@x.com). כך זה בג'ימייל, אאוטלוק, iCloud ורוב הספקים.
 * - בג'ימייל גם נקודות לא משנות (o.r.bh@gmail.com = orbh@gmail.com), ו-googlemail.com הוא אותו דבר כמו gmail.com.
 * המפתח משמש רק להשוואה; המיילים עצמם נשלחים לכתובת כפי שהמשתמש הזין אותה.
 */
public final class MailboxKey {

    private MailboxKey() {
    }

    public static String of(String email) {
        if (email == null) {
            return "";
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        int at = e.lastIndexOf('@');
        if (at <= 0) {
            return e;
        }
        String local = e.substring(0, at);
        String domain = e.substring(at + 1);
        int plus = local.indexOf('+');
        if (plus > 0) {
            local = local.substring(0, plus);
        }
        if (domain.equals("googlemail.com")) {
            domain = "gmail.com";
        }
        if (domain.equals("gmail.com")) {
            local = local.replace(".", "");
        }
        return local + "@" + domain;
    }
}
