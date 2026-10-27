package com.nadlanitica.lottery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LotteryOddsEngineTest {

    private final LotteryOddsEngine engine = new LotteryOddsEngine();

    @Test
    void localResidentUsesReservedQuotaPool() {
        LotteryProject project = new LotteryProject("עיר", "פרויקט", "1", 100, 40, 1000, 200, -1, -1, 0);
        assertThat(engine.calculateOddsPercent(project, true)).isEqualTo(20.0); // 40/200
    }

    @Test
    void generalApplicantUsesOpenPool() {
        LotteryProject project = new LotteryProject("עיר", "פרויקט", "1", 100, 40, 1000, 200, -1, -1, 0);
        assertThat(engine.calculateOddsPercent(project, false)).isCloseTo(7.5, org.assertj.core.data.Offset.offset(0.01)); // 60/800
    }

    @Test
    void oddsAreCappedAt100Percent() {
        LotteryProject project = new LotteryProject("עיר", "פרויקט", "1", 100, 50, 1000, 10, -1, -1, 0);
        assertThat(engine.calculateOddsPercent(project, true)).isEqualTo(100.0); // 50/10 > 100%
    }

    @Test
    void noApplicantsInPoolMeansFullOddsIfUnitsExist() {
        LotteryProject project = new LotteryProject("עיר", "פרויקט", "1", 100, 50, 1000, 0, -1, -1, 0);
        assertThat(engine.calculateOddsPercent(project, true)).isEqualTo(100.0);
    }


    @Test
    void loadsRealMinistryOfHousingDataFromClasspath() {
        assertThat(engine.allProjects()).hasSizeGreaterThan(2000);
    }

    @Test
    void findsKnownRealProjectAndMatchesOfficialNumbers() {
        LotteryProject project = engine.findProject("מגדל העמק", "מגרשים 16A,16B,16P,17A,17B,17P,18-19").orElseThrow();
        assertThat(project.getTotalUnits()).isEqualTo(79);
        assertThat(project.getLocalQuotaUnits()).isEqualTo(39);
        assertThat(project.getTotalApplicants()).isEqualTo(6594);
        assertThat(project.getLocalApplicants()).isEqualTo(530);
        assertThat(engine.calculateOddsPercent(project, true)).isCloseTo(7.36, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void findProjectIsCaseInsensitiveAndMissingProjectIsEmpty() {
        assertThat(engine.findProject("לא קיים", "לא קיים")).isEmpty();
    }

    @Test
    void historicalOddsAreEmptyWhenLocalWinnersNotPublished() {
        // בפרויקט זה WinnersBneyMakom="-" במקור - לא פורסם, לא 0
        LotteryProject project = engine.findProject("מגדל העמק", "מגרשים 16A,16B,16P,17A,17B,17P,18-19").orElseThrow();
        assertThat(engine.actualHistoricalOddsPercent(project, true)).isEmpty();
        assertThat(engine.actualHistoricalOddsPercent(project, false)).isEmpty();
    }

    @Test
    void historicalOddsMatchPublishedWinnerCountsWhenAvailable() {
        LotteryProject project = engine.findProject("קרית אונו", "דרום").orElseThrow();
        assertThat(engine.actualHistoricalOddsPercent(project, true).getAsDouble())
                .isCloseTo(0.279, org.assertj.core.data.Offset.offset(0.01)); // 1 local winner / 358 local applicants
        assertThat(engine.actualHistoricalOddsPercent(project, false).getAsDouble()).isEqualTo(0.0); // 0 non-local winners
    }

    @Test
    void cityComparisonExcludesCitiesBelowMinimumProjectCount() {
        var stats = engine.compareCities(false, 3, 15);
        assertThat(stats).allSatisfy(s -> assertThat(s.projectCount()).isGreaterThanOrEqualTo(3));
    }

    @Test
    void cityComparisonIsSortedByOddsDescendingAndRespectsLimit() {
        var stats = engine.compareCities(false, 3, 5);
        assertThat(stats).hasSizeLessThanOrEqualTo(5);
        assertThat(stats).isSortedAccordingTo((a, b) -> Double.compare(b.avgOddsPercent(), a.avgOddsPercent()));
    }
}
