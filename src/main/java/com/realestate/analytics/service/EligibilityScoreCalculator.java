package com.realestate.analytics.service;

import com.realestate.analytics.model.EligibilityProfile;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ניקוד זכאות במסלול הכללי (לא-דתי) של "מחיר למשתכן", לפי תקנון הגרלת בחירת זכאים
 * של משרד הבינוי והשיכון ומאמר "כל-זכות" על חישוב הניקוד.
 * ponytail: המסלול הדתי משתמש בטבלת ניקוד וסיפי-קבוצות שונים - לא ממומש כאן, נדרש מקור נפרד ומאומת.
 */
@Service
public class EligibilityScoreCalculator {

    private static final int POINTS_PER_CHILD = 10;
    private static final int MAX_CHILDREN_SCORED = 3;
    private static final int MILITARY_OR_NATIONAL_SERVICE_POINTS = 10;
    private static final int NEW_IMMIGRANT_POINTS = 20;
    private static final int DISABLED_COUPLE_EXEMPT_POINTS = 10;
    private static final int MEDICAL_DISABILITY_65_PLUS_POINTS = 15;
    private static final int EARNING_INCAPACITY_75_PLUS_POINTS = 5;
    private static final int LOW_INCOME_HOUSEHOLD_POINTS = 8;
    private static final int POINTS_PER_FAILED_APPLICATION = 3;
    private static final int MAX_FAILED_APPLICATIONS_SCORED = 4;

    private static final int MIN_REGISTRATION_SCORE = 11;

    public record ScoreBracket(String label, int minScore, Integer maxScore, double unitSharePercent) {
    }

    // חלוקת הדירות בין 3 קבוצות הניקוד (אומת מול "כל-זכות" המצטט את נוהל 08/31 מיום 20.10.2019
    // ואת תקנון הגרלת בחירת זכאים של משרד הבינוי והשיכון): 11-54 נק'=25%, 55-64 נק'=50%, 65+=25%.
    private static final ScoreBracket BRACKET_LOW = new ScoreBracket("11-54 נקודות", 11, 54, 25.0);
    private static final ScoreBracket BRACKET_MID = new ScoreBracket("55-64 נקודות", 55, 64, 50.0);
    private static final ScoreBracket BRACKET_HIGH = new ScoreBracket("65+ נקודות", 65, null, 25.0);

    public int calculateScore(EligibilityProfile profile) {
        int score = 0;
        score += Math.min(profile.getNumberOfChildren(), MAX_CHILDREN_SCORED) * POINTS_PER_CHILD;
        if (profile.isCompletedMilitaryOrNationalService()) score += MILITARY_OR_NATIONAL_SERVICE_POINTS;
        if (profile.isNewImmigrant()) score += NEW_IMMIGRANT_POINTS;
        if (profile.isDisabledCoupleExemptFromService()) score += DISABLED_COUPLE_EXEMPT_POINTS;
        if (profile.isMedicalDisability65Plus()) score += MEDICAL_DISABILITY_65_PLUS_POINTS;
        if (profile.isEarningIncapacity75Plus()) score += EARNING_INCAPACITY_75_PLUS_POINTS;
        if (profile.isLowIncomeHousehold()) score += LOW_INCOME_HOUSEHOLD_POINTS;
        score += Math.min(profile.getFailedPriorApplications(), MAX_FAILED_APPLICATIONS_SCORED) * POINTS_PER_FAILED_APPLICATION;
        return score;
    }

    public Optional<ScoreBracket> determineBracket(int score) {
        if (score < MIN_REGISTRATION_SCORE) return Optional.empty();
        if (score <= BRACKET_LOW.maxScore()) return Optional.of(BRACKET_LOW);
        if (score <= BRACKET_MID.maxScore()) return Optional.of(BRACKET_MID);
        return Optional.of(BRACKET_HIGH);
    }
}
