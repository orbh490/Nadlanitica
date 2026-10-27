package com.nadlanitica.landtender;

import com.nadlanitica.mortgage.ApartmentCategory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record SelfBuildRequest(double landPrice, double developmentCost, double discountPercent, double guaranteeDeposit,
        double constructionBudget, double availableEquity, double monthlyIncome, ApartmentCategory category,
        int termYears) {
}

@RestController
@RequestMapping("/api/self-build")
class SelfBuildController {

    private final SelfBuildCalculatorService calculatorService;

    SelfBuildController(SelfBuildCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/calculate")
    SelfBuildCalculatorService.SelfBuildSummary calculate(@RequestBody SelfBuildRequest req) {
        return calculatorService.calculate(req.landPrice(), req.developmentCost(), req.discountPercent(),
                req.guaranteeDeposit(), req.constructionBudget(), req.availableEquity(), req.monthlyIncome(),
                req.category(), req.termYears());
    }
}
