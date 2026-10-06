package com.realestate.analytics.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MortgagePaymentCalculatorTest {

    private final MortgagePaymentCalculator calculator = new MortgagePaymentCalculator(new MortgageInterestRates());

    @Test
    void monthlyPaymentMatchesStandardAmortizationFormula() {
        // 1,000,000 loan, 20 years, ~3.4566% annual -> known amortization result
        double payment = calculator.monthlyPayment(1_000_000, 20);
        assertThat(payment).isCloseTo(5777.31, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void longerTermMeansLowerMonthlyPaymentForSameLoan() {
        double payment15 = calculator.monthlyPayment(1_000_000, 15);
        double payment25 = calculator.monthlyPayment(1_000_000, 25);
        assertThat(payment25).isLessThan(payment15);
    }

    @Test
    void ratePicksCorrectTierBoundary() {
        assertThat(calculator.annualRatePercentForTerm(5)).isEqualTo(3.3693115);
        assertThat(calculator.annualRatePercentForTerm(6)).isEqualTo(3.1622623);
        assertThat(calculator.annualRatePercentForTerm(30)).isEqualTo(3.42173014);
    }

    @Test
    void dtiWithinLegalLimitWhenPaymentIsAtMostHalfIncome() {
        assertThat(calculator.withinLegalDtiLimit(5000, 10000)).isTrue(); // exactly 50%
        assertThat(calculator.withinLegalDtiLimit(5001, 10000)).isFalse(); // just over 50%
    }

    @Test
    void dtiPercentIsZeroWhenIncomeUnknown() {
        assertThat(calculator.dtiPercent(5000, 0)).isEqualTo(0);
        assertThat(calculator.withinLegalDtiLimit(5000, 0)).isFalse();
    }

    @Test
    void amortizationScheduleHasOneRowPerYearAndEndsAtZeroBalance() {
        var schedule = calculator.amortizationScheduleByYear(1_000_000, 20);
        assertThat(schedule).hasSize(20);
        assertThat(schedule.get(19).remainingBalance()).isCloseTo(0, org.assertj.core.data.Offset.offset(1.0));
    }

    @Test
    void amortizationInterestShareShrinksAndPrincipalShareGrowsOverTime() {
        var schedule = calculator.amortizationScheduleByYear(1_000_000, 20);
        assertThat(schedule.get(19).principalPaid()).isGreaterThan(schedule.get(0).principalPaid()); // principal paid grows year over year
        assertThat(schedule.get(19).interestPaid()).isLessThan(schedule.get(0).interestPaid()); // interest paid shrinks year over year
    }

    @Test
    void amortizationTotalPrincipalAcrossYearsEqualsLoanAmount() {
        var schedule = calculator.amortizationScheduleByYear(1_000_000, 20);
        double totalPrincipal = schedule.stream().mapToDouble(MortgagePaymentCalculator.YearlyAmortization::principalPaid).sum();
        assertThat(totalPrincipal).isCloseTo(1_000_000, org.assertj.core.data.Offset.offset(1.0));
    }
}
