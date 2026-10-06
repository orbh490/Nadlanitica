package com.realestate.analytics.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/** טוקן חד-פעמי לאימות אימייל או איפוס סיסמה - נמחק מיד לאחר שימוש. */
@Entity
public class AccountToken {

    @Id
    private String token;
    private String userId;
    private String purpose;
    private Instant expiresAt;

    protected AccountToken() {
        // עבור Hibernate
    }

    public AccountToken(String token, String userId, String purpose, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
}
