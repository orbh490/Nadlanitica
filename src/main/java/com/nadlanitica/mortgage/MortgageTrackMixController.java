package com.nadlanitica.mortgage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

record TrackMixRequest(
        @PositiveOrZero(message = "סכום ההלוואה לא יכול להיות שלילי") double loanAmount,
        @Min(value = 1, message = "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה")
        @Max(value = 40, message = "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה") int termYears,
        @DecimalMin(value = "0", message = "אחוז מסלול חייב להיות בין 0 ל-100")
        @DecimalMax(value = "100", message = "אחוז מסלול חייב להיות בין 0 ל-100") double fixedNonIndexedPercent,
        @DecimalMin(value = "0", message = "אחוז מסלול חייב להיות בין 0 ל-100")
        @DecimalMax(value = "100", message = "אחוז מסלול חייב להיות בין 0 ל-100") double cpiLinkedPercent,
        @DecimalMin(value = "0", message = "אחוז מסלול חייב להיות בין 0 ל-100")
        @DecimalMax(value = "100", message = "אחוז מסלול חייב להיות בין 0 ל-100") double primePercent) {
}

@RestController
@RequestMapping("/api/mortgage")
class MortgageTrackMixController {

    private static final double PERCENT_TOLERANCE = 0.01;

    private final MortgageTrackMixCalculator calculator;

    MortgageTrackMixController(MortgageTrackMixCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/track-mix")
    MortgageTrackMixCalculator.TrackMixResult trackMix(@Valid @RequestBody TrackMixRequest req) {
        double sum = req.fixedNonIndexedPercent() + req.cpiLinkedPercent() + req.primePercent();
        if (Math.abs(sum - 100) > PERCENT_TOLERANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "האחוזים של המסלולים חייבים להסתכם ל-100%");
        }
        return calculator.calculate(req.loanAmount(), req.termYears(), req.fixedNonIndexedPercent(),
                req.cpiLinkedPercent(), req.primePercent());
    }
}
