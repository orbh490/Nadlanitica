package com.realestate.analytics.service;

import org.springframework.stereotype.Service;

/**
 * ביטוחי משכנתא חובה (חיים + מבנה) - הבנק מחייב את שניהם, אבל הלווה רשאי לבחור פוליסה חיצונית
 * (הוראת בנק ישראל). אומדן שוק בלבד, לא מחיר קבוע - תלוי גיל, בריאות, וגובה כיסוי בפועל.
 * מקור נקודת הייחוס: זוג בגיל 35, הלוואה של 1,000,000 ש"ח ל-25 שנה -> כ-50-70 ש"ח לחודש בתחילת הדרך.
 * ponytail: קנה מידה ליניארי מנקודת הייחוס היחידה שיש לי - לא מודל אקטוארי אמיתי לפי גיל.
 */
@Service
public class MortgageInsuranceEstimator {

    private static final double LIFE_INSURANCE_REFERENCE_LOAN = 1_000_000.0;
    private static final double LIFE_INSURANCE_REFERENCE_MONTHLY = 60.0; // אמצע הטווח 50-70 לזוג בני 35
    private static final double STRUCTURE_INSURANCE_MONTHLY_ESTIMATE = 55.0; // אמצע הטווח 30-100 לדירה ממוצעת

    public record InsuranceEstimate(double lifeInsuranceMonthly, double structureInsuranceMonthly, double totalMonthly) {
    }

    public InsuranceEstimate estimateMonthlyCost(double loanAmount) {
        double lifeInsurance = loanAmount * (LIFE_INSURANCE_REFERENCE_MONTHLY / LIFE_INSURANCE_REFERENCE_LOAN);
        return new InsuranceEstimate(lifeInsurance, STRUCTURE_INSURANCE_MONTHLY_ESTIMATE, lifeInsurance + STRUCTURE_INSURANCE_MONTHLY_ESTIMATE);
    }
}
