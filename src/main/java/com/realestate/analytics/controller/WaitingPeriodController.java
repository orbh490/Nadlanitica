package com.realestate.analytics.controller;

import com.realestate.analytics.service.TransparentCostEstimator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record WaitingPeriodRequest(double purchasePrice, double assumedAnnualIndexRatePercent, int waitYears) {
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
    WaitingPeriodResponse project(@RequestBody WaitingPeriodRequest req) {
        double extra = estimator.estimateWaitingPeriodExtraCost(req.purchasePrice(), req.assumedAnnualIndexRatePercent(), req.waitYears());
        return new WaitingPeriodResponse(extra, req.purchasePrice() + extra);
    }
}
