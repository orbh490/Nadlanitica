package com.nadlanitica.lottery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record EligibilityRequest(
        @Min(value = 0, message = "מספר הילדים חייב להיות בין 0 ל-20")
        @Max(value = 20, message = "מספר הילדים חייב להיות בין 0 ל-20") int numberOfChildren,
        boolean completedMilitaryOrNationalService, boolean newImmigrant,
        boolean disabledCoupleExemptFromService, boolean medicalDisability65Plus, boolean earningIncapacity75Plus,
        boolean lowIncomeHousehold,
        @Min(value = 0, message = "מספר ההגרלות הקודמות חייב להיות בין 0 ל-50")
        @Max(value = 50, message = "מספר ההגרלות הקודמות חייב להיות בין 0 ל-50") int failedPriorApplications) {
}

record EligibilityResponse(int score, boolean meetsMinimumThreshold, String bracketLabel, Double unitSharePercent) {
}

@RestController
@RequestMapping("/api/lottery")
class EligibilityController {

    private final EligibilityScoreCalculator calculator;

    EligibilityController(EligibilityScoreCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/eligibility-score")
    EligibilityResponse score(@Valid @RequestBody EligibilityRequest req) {
        EligibilityProfile profile = new EligibilityProfile(req.numberOfChildren(), req.completedMilitaryOrNationalService(),
                req.newImmigrant(), req.disabledCoupleExemptFromService(), req.medicalDisability65Plus(),
                req.earningIncapacity75Plus(), req.lowIncomeHousehold(), req.failedPriorApplications());
        int score = calculator.calculateScore(profile);
        var bracket = calculator.determineBracket(score);
        return new EligibilityResponse(score, bracket.isPresent(),
                bracket.map(EligibilityScoreCalculator.ScoreBracket::label).orElse(null),
                bracket.map(EligibilityScoreCalculator.ScoreBracket::unitSharePercent).orElse(null));
    }
}
