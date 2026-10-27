package com.nadlanitica.mortgage;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MortgagePaymentCalculator {

    public record YearlyAmortization(int year, double principalPaid, double interestPaid, double remainingBalance) {
    }

    // הוראת ניהול בנקאי תקין 329, סעיף 5: מגבלת שיעור החזר מהכנסה
    private static final double MAX_DTI_PERCENT = 50.0;

    private final MortgageInterestRates rates;

    public MortgagePaymentCalculator(MortgageInterestRates rates) {
        this.rates = rates;
    }

    public double monthlyPayment(double loanAmount, int termYears) {
        return monthlyPaymentForRate(loanAmount, termYears, rates.annualRatePercentForTerm(termYears));
    }

    // נוסחת האמורטיזציה הסטנדרטית עם ריבית נתונה - משמש גם למסלולי משכנתא אחרים (קבועה/פריים) שאינם צמודי מדד
    public double monthlyPaymentForRate(double loanAmount, int termYears, double annualRatePercent) {
        double monthlyRate = annualRatePercent / 100 / 12;
        int numPayments = termYears * 12;
        if (monthlyRate == 0) {
            return loanAmount / numPayments;
        }
        double factor = Math.pow(1 + monthlyRate, numPayments);
        return loanAmount * monthlyRate * factor / (factor - 1);
    }

    public double annualRatePercentForTerm(int termYears) {
        return rates.annualRatePercentForTerm(termYears);
    }

    public double dtiPercent(double monthlyPayment, double monthlyIncome) {
        return monthlyIncome > 0 ? monthlyPayment / monthlyIncome * 100 : 0;
    }

    public boolean withinLegalDtiLimit(double monthlyPayment, double monthlyIncome) {
        return monthlyIncome > 0 && dtiPercent(monthlyPayment, monthlyIncome) <= MAX_DTI_PERCENT;
    }

    // פילוח קרן/ריבית לפי שנה, לצורך תצוגת "לאן הולך התשלום החודשי" לאורך חיי ההלוואה
    public List<YearlyAmortization> amortizationScheduleByYear(double loanAmount, int termYears) {
        double monthly = monthlyPayment(loanAmount, termYears);
        double monthlyRate = rates.annualRatePercentForTerm(termYears) / 100 / 12;
        double balance = loanAmount;
        List<YearlyAmortization> schedule = new ArrayList<>();
        for (int year = 1; year <= termYears; year++) {
            double yearPrincipal = 0;
            double yearInterest = 0;
            for (int month = 0; month < 12 && balance > 0; month++) {
                double interestPortion = balance * monthlyRate;
                double principalPortion = Math.min(balance, monthly - interestPortion);
                balance -= principalPortion;
                yearPrincipal += principalPortion;
                yearInterest += interestPortion;
            }
            schedule.add(new YearlyAmortization(year, yearPrincipal, yearInterest, Math.max(0, balance)));
        }
        return schedule;
    }
}
