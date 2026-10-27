package com.nadlanitica.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/** אזור עניין שהמשתמש בחר (מקום בארץ). התראות על האזור - לוגיקה עתידית, כאן רק השמירה. */
@Entity
public class InterestArea {

    @Id
    private String id;
    private String userId;
    private String name;
    private Instant createdAt;

    protected InterestArea() {
        // עבור Hibernate
    }

    public InterestArea(String id, String userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
