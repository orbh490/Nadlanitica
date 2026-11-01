package com.nadlanitica.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailboxKeyTest {

    @Test
    void plusAliasIsTheSameMailbox() {
        assertThat(MailboxKey.of("orbh+1@walla.co.il")).isEqualTo("orbh@walla.co.il");
        assertThat(MailboxKey.of("orbh+news+x@outlook.com")).isEqualTo("orbh@outlook.com");
    }

    @Test
    void gmailIgnoresDotsAndGooglemail() {
        assertThat(MailboxKey.of(" O.r.Bh+test@GoogleMail.com ")).isEqualTo("orbh@gmail.com");
        assertThat(MailboxKey.of("or.bh@gmail.com")).isEqualTo("orbh@gmail.com");
    }

    @Test
    void dotsCountOutsideGmail() {
        // באאוטלוק ובשאר הספקים נקודה היא חלק אמיתי מהשם - שתי תיבות שונות
        assertThat(MailboxKey.of("or.bh@outlook.com")).isNotEqualTo(MailboxKey.of("orbh@outlook.com"));
    }

    @Test
    void oddInputDoesNotThrow() {
        assertThat(MailboxKey.of(null)).isEmpty();
        assertThat(MailboxKey.of("no-at-sign")).isEqualTo("no-at-sign");
        assertThat(MailboxKey.of("+tag@x.com")).isEqualTo("+tag@x.com");
    }
}
