package com.nadlanitica.mortgage;

import org.springframework.stereotype.Service;

/**
 * מבוסס על הוראת ניהול בנקאי תקין 329 של בנק ישראל - "מגבלות למתן הלוואות לדיור" (גרסה 10, 27/12/2021).
 */
@Service
public class MortgageRuleEngine {

    private static final double SUBSIDIZED_VALUE_CAP = 1_800_000.0;
    private static final double MIN_CASH_SUBSIDIZED_WITH_GRANT = 60_000.0;
    private static final double MIN_CASH_OTHER = 100_000.0;

    public double calculateRequiredEquity(BuyerProfile buyer, Property property) {
        double maxLtv = maxLtvFor(buyer.getCategory());
        double basisValue = ltvBasisValue(property);
        double requiredByLtv = basisValue * (1 - maxLtv);
        double minimumCash = property.isSubsidized() ? MIN_CASH_SUBSIDIZED_WITH_GRANT : MIN_CASH_OTHER;
        return Math.max(requiredByLtv, minimumCash);
    }

    public boolean canAfford(BuyerProfile buyer, Property property) {
        return buyer.getAvailableEquity() >= calculateRequiredEquity(buyer, property);
    }

    /**
     * גודל ההלוואה בפועל: ההון העצמי מכסה קודם את העלויות שמשולמות במזומן (מס רכישה, עלויות סגירה, ערבות),
     * ורק היתרה הולכת למקדמה. ההלוואה לעולם לא עולה על המחיר פחות ההון העצמי המינימלי לפי ההוראה -
     * כשהלקוח לא מגיע לזה, ההלוואה נחתכת ב-maxLoanAmount ו-equityShortfall אומר כמה מזומן חסר.
     */
    public record LoanSizing(double loanAmount, double maxLoanAmount, boolean cappedByLtv, double equityShortfall) {
    }

    public LoanSizing sizeLoan(double price, double requiredEquity, double availableEquity, double upfrontCashCosts) {
        double equityForDownPayment = Math.max(0, availableEquity - upfrontCashCosts);
        double neededLoan = Math.max(0, price - equityForDownPayment);
        double maxLoan = Math.max(0, price - requiredEquity);
        double shortfall = Math.max(0, requiredEquity + upfrontCashCosts - availableEquity);
        return new LoanSizing(Math.min(neededLoan, maxLoan), maxLoan, neededLoan > maxLoan, shortfall);
    }

    // סעיף 2 להוראה: מגבלת שיעור מימון (LTV) לפי קטגוריית הדירה
    private static double maxLtvFor(ApartmentCategory category) {
        return switch (category) {
            case SINGLE -> 0.75;
            case REPLACEMENT -> 0.70;
            case INVESTMENT -> 0.50;
        };
    }

    // סעיף 4א להוראה: בדירה במחיר מופחת ששוויה השמאי עולה על 1.8 מיליון ש"ח,
    // בסיס החישוב הוא 1.8 מיליון ש"ח או מחיר הרכישה - הגבוה מביניהם
    private static double ltvBasisValue(Property property) {
        if (property.isSubsidized() && property.getMarketValue() > SUBSIDIZED_VALUE_CAP) {
            return Math.max(SUBSIDIZED_VALUE_CAP, property.getPurchasePrice());
        }
        return property.isSubsidized() ? property.getMarketValue() : property.getPurchasePrice();
    }
}
