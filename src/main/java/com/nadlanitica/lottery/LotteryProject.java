package com.nadlanitica.lottery;

public class LotteryProject {
    private final String city;
    private final String projectName;
    private final String projectId;      // מזהה הפרויקט של משרד השיכון - אותו מזהה בדיוק מופיע ב-API החי (ProjectNumber), ומאפשר התאמה מדויקת בלי להסתמך על שם הפרויקט
    private final int totalUnits;
    private final int localQuotaUnits;   // יחידות שמורות לבני מקום
    private final int totalApplicants;
    private final int localApplicants;   // מתוך הנרשמים, כמה הם בני מקום
    private final int totalWinners;      // -1 אם לא ידוע/לא רלוונטי
    private final int localWinners;      // מתוך הזוכים, כמה הם בני מקום; -1 אם לא ידוע
    private final double pricePerSqm;    // מחיר למ"ר; 0 אם לא ידוע

    public LotteryProject(String city, String projectName, String projectId, int totalUnits, int localQuotaUnits,
            int totalApplicants, int localApplicants, int totalWinners, int localWinners, double pricePerSqm) {
        this.city = city;
        this.projectName = projectName;
        this.projectId = projectId;
        this.totalUnits = totalUnits;
        this.localQuotaUnits = localQuotaUnits;
        this.totalApplicants = totalApplicants;
        this.localApplicants = localApplicants;
        this.totalWinners = totalWinners;
        this.localWinners = localWinners;
        this.pricePerSqm = pricePerSqm;
    }

    public String getCity() { return city; }
    public String getProjectName() { return projectName; }
    public String getProjectId() { return projectId; }
    public int getTotalUnits() { return totalUnits; }
    public int getLocalQuotaUnits() { return localQuotaUnits; }
    public int getTotalApplicants() { return totalApplicants; }
    public int getLocalApplicants() { return localApplicants; }
    public int getTotalWinners() { return totalWinners; }
    public int getLocalWinners() { return localWinners; }
    public double getPricePerSqm() { return pricePerSqm; }
}
