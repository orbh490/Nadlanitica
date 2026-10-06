package com.realestate.analytics.model;

// קלט לחישוב ניקוד זכאות במסלול הכללי של "מחיר למשתכן" (תקנון משרד הבינוי והשיכון)
public class EligibilityProfile {
    private int numberOfChildren;
    private boolean completedMilitaryOrNationalService;
    private boolean newImmigrant;
    private boolean disabledCoupleExemptFromService;
    private boolean medicalDisability65Plus;
    private boolean earningIncapacity75Plus;
    private boolean lowIncomeHousehold;
    private int failedPriorApplications;
    private boolean reservistEligible;

    public EligibilityProfile(int numberOfChildren, boolean completedMilitaryOrNationalService, boolean newImmigrant,
            boolean disabledCoupleExemptFromService, boolean medicalDisability65Plus, boolean earningIncapacity75Plus,
            boolean lowIncomeHousehold, int failedPriorApplications, boolean reservistEligible) {
        this.numberOfChildren = numberOfChildren;
        this.completedMilitaryOrNationalService = completedMilitaryOrNationalService;
        this.newImmigrant = newImmigrant;
        this.disabledCoupleExemptFromService = disabledCoupleExemptFromService;
        this.medicalDisability65Plus = medicalDisability65Plus;
        this.earningIncapacity75Plus = earningIncapacity75Plus;
        this.lowIncomeHousehold = lowIncomeHousehold;
        this.failedPriorApplications = failedPriorApplications;
        this.reservistEligible = reservistEligible;
    }

    public int getNumberOfChildren() { return numberOfChildren; }
    public boolean isCompletedMilitaryOrNationalService() { return completedMilitaryOrNationalService; }
    public boolean isNewImmigrant() { return newImmigrant; }
    public boolean isDisabledCoupleExemptFromService() { return disabledCoupleExemptFromService; }
    public boolean isMedicalDisability65Plus() { return medicalDisability65Plus; }
    public boolean isEarningIncapacity75Plus() { return earningIncapacity75Plus; }
    public boolean isLowIncomeHousehold() { return lowIncomeHousehold; }
    public int getFailedPriorApplications() { return failedPriorApplications; }
    public boolean isReservistEligible() { return reservistEligible; }
}
