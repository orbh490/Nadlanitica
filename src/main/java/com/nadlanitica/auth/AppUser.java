package com.nadlanitica.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class AppUser {

    @Id
    private String id;
    @Column(unique = true, nullable = false)
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private boolean emailVerified;

    protected AppUser() {
        // עבור Hibernate
    }

    public AppUser(String id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.emailVerified = false;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
