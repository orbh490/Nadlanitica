package com.realestate.analytics.service;

import com.realestate.analytics.model.ApartmentCategory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * מדרגות מס רכישה, מקור: רשות המסים (חוק מיסוי מקרקעין, סעיף 9).
 * דירה יחידה/חליפית: מדרגות בתוקף 16.01.2024-15.01.2028 (קפואות עד אז).
 * דירה להשקעה: מדרגות בתוקף 16.01.2024-31.12.2026.
 */
@Service
public class PurchaseTaxCalculator {

    private record TaxBracket(double upperBound, double ratePercent) {
    }

    private static final List<TaxBracket> SINGLE_OR_REPLACEMENT_BRACKETS = List.of(
            new TaxBracket(1_978_745, 0),
            new TaxBracket(2_347_040, 3.5),
            new TaxBracket(6_055_070, 5),
            new TaxBracket(20_183_565, 8),
            new TaxBracket(Double.MAX_VALUE, 10)
    );

    private static final List<TaxBracket> INVESTMENT_BRACKETS = List.of(
            new TaxBracket(6_055_070, 8),
            new TaxBracket(Double.MAX_VALUE, 10)
    );

    public double calculatePurchaseTax(double purchasePrice, ApartmentCategory category) {
        List<TaxBracket> brackets = category == ApartmentCategory.INVESTMENT
                ? INVESTMENT_BRACKETS
                : SINGLE_OR_REPLACEMENT_BRACKETS;
        double tax = 0;
        double lowerBound = 0;
        for (TaxBracket bracket : brackets) {
            if (purchasePrice <= lowerBound) {
                break;
            }
            double taxableInBracket = Math.min(purchasePrice, bracket.upperBound()) - lowerBound;
            tax += taxableInBracket * bracket.ratePercent() / 100;
            lowerBound = bracket.upperBound();
        }
        return tax;
    }
}
