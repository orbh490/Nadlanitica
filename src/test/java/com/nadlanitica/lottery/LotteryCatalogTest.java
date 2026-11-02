package com.nadlanitica.lottery;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** הגרלה שנפתחת להרשמה מופיעה ברשימה ואפשר לחשב בה סיכוי, גם כשאין לה עדיין שורה בקובץ ההיסטורי. */
class LotteryCatalogTest {

    private final LotteryOddsEngine engine = new LotteryOddsEngine();

    private static MochLotteryApiClient.MochProjectItem board(String lotteryNumber, LocalDate start, LocalDate end,
            int units, int localUnits, int subscribers, int localSubscribers) {
        return new MochLotteryApiClient.MochProjectItem(lotteryNumber, start + "T00:00:00", end + "T00:00:00",
                "מחיר מופחת", "P-" + lotteryNumber, "עיר בדיקה", "מגרש 7", "שכונה", "קבלן", units + 20, units, 10000.0,
                "מכרז", "חסרי דירה", subscribers, localSubscribers, localUnits, end.plusMonths(1) + "T00:00:00");
    }

    private LotteryController controllerWithBoard(MochLotteryApiClient.MochProjectItem... items) {
        MochLotteryApiClient client = new MochLotteryApiClient() {
            @Override
            List<MochProjectItem> fetchProjectsBoard() {
                return List.of(items);
            }
        };
        LotteryLiveService live = new LotteryLiveService(client);
        live.refreshInBackground();
        return new LotteryController(engine, new PeripheryGrantCities(), live, new LotteryCatalog(engine, live));
    }

    @Test
    void openLotteryMissingFromHistoryIsListedWithOdds() {
        LocalDate today = LocalDate.now();
        LotteryController controller = controllerWithBoard(
                board("990001", today.minusDays(3), today.plusDays(10), 100, 20, 4000, 500));

        List<ProjectSummary> projects = controller.projects();
        assertThat(projects).hasSize(engine.allProjects().size() + 1);
        ProjectSummary open = projects.stream().filter(p -> p.lotteryId().equals("990001")).findFirst().orElseThrow();
        assertThat(open.liveStatus()).isEqualTo("OPEN");
        assertThat(open.fromLiveBoard()).isTrue();
        assertThat(open.city()).isEqualTo("עיר בדיקה");

        // כלל הארץ: 80 יחידות מול 3,500 נרשמים; בני מקום: 20 מול 500
        LotteryOddsResponse general = controller.odds("990001", false);
        assertThat(general.poolUnits()).isEqualTo(80);
        assertThat(general.poolApplicants()).isEqualTo(3500);
        assertThat(general.winProbabilityPercent()).isCloseTo(80 * 100.0 / 3500, org.assertj.core.data.Offset.offset(0.001));
        assertThat(general.fromLiveBoard()).isTrue();
        assertThat(general.actualHistoricalWinRatePercent()).isNull();
        assertThat(controller.odds("990001", true).winProbabilityPercent()).isEqualTo(4.0);
    }

    @Test
    void upcomingAndClosedLiveLotteriesGetTheirStatus() {
        LocalDate today = LocalDate.now();
        LotteryController controller = controllerWithBoard(
                board("990002", today.plusDays(5), today.plusDays(30), 50, 10, 0, 0),
                board("990003", today.minusDays(60), today.minusDays(30), 50, 10, 900, 100));

        var byId = controller.projects().stream()
                .filter(ProjectSummary::fromLiveBoard)
                .collect(java.util.stream.Collectors.toMap(ProjectSummary::lotteryId, ProjectSummary::liveStatus));
        assertThat(byId).containsEntry("990002", "NOT_YET_OPEN").containsEntry("990003", "NONE");
    }

    @Test
    void lotteryAlreadyInHistoryIsNotDuplicated() {
        LocalDate today = LocalDate.now();
        LotteryController controller = controllerWithBoard(
                board("2564", today.minusDays(3), today.plusDays(10), 100, 20, 4000, 500));

        assertThat(controller.projects()).hasSize(engine.allProjects().size());
        assertThat(controller.projects().stream().filter(p -> p.lotteryId().equals("2564")))
                .singleElement().satisfies(p -> assertThat(p.fromLiveBoard()).isFalse());
    }
}
