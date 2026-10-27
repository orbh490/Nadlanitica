package com.nadlanitica.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExpiredAuthDataCleanupTest {

    @Autowired
    ExpiredAuthDataCleanup cleanup;
    @Autowired
    UserSessionRepository sessions;
    @Autowired
    AccountTokenRepository tokens;

    @Test
    void deletesOnlyExpiredSessionsAndTokens() {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        Instant future = Instant.now().plus(Duration.ofHours(1));
        sessions.save(new UserSession("cleanup-expired-session", "u1", past));
        sessions.save(new UserSession("cleanup-valid-session", "u1", future));
        tokens.save(new AccountToken("cleanup-expired-token", "u1", "VERIFY_EMAIL", past));
        tokens.save(new AccountToken("cleanup-valid-token", "u1", "VERIFY_EMAIL", future));

        cleanup.deleteExpired();

        assertThat(sessions.existsById("cleanup-expired-session")).isFalse();
        assertThat(sessions.existsById("cleanup-valid-session")).isTrue();
        assertThat(tokens.existsById("cleanup-expired-token")).isFalse();
        assertThat(tokens.existsById("cleanup-valid-token")).isTrue();
    }
}
