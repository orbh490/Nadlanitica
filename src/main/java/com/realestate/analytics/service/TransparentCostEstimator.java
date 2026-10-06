package com.realestate.analytics.service;

import org.springframework.stereotype.Service;

/**
 * עלויות נוספות מעבר להון עצמי ומס רכישה, כדי לחשוף את "המחיר האמיתי" של העסקה.
 * בשונה מ-MortgageRuleEngine/PurchaseTaxCalculator, האחוזים כאן הם אומדן מקובל בשוק ולא חוק מחייב -
 * שכ"ט עו"ד ועמלת תיווך משתנים בפועל בין עסקאות. מע"מ (18%) הוא כן שיעור קבוע בחוק.
 */
@Service
public class TransparentCostEstimator {

    private static final double VAT_RATE = 0.18;
    private static final double LAWYER_FEE_PERCENT = 1.0;
    private static final double APPRAISER_FLAT_FEE = 2_000.0;
    private static final double BROKER_COMMISSION_PERCENT = 2.0;

    public record ClosingCosts(double lawyerFee, double appraiserFee, double brokerCommission, double total) {
    }

    public ClosingCosts estimateClosingCosts(double purchasePrice, boolean usedBroker) {
        double lawyerFee = purchasePrice * LAWYER_FEE_PERCENT / 100 * (1 + VAT_RATE);
        double brokerCommission = usedBroker ? purchasePrice * BROKER_COMMISSION_PERCENT / 100 * (1 + VAT_RATE) : 0;
        double total = lawyerFee + APPRAISER_FLAT_FEE + brokerCommission;
        return new ClosingCosts(lawyerFee, APPRAISER_FLAT_FEE, brokerCommission, total);
    }

    // עלות נוספת משוערת מהצמדה למדד תשומות הבנייה בזמן ההמתנה לקבלן (רלוונטי בעיקר לדירה בהנחה).
    // שיעור המדד הוא הנחת עבודה הניתנת להתאמה - טווח היסטורי אופייני 2%-5% לשנה (הלמ"ס), עם חריגות (2025: 5.1%)
    public double estimateWaitingPeriodExtraCost(double purchasePrice, double assumedAnnualIndexRatePercent, int waitYears) {
        double rate = assumedAnnualIndexRatePercent / 100;
        return purchasePrice * (Math.pow(1 + rate, waitYears) - 1);
    }
}
