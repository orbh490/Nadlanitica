package com.realestate.analytics.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class BuyerSession {

    @Id
    private String id;
    private Instant createdAt;
    private String userId;

    private double availableEquity;
    private double monthlyIncome;
    @Enumerated(EnumType.STRING)
    private ApartmentCategory category;
    private double marketValue;
    private double purchasePrice;
    private boolean subsidized;
    private boolean usedBroker;
    private int termYears;

    protected BuyerSession() {
        // עבור Hibernate
    }

    public BuyerSession(String id, String userId, double availableEquity, double monthlyIncome, ApartmentCategory category,
            double marketValue, double purchasePrice, boolean subsidized,
            boolean usedBroker, int termYears) {
        this.id = id;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.availableEquity = availableEquity;
        this.monthlyIncome = monthlyIncome;
        this.category = category;
        this.marketValue = marketValue;
        this.purchasePrice = purchasePrice;
        this.subsidized = subsidized;
        this.usedBroker = usedBroker;
        this.termYears = termYears;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public double getAvailableEquity() { return availableEquity; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public ApartmentCategory getCategory() { return category; }
    public double getMarketValue() { return marketValue; }
    public double getPurchasePrice() { return purchasePrice; }
    public boolean isSubsidized() { return subsidized; }
    public boolean isUsedBroker() { return usedBroker; }
    public int getTermYears() { return termYears; }
}
