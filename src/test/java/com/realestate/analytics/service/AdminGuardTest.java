package com.realestate.analytics.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

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
}
