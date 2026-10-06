package com.realestate.analytics.controller;

import com.realestate.analytics.service.MortgageInsuranceEstimator;
import com.realestate.analytics.service.MortgagePaymentCalculator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

record MortgagePaymentRequest(double loanAmount, int termYears, double monthlyIncome) {
}

record MortgagePaymentResponse(double annualRatePercent, double monthlyPayment, double totalPayment,
        double dtiPercent, boolean withinLegalDtiLimit, MortgageInsuranceEstimator.InsuranceEstimate insuranceEstimate,
        double totalMonthlyWithInsurance) {
}

record RateStressTestRequest(double loanAmount, int termYears, double monthlyIncome) {
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
    MortgagePaymentResponse monthlyPayment(@RequestBody MortgagePaymentRequest req) {
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
    RateStressTestResponse rateStressTest(@RequestBody RateStressTestRequest req) {
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
        return calculator.amortizationScheduleByYear(loanAmount, termYears);
    }
}
