package com.realestate.analytics.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/** טוקן התחברות אטום (לא JWT) - נבדק מול הטבלה הזו בכל בקשה מאומתת, כדי שאפשר יהיה גם לבטל אותו (התנתקות). */
@Entity
public class UserSession {

    @Id
    private String token;
    private String userId;
    private Instant createdAt;
    private Instant expiresAt;

    protected UserSession() {
        // עבור Hibernate
    }

    public UserSession(String token, String userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
