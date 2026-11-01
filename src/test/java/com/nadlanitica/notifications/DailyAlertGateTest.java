package com.nadlanitica.notifications;

import com.nadlanitica.common.AppState;
import com.nadlanitica.common.AppStateRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyAlertGateTest {

    private final Map<String, AppState> stored = new HashMap<>();
    private final AppStateRepository repo = mock(AppStateRepository.class);

    DailyAlertGateTest() {
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(stored.get(inv.<String>getArgument(0))));
        when(repo.save(any())).thenAnswer(inv -> {
            AppState s = inv.getArgument(0);
            stored.put(s.getStateKey(), s);
            return s;
        });
    }

    private DailyAlertGate gateAt(String instant) {
        return new DailyAlertGate(repo, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void onlyTheFirstAlertOfTheDayGoesOut() {
        DailyAlertGate morning = gateAt("2026-12-01T06:00:00Z");
        assertThat(morning.firstToday("regulatory")).isTrue();
        assertThat(gateAt("2026-12-01T18:00:00Z").firstToday("regulatory")).isFalse();
        // נושא אחר לא מושפע
        assertThat(morning.firstToday("boi")).isTrue();
    }

    @Test
    void nextDaySendsAgain() {
        assertThat(gateAt("2026-12-01T06:00:00Z").firstToday("regulatory")).isTrue();
        assertThat(gateAt("2026-12-02T06:00:00Z").firstToday("regulatory")).isTrue();
    }

    @Test
    void databaseTroubleStillLetsTheAlertOut() {
        when(repo.findById(anyString())).thenThrow(new RuntimeException("db down"));
        assertThat(gateAt("2026-12-01T06:00:00Z").firstToday("regulatory")).isTrue();
    }
}
