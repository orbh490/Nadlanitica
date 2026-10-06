package com.realestate.analytics.controller;

import com.realestate.analytics.model.EligibilityProfile;
import com.realestate.analytics.service.EligibilityScoreCalculator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record EligibilityRequest(int numberOfChildren, boolean completedMilitaryOrNationalService, boolean newImmigrant,
        boolean disabledCoupleExemptFromService, boolean medicalDisability65Plus, boolean earningIncapacity75Plus,
        boolean lowIncomeHousehold, int failedPriorApplications, boolean reservistEligible) {
}

record EligibilityResponse(int score, boolean meetsMinimumThreshold, String bracketLabel, Double unitSharePercent,
        boolean reservistEligible) {
}

@RestController
@RequestMapping("/api/lottery")
class EligibilityController {

    private final EligibilityScoreCalculator calculator;

    EligibilityController(EligibilityScoreCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/eligibility-score")
    EligibilityResponse score(@RequestBody EligibilityRequest req) {
        EligibilityProfile profile = new EligibilityProfile(req.numberOfChildren(), req.completedMilitaryOrNationalService(),
                req.newImmigrant(), req.disabledCoupleExemptFromService(), req.medicalDisability65Plus(),
                req.earningIncapacity75Plus(), req.lowIncomeHousehold(), req.failedPriorApplications(), req.reservistEligible());
        int score = calculator.calculateScore(profile);
        var bracket = calculator.determineBracket(score);
        return new EligibilityResponse(score, bracket.isPresent(),
                bracket.map(EligibilityScoreCalculator.ScoreBracket::label).orElse(null),
                bracket.map(EligibilityScoreCalculator.ScoreBracket::unitSharePercent).orElse(null),
                req.reservistEligible());
    }
}
