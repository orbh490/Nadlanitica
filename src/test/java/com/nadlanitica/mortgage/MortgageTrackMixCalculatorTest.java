package com.nadlanitica.mortgage;

import com.nadlanitica.notifications.NotificationEmailService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class MortgageTrackMixCalculatorTest {

    private final MortgageTrackMixCalculator calculator = new MortgageTrackMixCalculator(
            new MortgagePaymentCalculator(new MortgageInterestRates()), new MortgageInterestRates(),
            new BoiRateService(new NotificationEmailService(null, "test@example.com")));

    @Test
    void hundredPercentFixedMatchesSingleTrackPayment() {
        var result = calculator.calculate(1_000_000, 20, 100, 0, 0);
        assertThat(result.tracks()).hasSize(3);
        assertThat(result.blendedAnnualRatePercent()).isCloseTo(4.85, offset(0.001));
        assertThat(result.exceedsVariableRateCap()).isFalse();
    }

    @Test
    void primeAboveLegalCapIsFlagged() {
        var result = calculator.calculate(1_000_000, 20, 10, 20, 70);
        assertThat(result.exceedsVariableRateCap()).isTrue();
    }

    @Test
    void primeAtOrBelowCapIsNotFlagged() {
        var result = calculator.calculate(1_000_000, 20, 34, 0, 66);
        assertThat(result.exceedsVariableRateCap()).isFalse();
    }

    @Test
    void totalMonthlyPaymentIsSumOfEachTrackPortion() {
        var result = calculator.calculate(1_000_000, 20, 40, 30, 30);
        double sum = result.tracks().stream().mapToDouble(MortgageTrackMixCalculator.TrackResult::monthlyPayment).sum();
        assertThat(result.totalMonthlyPayment()).isCloseTo(sum, offset(0.01));
    }

    @Test
    void loanPortionsSumToTotalLoanAmount() {
        var result = calculator.calculate(1_000_000, 20, 40, 30, 30);
        double sum = result.tracks().stream().mapToDouble(MortgageTrackMixCalculator.TrackResult::loanPortion).sum();
        assertThat(sum).isCloseTo(1_000_000, offset(0.01));
    }
}
