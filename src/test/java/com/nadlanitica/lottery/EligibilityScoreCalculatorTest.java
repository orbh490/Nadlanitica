package com.nadlanitica.lottery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityScoreCalculatorTest {

    private final EligibilityScoreCalculator calculator = new EligibilityScoreCalculator();

    @Test
    void zeroProfileScoresZero() {
        EligibilityProfile profile = new EligibilityProfile(0, false, false, false, false, false, false, 0);
        assertThat(calculator.calculateScore(profile)).isEqualTo(0);
    }

    @Test
    void childrenCapAtThreeForScoring() {
        EligibilityProfile profile = new EligibilityProfile(5, false, false, false, false, false, false, 0);
        assertThat(calculator.calculateScore(profile)).isEqualTo(30); // capped at 3 * 10
    }

    @Test
    void failedApplicationsCapAtFour() {
        EligibilityProfile profile = new EligibilityProfile(0, false, false, false, false, false, false, 10);
        assertThat(calculator.calculateScore(profile)).isEqualTo(12); // capped at 4 * 3
    }

    @Test
    void allCriteriaSumCorrectly() {
        EligibilityProfile profile = new EligibilityProfile(3, true, true, false, true, false, true, 2);
        // 30 (children) + 10 (service) + 20 (immigrant) + 15 (disability) + 8 (income) + 6 (2 failed apps)
        assertThat(calculator.calculateScore(profile)).isEqualTo(89);
    }

    @Test
    void scoreBelowMinimumHasNoBracket() {
        assertThat(calculator.determineBracket(10)).isEmpty();
    }

    @Test
    void bracketBoundariesAreCorrect() {
        assertThat(calculator.determineBracket(11).orElseThrow().label()).isEqualTo("11-54 נקודות");
        assertThat(calculator.determineBracket(54).orElseThrow().unitSharePercent()).isEqualTo(25.0);
        assertThat(calculator.determineBracket(55).orElseThrow().label()).isEqualTo("55-64 נקודות");
        assertThat(calculator.determineBracket(64).orElseThrow().unitSharePercent()).isEqualTo(50.0);
        assertThat(calculator.determineBracket(65).orElseThrow().label()).isEqualTo("65+ נקודות");
        assertThat(calculator.determineBracket(200).orElseThrow().unitSharePercent()).isEqualTo(25.0);
    }
}
