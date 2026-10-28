package com.nadlanitica.lottery;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** הרשימה ההיסטורית היא מקומית - כשהאתר של משרד השיכון לא עונה, היא חייבת להמשיך לעבוד (בלי הסטטוס החי). */
class LotteryProjectsResilienceTest {

    @Test
    void historicalListStillLoadsWhenTheLiveBoardIsDown() {
        MochLotteryApiClient failingClient = new MochLotteryApiClient() {
            @Override
            List<MochProjectItem> fetchProjectsBoard() {
                throw new ResourceAccessException("timeout");
            }
        };
        LotteryOddsEngine engine = new LotteryOddsEngine();
        LotteryController controller = new LotteryController(engine, new PeripheryGrantCities(),
                new LotteryLiveService(failingClient));

        List<ProjectSummary> projects = controller.projects();

        assertThat(projects).hasSize(engine.allProjects().size());
        assertThat(projects).allSatisfy(p -> assertThat(p.liveStatus()).isEqualTo("UNKNOWN"));
    }
}
