package com.nadlanitica.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminGuardTest {

    @Test
    void correctKeyPasses() {
        AdminGuard guard = new AdminGuard("secret123");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> guard.check("secret123"))).isNull();
    }

    @Test
    void wrongKeyIsRejected() {
        AdminGuard guard = new AdminGuard("secret123");
        assertThatThrownBy(() -> guard.check("wrong")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void nullKeyIsRejected() {
        AdminGuard guard = new AdminGuard("secret123");
        assertThatThrownBy(() -> guard.check(null)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blankConfiguredKeyAlwaysRejects() {
        // אם ADMIN_KEY לא הוגדר בסביבה בכלל - אסור שזה "ייפתח" לכל אחד
        AdminGuard guard = new AdminGuard("");
        assertThatThrownBy(() -> guard.check("")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void tooManyWrongGuessesBlockEvenTheRightKeyUntilTheWindowEnds() {
        MutableClock clock = new MutableClock();
        AdminGuard guard = new AdminGuard("secret123", clock);
        for (int i = 0; i < AdminGuard.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> guard.check("guess")).isInstanceOf(ResponseStatusException.class);
        }

        assertThatThrownBy(() -> guard.check("secret123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        clock.advance(AdminGuard.WINDOW.plusSeconds(1));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> guard.check("secret123"))).isNull();
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }
}
