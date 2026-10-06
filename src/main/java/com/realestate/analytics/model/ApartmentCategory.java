package com.realestate.analytics.model;

// קטגוריות רוכש לפי הוראת ניהול בנקאי תקין 329 (מגבלות למתן הלוואות לדיור)
public enum ApartmentCategory {
    SINGLE,      // "דירה יחידה" - אין לרוכש דירה אחרת, LTV מקס' 75%
    REPLACEMENT, // "דירה חליפית" - משפר דיור שמתחייב למכור את דירתו הקיימת, LTV מקס' 70%
    INVESTMENT   // "דירה להשקעה" - נשארת בבעלות הרוכש דירה נוספת, LTV מקס' 50%
}
