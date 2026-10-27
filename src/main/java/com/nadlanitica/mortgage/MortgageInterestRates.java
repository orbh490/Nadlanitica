package com.nadlanitica.mortgage;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ריבית משכנתא ממוצעת לפי תקופת הלוואה, מקור: בנק ישראל (pribmash.xls, תמונת מצב).
 * ponytail: טבלה סטטית שמתעדכנת ידנית - אין צורך בפרסר לקובץ Excel שרוב הגיליונות בו שבורים ממילא.
 */
@Service
public class MortgageInterestRates {

    private record RateTier(int maxYears, double annualRatePercent) {
    }

    private static final List<RateTier> TIERS = List.of(
            new RateTier(5, 3.3693115),
            new RateTier(10, 3.1622623),
            new RateTier(15, 3.13752939),
            new RateTier(20, 3.45658799),
            new RateTier(25, 3.48604886),
            new RateTier(Integer.MAX_VALUE, 3.42173014)
    );

    public double annualRatePercentForTerm(int termYears) {
        return TIERS.stream()
                .filter(t -> termYears <= t.maxYears())
                .findFirst()
                .orElse(TIERS.get(TIERS.size() - 1))
                .annualRatePercent();
    }
}
