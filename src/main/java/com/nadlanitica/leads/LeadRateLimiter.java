package com.nadlanitica.leads;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * הגנה בסיסית מפני ספאם על טופס הלידים הציבורי (לא-מאומת, בלי captcha) - מגבילה לפי כתובת IP.
 * ponytail: אותה גישה כמו LoginRateLimiter (זיכרון per-instance, לא Redis) - ראו שם למה זה מספיק כרגע.
 */
@Component
public class LeadRateLimiter {

    private static final int MAX_SUBMISSIONS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(AtomicInteger count, AtomicReference<Instant> start) {
    }

    private final ConcurrentHashMap<String, Window> submissionsByIp = new ConcurrentHashMap<>();

    public void checkAndRecord(String ip) {
        Window window = submissionsByIp.compute(ip == null ? "" : ip, (k, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Window(new AtomicInteger(0), new AtomicReference<>(Instant.now()));
            }
            return existing;
        });
        if (window.count().incrementAndGet() > MAX_SUBMISSIONS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "יותר מדי בקשות - נסו שוב בעוד כמה דקות");
        }
    }

    private boolean isExpired(Window window) {
        return Duration.between(window.start().get(), Instant.now()).compareTo(WINDOW) > 0;
    }
}
