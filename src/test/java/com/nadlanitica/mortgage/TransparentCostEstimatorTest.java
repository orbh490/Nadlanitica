package com.nadlanitica.mortgage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class TransparentCostEstimatorTest {

    private final TransparentCostEstimator estimator = new TransparentCostEstimator();

    @Test
    void closingCostsWithoutBrokerIncludeOnlyLawyerAndAppraiser() {
        var costs = estimator.estimateClosingCosts(1_400_000, false);
        assertThat(costs.lawyerFee()).isCloseTo(16_520.0, offset(0.5)); // 1% + 18% VAT
        assertThat(costs.appraiserFee()).isEqualTo(2_000.0);
        assertThat(costs.brokerCommission()).isEqualTo(0.0);
        assertThat(costs.total()).isCloseTo(18_520.0, offset(0.5));
    }

    @Test
    void closingCostsWithBrokerAddCommission() {
        var costs = estimator.estimateClosingCosts(1_400_000, true);
        assertThat(costs.brokerCommission()).isCloseTo(33_040.0, offset(0.5)); // 2% + 18% VAT
        assertThat(costs.total()).isCloseTo(51_560.0, offset(0.5));
    }

    @Test
    void waitingPeriodExtraCostCompoundsAnnually() {
        // 1,400,000 at 3.5%/year for 5 years
        double extra = estimator.estimateWaitingPeriodExtraCost(1_400_000, 3.5, 5);
        assertThat(extra).isCloseTo(262_760.83, offset(1.0));
    }

    @Test
    void noWaitMeansNoExtraCost() {
        assertThat(estimator.estimateWaitingPeriodExtraCost(1_400_000, 3.5, 0)).isEqualTo(0.0);
    }
}
