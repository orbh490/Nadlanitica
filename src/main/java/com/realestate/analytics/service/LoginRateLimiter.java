package com.realestate.analytics.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * הגנה בסיסית מפני ניחוש סיסמאות בכוח גס: אחרי כמה כישלונות רצופים על אותו אימייל, חוסמים ניסיונות נוספים לזמן קצוב.
 * ponytail: המונה בזיכרון (per-instance) ולא ב-DB - מספיק כל עוד האפליקציה רצה כאינסטנס יחיד (כמו ה-H2 עצמו).
 * אם בעתיד ירוצו כמה אינסטנסים מאחורי load balancer, יש להעביר את זה למקור משותף (Redis וכו').
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(AtomicInteger count, AtomicReference<Instant> start) {
    }

    private final ConcurrentHashMap<String, Window> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkNotBlocked(String email) {
        Window window = attemptsByEmail.get(key(email));
        if (window == null || isExpired(window)) {
            return;
        }
        if (window.count().get() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "יותר מדי ניסיונות התחברות כושלים - נסו שוב בעוד כ-15 דקות");
        }
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(key(email), (k, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Window(new AtomicInteger(1), new AtomicReference<>(Instant.now()));
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(key(email));
    }

    private boolean isExpired(Window window) {
        return Duration.between(window.start().get(), Instant.now()).compareTo(WINDOW) > 0;
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
