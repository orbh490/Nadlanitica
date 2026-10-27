package com.nadlanitica.leads;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class MortgageLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt;
    private String fullName;
    private String phone;
    private String email;
    private String message;

    protected MortgageLead() {
        // עבור Hibernate
    }

    public MortgageLead(String fullName, String phone, String email, String message) {
        this.createdAt = Instant.now();
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.message = message;
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getMessage() { return message; }
}
