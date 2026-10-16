package com.realestate.analytics.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeadRateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        LeadRateLimiter limiter = new LeadRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.checkAndRecord("1.2.3.4")).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> limiter.checkAndRecord("1.2.3.4")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void differentIpsAreTrackedSeparately() {
        LeadRateLimiter limiter = new LeadRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAndRecord("1.1.1.1");
        }
        assertThatCode(() -> limiter.checkAndRecord("2.2.2.2")).doesNotThrowAnyException();
    }
}
