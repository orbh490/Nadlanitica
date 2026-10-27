package com.nadlanitica.landtender;

import com.nadlanitica.mortgage.ApartmentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record SelfBuildRequest(
        @PositiveOrZero(message = "מחיר הקרקע לא יכול להיות שלילי") double landPrice,
        @PositiveOrZero(message = "עלות הפיתוח לא יכולה להיות שלילית") double developmentCost,
        @DecimalMin(value = "0", message = "אחוז ההנחה חייב להיות בין 0 ל-100")
        @DecimalMax(value = "100", message = "אחוז ההנחה חייב להיות בין 0 ל-100") double discountPercent,
        @PositiveOrZero(message = "ערבות המכרז לא יכולה להיות שלילית") double guaranteeDeposit,
        @PositiveOrZero(message = "תקציב הבנייה לא יכול להיות שלילי") double constructionBudget,
        @PositiveOrZero(message = "הון עצמי לא יכול להיות שלילי") double availableEquity,
        @PositiveOrZero(message = "הכנסה חודשית לא יכולה להיות שלילית") double monthlyIncome,
        @NotNull(message = "יש לבחור סוג רכישה") ApartmentCategory category,
        // 0 = ברירת המחדל (20 שנה) - ראו SelfBuildCalculatorService
        @Min(value = 0, message = "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה")
        @Max(value = 40, message = "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה") int termYears) {
}

@RestController
@RequestMapping("/api/self-build")
class SelfBuildController {

    private final SelfBuildCalculatorService calculatorService;

    SelfBuildController(SelfBuildCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/calculate")
    SelfBuildCalculatorService.SelfBuildSummary calculate(@Valid @RequestBody SelfBuildRequest req) {
        return calculatorService.calculate(req.landPrice(), req.developmentCost(), req.discountPercent(),
                req.guaranteeDeposit(), req.constructionBudget(), req.availableEquity(), req.monthlyIncome(),
                req.category(), req.termYears());
    }
}
