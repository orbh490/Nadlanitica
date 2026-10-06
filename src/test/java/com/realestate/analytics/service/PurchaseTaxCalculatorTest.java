package com.realestate.analytics.service;

import com.realestate.analytics.model.ApartmentCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class PurchaseTaxCalculatorTest {

    private final PurchaseTaxCalculator calculator = new PurchaseTaxCalculator();

    @Test
    void singleHomeUnderExemptionThresholdPaysNoTax() {
        assertThat(calculator.calculatePurchaseTax(1_400_000, ApartmentCategory.SINGLE)).isEqualTo(0.0);
    }

    @Test
    void singleHomeAboveThresholdPaysMarginalRateOnlyOnExcess() {
        // exact figures cross-checked against kolzchut.org.il purchase-tax article
        assertThat(calculator.calculatePurchaseTax(3_000_000, ApartmentCategory.SINGLE))
                .isCloseTo(45_538.33, offset(0.5));
    }

    @Test
    void replacementApartmentUsesSameBracketsAsSingleHome() {
        assertThat(calculator.calculatePurchaseTax(3_000_000, ApartmentCategory.REPLACEMENT))
                .isEqualTo(calculator.calculatePurchaseTax(3_000_000, ApartmentCategory.SINGLE));
    }

    @Test
    void investmentApartmentHasNoExemptionAndHigherRate() {
        assertThat(calculator.calculatePurchaseTax(1_400_000, ApartmentCategory.INVESTMENT))
                .isEqualTo(112_000.0); // flat 8% - entire price is under the first investment bracket
    }
}
