package com.nadlanitica.mortgage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record EquityRequest(
        @PositiveOrZero(message = "הון עצמי לא יכול להיות שלילי") double availableEquity,
        @PositiveOrZero(message = "הכנסה חודשית לא יכולה להיות שלילית") double monthlyIncome,
        @NotNull(message = "יש לבחור סוג רכישה") ApartmentCategory category,
        @PositiveOrZero(message = "שווי השוק לא יכול להיות שלילי") double marketValue,
        @Positive(message = "יש להזין מחיר רכישה") double purchasePrice,
        boolean subsidized, boolean usedBroker) {
}

record EquityResponse(double requiredEquity, double purchaseTax, TransparentCostEstimator.ClosingCosts closingCosts,
        double totalCashNeeded, boolean canAfford) {
}

@RestController
@RequestMapping("/api/equity")
class EquityController {

    private final MortgageRuleEngine ruleEngine;
    private final PurchaseTaxCalculator taxCalculator;
    private final TransparentCostEstimator costEstimator;

    EquityController(MortgageRuleEngine ruleEngine, PurchaseTaxCalculator taxCalculator, TransparentCostEstimator costEstimator) {
        this.ruleEngine = ruleEngine;
        this.taxCalculator = taxCalculator;
        this.costEstimator = costEstimator;
    }

    @PostMapping("/calculate")
    EquityResponse calculate(@Valid @RequestBody EquityRequest req) {
        BuyerProfile buyer = new BuyerProfile(req.availableEquity(), req.monthlyIncome(), req.category());
        Property property = new Property(req.marketValue(), req.purchasePrice(), req.subsidized());
        double requiredEquity = ruleEngine.calculateRequiredEquity(buyer, property);
        double purchaseTax = taxCalculator.calculatePurchaseTax(property.getPurchasePrice(), buyer.getCategory());
        TransparentCostEstimator.ClosingCosts closingCosts = costEstimator.estimateClosingCosts(property.getPurchasePrice(), req.usedBroker());
        double totalCashNeeded = requiredEquity + purchaseTax + closingCosts.total();
        return new EquityResponse(requiredEquity, purchaseTax, closingCosts, totalCashNeeded,
                buyer.getAvailableEquity() >= totalCashNeeded);
    }
}
