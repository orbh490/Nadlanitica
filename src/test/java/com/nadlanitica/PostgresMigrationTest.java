package com.nadlanitica;

import com.nadlanitica.account.InterestArea;
import com.nadlanitica.account.InterestAreaRepository;
import com.nadlanitica.auth.AppUser;
import com.nadlanitica.auth.AppUserRepository;
import com.nadlanitica.auth.UserSession;
import com.nadlanitica.auth.UserSessionRepository;
import com.nadlanitica.leads.MortgageLead;
import com.nadlanitica.leads.MortgageLeadRepository;
import com.nadlanitica.mortgage.ApartmentCategory;
import com.nadlanitica.mortgage.BuyerSession;
import com.nadlanitica.mortgage.BuyerSessionRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * שאר הטסטים רצים על H2 במצב תאימות ל-PostgreSQL. הטסט הזה מריץ PostgreSQL אמיתי, כדי לוודא שהמיגרציות
 * של Flyway עוברות על המסד של פרודקשן, ש-Hibernate מאשר שהישויות תואמות לטבלאות (ddl-auto=validate),
 * ושהאורכים והמחיקה המדורגת עובדים כמו שצריך.
 */
@SpringBootTest
class PostgresMigrationTest {

    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException("לא הצלחנו להפעיל PostgreSQL מקומי לטסט", e);
        }
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @AfterAll
    static void stop() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    AppUserRepository users;
    @Autowired
    UserSessionRepository sessions;
    @Autowired
    BuyerSessionRepository buyerSessions;
    @Autowired
    InterestAreaRepository interestAreas;
    @Autowired
    MortgageLeadRepository leads;

    @Test
    void leadFieldsUpToTheApiLimitFitInTheColumns() {
        // ה-API מאפשר עד 300 תווים - ב-varchar(255) של ברירת המחדל זה היה נכשל ב-PostgreSQL
        String max = "א".repeat(300);
        MortgageLead saved = leads.save(new MortgageLead(max, max, max, max));
        assertThat(leads.findById(saved.getId())).isPresent();
    }

    @Test
    void deletingAUserDeletesAllTheirData() {
        users.save(new AppUser("pg-user", "pg@example.test", "hash"));
        sessions.save(new UserSession("pg-session", "pg-user", Instant.now().plus(Duration.ofDays(1))));
        buyerSessions.save(new BuyerSession("pg-calc", "pg-user", 1, 1, ApartmentCategory.SINGLE, 1, 1, false, false, 20));
        interestAreas.save(new InterestArea("pg-area", "pg-user", "חיפה"));

        users.deleteById("pg-user");

        assertThat(sessions.existsById("pg-session")).isFalse();
        assertThat(buyerSessions.existsById("pg-calc")).isFalse();
        assertThat(interestAreas.existsById("pg-area")).isFalse();
    }
}
