package com.nadlanitica.common;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/** ערך שהאפליקציה זוכרת בין הפעלות (מפתח-ערך פשוט), למשל ריבית בנק ישראל האחרונה שנשלפה. */
@Entity
public class AppState {

    @Id
    private String stateKey;
    private String stateValue;
    private Instant updatedAt;

    protected AppState() {
        // עבור Hibernate
    }

    public AppState(String stateKey, String stateValue) {
        this.stateKey = stateKey;
        this.stateValue = stateValue;
        this.updatedAt = Instant.now();
    }

    public String getStateKey() { return stateKey; }
    public String getStateValue() { return stateValue; }
    public Instant getUpdatedAt() { return updatedAt; }
}
