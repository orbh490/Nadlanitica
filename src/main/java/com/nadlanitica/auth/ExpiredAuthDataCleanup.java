package com.nadlanitica.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * sessions וטוקנים חד-פעמיים שפג תוקפם כבר לא מאפשרים כלום (נבדקים מול expiresAt בכל שימוש), אבל בלי ניקוי
 * הם נשארים בטבלאות לנצח - גם מידע מיותר על משתמשים וגם טבלה שרק גדלה.
 */
@Component
class ExpiredAuthDataCleanup {

    private static final Logger log = LoggerFactory.getLogger(ExpiredAuthDataCleanup.class);

    private final UserSessionRepository sessionRepository;
    private final AccountTokenRepository tokenRepository;

    ExpiredAuthDataCleanup(UserSessionRepository sessionRepository, AccountTokenRepository tokenRepository) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void deleteExpired() {
        Instant now = Instant.now();
        int sessions = sessionRepository.deleteExpired(now);
        int tokens = tokenRepository.deleteExpired(now);
        log.info("ניקוי יומי: נמחקו {} sessions ו-{} טוקנים שפג תוקפם", sessions, tokens);
    }
}
