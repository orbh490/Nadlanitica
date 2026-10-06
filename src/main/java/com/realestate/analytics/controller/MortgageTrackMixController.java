package com.realestate.analytics.controller;

import com.realestate.analytics.service.MortgageTrackMixCalculator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record TrackMixRequest(double loanAmount, int termYears, double fixedNonIndexedPercent, double cpiLinkedPercent,
        double primePercent) {
}

@RestController
@RequestMapping("/api/mortgage")
class MortgageTrackMixController {

    private final MortgageTrackMixCalculator calculator;

    MortgageTrackMixController(MortgageTrackMixCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/track-mix")
    MortgageTrackMixCalculator.TrackMixResult trackMix(@RequestBody TrackMixRequest req) {
        return calculator.calculate(req.loanAmount(), req.termYears(), req.fixedNonIndexedPercent(),
                req.cpiLinkedPercent(), req.primePercent());
    }
}
