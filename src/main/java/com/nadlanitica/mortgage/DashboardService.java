package com.nadlanitica.mortgage;

import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final MortgageRuleEngine mortgageRuleEngine;
    private final PurchaseTaxCalculator purchaseTaxCalculator;
    private final TransparentCostEstimator costEstimator;
    private final MortgagePaymentCalculator paymentCalculator;
    private final MortgageInsuranceEstimator insuranceEstimator;

    public DashboardService(MortgageRuleEngine mortgageRuleEngine, PurchaseTaxCalculator purchaseTaxCalculator,
            TransparentCostEstimator costEstimator, MortgagePaymentCalculator paymentCalculator,
            MortgageInsuranceEstimator insuranceEstimator) {
        this.mortgageRuleEngine = mortgageRuleEngine;
        this.purchaseTaxCalculator = purchaseTaxCalculator;
        this.costEstimator = costEstimator;
        this.paymentCalculator = paymentCalculator;
        this.insuranceEstimator = insuranceEstimator;
    }

    public record DashboardSummary(double requiredEquity, double purchaseTax, double closingCosts,
            double totalCashNeeded, boolean canAffordEquity, double loanAmount, double maxLoanAmount,
            boolean loanCappedByLtv, double equityShortfall, int termYears, double monthlyPayment,
            double annualRatePercent, double dtiPercent, boolean withinLegalDtiLimit,
            MortgageInsuranceEstimator.InsuranceEstimate insuranceEstimate, double totalMonthlyWithInsurance) {
    }

    public DashboardSummary buildSummary(BuyerSession session) {
        BuyerProfile buyer = new BuyerProfile(session.getAvailableEquity(), session.getMonthlyIncome(),
                session.getCategory());
        Property property = new Property(session.getMarketValue(), session.getPurchasePrice(), session.isSubsidized());

        double requiredEquity = mortgageRuleEngine.calculateRequiredEquity(buyer, property);
        double purchaseTax = purchaseTaxCalculator.calculatePurchaseTax(property.getPurchasePrice(), buyer.getCategory());
        var closingCosts = costEstimator.estimateClosingCosts(property.getPurchasePrice(), session.isUsedBroker());
        double totalCashNeeded = requiredEquity + purchaseTax + closingCosts.total();
        boolean canAffordEquity = buyer.getAvailableEquity() >= totalCashNeeded;

        var loan = mortgageRuleEngine.sizeLoan(property.getPurchasePrice(), requiredEquity, buyer.getAvailableEquity(),
                purchaseTax + closingCosts.total());
        double loanAmount = loan.loanAmount();
        int termYears = session.getTermYears() > 0 ? session.getTermYears() : 20;
        double monthlyPayment = paymentCalculator.monthlyPayment(loanAmount, termYears);
        double annualRate = paymentCalculator.annualRatePercentForTerm(termYears);
        double dtiPercent = paymentCalculator.dtiPercent(monthlyPayment, buyer.getMonthlyIncome());
        boolean withinDti = paymentCalculator.withinLegalDtiLimit(monthlyPayment, buyer.getMonthlyIncome());
        var insurance = insuranceEstimator.estimateMonthlyCost(loanAmount);

        return new DashboardSummary(requiredEquity, purchaseTax, closingCosts.total(), totalCashNeeded,
                canAffordEquity, loanAmount, loan.maxLoanAmount(), loan.cappedByLtv(), loan.equityShortfall(),
                termYears, monthlyPayment, annualRate, dtiPercent, withinDti,
                insurance, monthlyPayment + insurance.totalMonthly());
    }
}
