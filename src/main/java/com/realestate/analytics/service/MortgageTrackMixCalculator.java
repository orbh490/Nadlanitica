package com.realestate.analytics.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * תמהיל משכנתא אמיתי: הלוואה מפוצלת למספר מסלולים בריביות שונות, בכפוף למגבלה הרגולטורית
 * על חלק ההלוואה בריבית משתנה (הוראה 329, סעיף 7: עד 66.66% בריבית משתנה).
 * ריביות המסלולים: קבועה לא צמודה ~4.85% (אומדן תמונת מצב, לא מתעדכן אוטומטית - ראו הערה למטה),
 * פריים = ריבית בנק ישראל **החיה** (BoiRateService, מתעדכנת פעם ביום) + מרווח בנקאי קבוע 1.5% (המרווח עצמו הוא
 * מוסכמה שוקית, לא מתפרסם ע"י בנק ישראל, ולכן נשאר קבוע קשיח).
 * צמודת המדד ממשיכה להסתמך על הטבלה הקיימת (MortgageInterestRates, מבנק ישראל).
 * ponytail: אין כאן הבחנה בין "קבועה צמודת מדד" ל"משתנה צמודת מדד" - מוצג כמסלול אחד "צמודת מדד",
 * ולכן מגבלת ה-66.66% מוחלת רק על מסלול הפריים, שהוא ה"ריבית משתנה" החד-משמעית מבין השלושה.
 */
@Service
public class MortgageTrackMixCalculator {

    private static final double FIXED_NON_INDEXED_RATE_PERCENT = 4.85;
    private static final double PRIME_MARGIN_PERCENT = 1.5;
    private static final double MAX_VARIABLE_SHARE_PERCENT = 66.66;

    private final MortgagePaymentCalculator paymentCalculator;
    private final MortgageInterestRates cpiLinkedRates;
    private final BoiRateService boiRateService;

    public MortgageTrackMixCalculator(MortgagePaymentCalculator paymentCalculator, MortgageInterestRates cpiLinkedRates,
            BoiRateService boiRateService) {
        this.paymentCalculator = paymentCalculator;
        this.cpiLinkedRates = cpiLinkedRates;
        this.boiRateService = boiRateService;
    }

    public record TrackResult(String trackName, double percentOfLoan, double annualRatePercent, double loanPortion,
            double monthlyPayment) {
    }

    public record TrackMixResult(List<TrackResult> tracks, double totalMonthlyPayment, double blendedAnnualRatePercent,
            boolean exceedsVariableRateCap) {
    }

    public TrackMixResult calculate(double loanAmount, int termYears, double fixedNonIndexedPercent,
            double cpiLinkedPercent, double primePercent) {
        double cpiLinkedRate = cpiLinkedRates.annualRatePercentForTerm(termYears);
        TrackResult fixed = track("קבועה לא צמודה", fixedNonIndexedPercent, FIXED_NON_INDEXED_RATE_PERCENT, loanAmount, termYears);
        TrackResult cpiLinked = track("צמודת מדד", cpiLinkedPercent, cpiLinkedRate, loanAmount, termYears);
        double primeRate = boiRateService.currentBaseRatePercent() + PRIME_MARGIN_PERCENT;
        TrackResult prime = track("פריים", primePercent, primeRate, loanAmount, termYears);

        List<TrackResult> tracks = List.of(fixed, cpiLinked, prime);
        double totalMonthly = tracks.stream().mapToDouble(TrackResult::monthlyPayment).sum();
        double blendedRate = tracks.stream()
                .mapToDouble(t -> t.annualRatePercent() * t.percentOfLoan() / 100)
                .sum();

        return new TrackMixResult(tracks, totalMonthly, blendedRate, primePercent > MAX_VARIABLE_SHARE_PERCENT);
    }

    private TrackResult track(String name, double percent, double annualRate, double loanAmount, int termYears) {
        double portion = loanAmount * percent / 100;
        double monthly = portion > 0 ? paymentCalculator.monthlyPaymentForRate(portion, termYears, annualRate) : 0;
        return new TrackResult(name, percent, annualRate, portion, monthly);
    }
}
