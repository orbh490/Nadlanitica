package com.realestate.analytics.service;

import com.realestate.analytics.model.ApartmentCategory;
import com.realestate.analytics.model.BuyerProfile;
import com.realestate.analytics.model.Property;
import org.springframework.stereotype.Service;

/**
 * מחשבון "בנייה עצמית": קניית מגרש ממכרז רמ"י + עלויות פיתוח + תקציב בנייה עצמאי.
 * שימו לב: בשונה מדירה מוכנה, מס הרכישה חל על שווי הקרקע בלבד (לא על עלות הבנייה שמגיעה אחר כך),
 * ואילו דרישת ההון העצמי של הבנק (הוראה 329) בדרך כלל נבחנת מול שווי הפרויקט הכולל (קרקע + בנייה) -
 * מדיניות הבנק המדויקת למשכנתא לבנייה עצמית משתנה בין בנקים, ולכן זו הערכה ולא התחייבות.
 */
@Service
public class SelfBuildCalculatorService {

    private final MortgageRuleEngine mortgageRuleEngine;
    private final PurchaseTaxCalculator purchaseTaxCalculator;
    private final TransparentCostEstimator costEstimator;
    private final MortgagePaymentCalculator paymentCalculator;
    private final MortgageInsuranceEstimator insuranceEstimator;

    public SelfBuildCalculatorService(MortgageRuleEngine mortgageRuleEngine, PurchaseTaxCalculator purchaseTaxCalculator,
            TransparentCostEstimator costEstimator, MortgagePaymentCalculator paymentCalculator,
            MortgageInsuranceEstimator insuranceEstimator) {
        this.mortgageRuleEngine = mortgageRuleEngine;
        this.purchaseTaxCalculator = purchaseTaxCalculator;
        this.costEstimator = costEstimator;
        this.paymentCalculator = paymentCalculator;
        this.insuranceEstimator = insuranceEstimator;
    }

    public record SelfBuildSummary(double landCostAfterDiscount, double purchaseTax, double closingCosts,
            double guaranteeDeposit, double totalCashNeeded, boolean canAffordEquity, double totalPropertyValue,
            double loanAmount, int termYears, double monthlyPayment, double annualRatePercent, double dtiPercent,
            boolean withinLegalDtiLimit, MortgageInsuranceEstimator.InsuranceEstimate insuranceEstimate,
            double totalMonthlyWithInsurance) {
    }

    public SelfBuildSummary calculate(double landPrice, double developmentCost, double discountPercent,
            double guaranteeDeposit, double constructionBudget, double availableEquity, double monthlyIncome,
            ApartmentCategory category, int termYears) {
        double discount = landPrice * Math.max(0, Math.min(100, discountPercent)) / 100;
        double landCostAfterDiscount = Math.max(0, landPrice - discount) + Math.max(0, developmentCost);

        double purchaseTax = purchaseTaxCalculator.calculatePurchaseTax(landCostAfterDiscount, category);
        var closingCosts = costEstimator.estimateClosingCosts(landCostAfterDiscount, false);

        double totalPropertyValue = landCostAfterDiscount + Math.max(0, constructionBudget);
        BuyerProfile buyer = new BuyerProfile(availableEquity, monthlyIncome, category);
        Property property = new Property(totalPropertyValue, totalPropertyValue, false);
        double requiredEquity = mortgageRuleEngine.calculateRequiredEquity(buyer, property);

        double totalCashNeeded = requiredEquity + purchaseTax + closingCosts.total() + Math.max(0, guaranteeDeposit);
        boolean canAffordEquity = availableEquity >= totalCashNeeded;

        double loanAmount = Math.max(0, totalPropertyValue - availableEquity);
        int effectiveTermYears = termYears > 0 ? termYears : 20;
        double monthlyPayment = paymentCalculator.monthlyPayment(loanAmount, effectiveTermYears);
        double annualRate = paymentCalculator.annualRatePercentForTerm(effectiveTermYears);
        double dtiPercent = paymentCalculator.dtiPercent(monthlyPayment, monthlyIncome);
        boolean withinDti = paymentCalculator.withinLegalDtiLimit(monthlyPayment, monthlyIncome);
        var insurance = insuranceEstimator.estimateMonthlyCost(loanAmount);

        return new SelfBuildSummary(landCostAfterDiscount, purchaseTax, closingCosts.total(),
                Math.max(0, guaranteeDeposit), totalCashNeeded, canAffordEquity, totalPropertyValue, loanAmount,
                effectiveTermYears, monthlyPayment, annualRate, dtiPercent, withinDti, insurance,
                monthlyPayment + insurance.totalMonthly());
    }
}
