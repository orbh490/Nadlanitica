package com.realestate.analytics.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class MortgageInsuranceEstimatorTest {

    private final MortgageInsuranceEstimator estimator = new MortgageInsuranceEstimator();

    @Test
    void matchesReferencePointForOneMillionLoan() {
        var estimate = estimator.estimateMonthlyCost(1_000_000);
        assertThat(estimate.lifeInsuranceMonthly()).isCloseTo(60.0, offset(0.01));
        assertThat(estimate.structureInsuranceMonthly()).isEqualTo(55.0);
        assertThat(estimate.totalMonthly()).isCloseTo(115.0, offset(0.01));
    }

    @Test
    void lifeInsuranceScalesWithLoanAmount() {
        var half = estimator.estimateMonthlyCost(500_000);
        var full = estimator.estimateMonthlyCost(1_000_000);
        assertThat(half.lifeInsuranceMonthly()).isCloseTo(full.lifeInsuranceMonthly() / 2, offset(0.01));
    }
}
