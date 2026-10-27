package com.nadlanitica.mortgage;

public class Property {
    private double marketValue;       // שווי שוק אמיתי
    private double purchasePrice;     // מחיר רכישה בפועל 
    private boolean isSubsidized;     // האם זו "דירה בהנחה"

    public Property(double marketValue, double purchasePrice, boolean isSubsidized) {
        this.marketValue = marketValue;
        this.purchasePrice = purchasePrice;
        this.isSubsidized = isSubsidized;
    }

    public double getMarketValue() { return marketValue; }
    public double getPurchasePrice() { return purchasePrice; }
    public boolean isSubsidized() { return isSubsidized; }
}
