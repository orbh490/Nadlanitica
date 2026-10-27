package com.nadlanitica.mortgage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record WaitingPeriodRequest(
        @PositiveOrZero(message = "מחיר הדירה לא יכול להיות שלילי") double purchasePrice,
        @DecimalMin(value = "0", message = "שיעור ההצמדה השנתי חייב להיות בין 0% ל-30%")
        @DecimalMax(value = "30", message = "שיעור ההצמדה השנתי חייב להיות בין 0% ל-30%") double assumedAnnualIndexRatePercent,
        @Min(value = 0, message = "תקופת ההמתנה חייבת להיות בין 0 ל-15 שנה")
        @Max(value = 15, message = "תקופת ההמתנה חייבת להיות בין 0 ל-15 שנה") int waitYears) {
}

record WaitingPeriodResponse(double extraCost, double totalProjectedCost) {
}

@RestController
@RequestMapping("/api/costs")
class WaitingPeriodController {

    private final TransparentCostEstimator estimator;

    WaitingPeriodController(TransparentCostEstimator estimator) {
        this.estimator = estimator;
    }

    @PostMapping("/waiting-period-projection")
    WaitingPeriodResponse project(@Valid @RequestBody WaitingPeriodRequest req) {
        double extra = estimator.estimateWaitingPeriodExtraCost(req.purchasePrice(), req.assumedAnnualIndexRatePercent(), req.waitYears());
        return new WaitingPeriodResponse(extra, req.purchasePrice() + extra);
    }
}
