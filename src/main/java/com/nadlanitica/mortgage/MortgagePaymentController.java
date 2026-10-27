package com.nadlanitica.mortgage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

record MortgagePaymentRequest(double loanAmount, int termYears,
        @PositiveOrZero(message = "הכנסה חודשית לא יכולה להיות שלילית") double monthlyIncome) {
}

record MortgagePaymentResponse(double annualRatePercent, double monthlyPayment, double totalPayment,
        double dtiPercent, boolean withinLegalDtiLimit, MortgageInsuranceEstimator.InsuranceEstimate insuranceEstimate,
        double totalMonthlyWithInsurance) {
}

record RateStressTestRequest(double loanAmount, int termYears,
        @PositiveOrZero(message = "הכנסה חודשית לא יכולה להיות שלילית") double monthlyIncome) {
}

record RateScenario(double extraRatePercent, double annualRatePercent, double monthlyPayment, double dtiPercent,
        boolean withinLegalDtiLimit) {
}

record RateStressTestResponse(double baseAnnualRatePercent, double basePayment, java.util.List<RateScenario> scenarios) {
}

@RestController
@RequestMapping("/api/mortgage")
class MortgagePaymentController {

    private final MortgagePaymentCalculator calculator;
    private final MortgageInsuranceEstimator insuranceEstimator;

    MortgagePaymentController(MortgagePaymentCalculator calculator, MortgageInsuranceEstimator insuranceEstimator) {
        this.calculator = calculator;
        this.insuranceEstimator = insuranceEstimator;
    }

    @PostMapping("/monthly-payment")
    MortgagePaymentResponse monthlyPayment(@Valid @RequestBody MortgagePaymentRequest req) {
        validateLoanInputs(req.loanAmount(), req.termYears());
        double monthly = calculator.monthlyPayment(req.loanAmount(), req.termYears());
        double rate = calculator.annualRatePercentForTerm(req.termYears());
        double dti = calculator.dtiPercent(monthly, req.monthlyIncome());
        boolean withinLimit = calculator.withinLegalDtiLimit(monthly, req.monthlyIncome());
        var insurance = insuranceEstimator.estimateMonthlyCost(req.loanAmount());
        return new MortgagePaymentResponse(rate, monthly, monthly * req.termYears() * 12, dti, withinLimit,
                insurance, monthly + insurance.totalMonthly());
    }

    // "מה קורה אם הריבית עולה" - בדיקה עצמית ולא מבחן שדרוש מפורשות בהוראה 329 (נבדק מול נוסח ההוראה) -
    // תרחישי עלייה מקובלים להמחשה בלבד, כדי שהלווה יראה את הסיכון במסלול צמוד-ריבית לפני שהוא בוחר בו.
    private static final double[] STRESS_SCENARIOS_PERCENT = {1.0, 2.0, 3.0};

    @PostMapping("/rate-stress-test")
    RateStressTestResponse rateStressTest(@Valid @RequestBody RateStressTestRequest req) {
        validateLoanInputs(req.loanAmount(), req.termYears());
        double baseRate = calculator.annualRatePercentForTerm(req.termYears());
        double basePayment = calculator.monthlyPaymentForRate(req.loanAmount(), req.termYears(), baseRate);
        List<RateScenario> scenarios = new java.util.ArrayList<>();
        for (double extra : STRESS_SCENARIOS_PERCENT) {
            double stressedRate = baseRate + extra;
            double payment = calculator.monthlyPaymentForRate(req.loanAmount(), req.termYears(), stressedRate);
            double dti = calculator.dtiPercent(payment, req.monthlyIncome());
            boolean withinLimit = calculator.withinLegalDtiLimit(payment, req.monthlyIncome());
            scenarios.add(new RateScenario(extra, stressedRate, payment, dti, withinLimit));
        }
        return new RateStressTestResponse(baseRate, basePayment, scenarios);
    }

    @GetMapping("/amortization-schedule")
    List<MortgagePaymentCalculator.YearlyAmortization> amortizationSchedule(
            @RequestParam double loanAmount, @RequestParam int termYears) {
        validateLoanInputs(loanAmount, termYears);
        return calculator.amortizationScheduleByYear(loanAmount, termYears);
    }

    // בלי זה, termYears<=0 מייצר חלוקה ב-0 (Infinity/NaN) שמוצג למשתמש כ-200 תקין במקום שגיאה ברורה -
    // טווח ה-UI של תקופת המשכנתא (dashboard.html) לא הגביל את זה בכלל, אז זה נגיש גם מהשימוש הרגיל.
    private static void validateLoanInputs(double loanAmount, int termYears) {
        if (loanAmount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "סכום ההלוואה לא יכול להיות שלילי");
        }
        if (termYears <= 0 || termYears > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "תקופת המשכנתא חייבת להיות בין 1 ל-40 שנה");
        }
    }
}
