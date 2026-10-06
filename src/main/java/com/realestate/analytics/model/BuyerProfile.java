package com.realestate.analytics.model;

public class BuyerProfile {
    private double availableEquity;   // הון עצמי קיים
    private double monthlyIncome;     // הכנסה חודשית נטו
    private ApartmentCategory category; // דירה יחידה / חליפית / להשקעה

    public BuyerProfile(double availableEquity, double monthlyIncome, ApartmentCategory category) {
        this.availableEquity = availableEquity;
        this.monthlyIncome = monthlyIncome;
        this.category = category;
    }

    public double getAvailableEquity() { return availableEquity; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public ApartmentCategory getCategory() { return category; }
}
