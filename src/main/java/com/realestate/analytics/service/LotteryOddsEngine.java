package com.realestate.analytics.service;

import com.realestate.analytics.model.LotteryProject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class LotteryOddsEngine {

    public record CityStats(String city, int projectCount, double avgOddsPercent, double avgPricePerSqm) {
    }

    // עמודות בקובץ הרשמי של משרד השיכון (תוצאות הגרלות מחיר למשתכן, data.gov.il)
    private static final int COL_WINNERS_BNEY_MAKOM = 5;
    private static final int COL_WINNERS = 6;
    private static final int COL_SUBSCRIBERS_BNEY_MAKOM = 12;
    private static final int COL_SUBSCRIBERS = 13;
    private static final int COL_LOTTERY_NATIVE_HOUSING_UNITS = 14;
    private static final int COL_LOTTERY_HOUSING_UNITS = 15;
    private static final int COL_PRICE_PER_METER = 18;
    private static final int COL_PROJECT_NAME = 22;
    private static final int COL_PROJECT_ID = 23;
    private static final int COL_CITY = 25;
    private static final int COL_STATUS = 29;
    private static final String STATUS_RESULTS_PUBLISHED = "פורסמו תוצאות";

    private final List<LotteryProject> projects = loadProjects();

    public List<LotteryProject> allProjects() {
        return projects;
    }

    public Optional<LotteryProject> findProject(String city, String projectName) {
        return projects.stream()
                .filter(p -> p.getCity().equalsIgnoreCase(city) && p.getProjectName().equalsIgnoreCase(projectName))
                .findFirst();
    }

    public double calculateOddsPercent(LotteryProject project, boolean isLocalResident) {
        int poolUnits = isLocalResident ? project.getLocalQuotaUnits() : project.getTotalUnits() - project.getLocalQuotaUnits();
        int poolApplicants = isLocalResident ? project.getLocalApplicants() : project.getTotalApplicants() - project.getLocalApplicants();
        if (poolApplicants <= 0) {
            return poolUnits > 0 ? 100.0 : 0.0;
        }
        return Math.min(100.0, (poolUnits * 100.0) / poolApplicants);
    }

    // שיעור הזכייה בפועל בהגרלה הזו (בדיעבד), אם הנתון פורסם - לצורך השוואה מול האומדן החזוי
    public OptionalDouble actualHistoricalOddsPercent(LotteryProject project, boolean isLocalResident) {
        int poolApplicants = isLocalResident ? project.getLocalApplicants() : project.getTotalApplicants() - project.getLocalApplicants();
        int poolWinners;
        if (isLocalResident) {
            poolWinners = project.getLocalWinners();
        } else if (project.getTotalWinners() >= 0 && project.getLocalWinners() >= 0) {
            poolWinners = project.getTotalWinners() - project.getLocalWinners();
        } else {
            poolWinners = -1;
        }
        if (poolWinners < 0 || poolApplicants <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.min(100.0, (poolWinners * 100.0) / poolApplicants));
    }

    // השוואת ערים: ממוצע סיכוי זכייה וממוצע מחיר למ"ר, לכל עיר עם לפחות minProjects פרויקטים, ממוין לפי סיכוי יורד
    public List<CityStats> compareCities(boolean isLocalResident, int minProjects, int limit) {
        return projects.stream()
                .collect(Collectors.groupingBy(LotteryProject::getCity))
                .entrySet().stream()
                .filter(e -> e.getValue().size() >= minProjects)
                .map(e -> {
                    List<LotteryProject> cityProjects = e.getValue();
                    double avgOdds = cityProjects.stream()
                            .mapToDouble(p -> calculateOddsPercent(p, isLocalResident))
                            .average().orElse(0);
                    double avgPrice = cityProjects.stream()
                            .mapToDouble(LotteryProject::getPricePerSqm)
                            .filter(price -> price > 0)
                            .average().orElse(0);
                    return new CityStats(e.getKey(), cityProjects.size(), avgOdds, avgPrice);
                })
                .sorted(Comparator.comparingDouble(CityStats::avgOddsPercent).reversed())
                .limit(limit)
                .toList();
    }

    private static List<LotteryProject> loadProjects() {
        try (InputStream in = LotteryOddsEngine.class.getResourceAsStream("/data/lottery_results.csv")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines()
                    .skip(1)
                    .map(line -> line.split(";", -1))
                    .filter(f -> f.length > COL_STATUS && STATUS_RESULTS_PUBLISHED.equals(f[COL_STATUS]))
                    .map(LotteryOddsEngine::toProject)
                    .filter(p -> p != null)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static LotteryProject toProject(String[] f) {
        String city = f[COL_CITY].trim();
        String projectName = f[COL_PROJECT_NAME].trim();
        int totalUnits = parseCount(f[COL_LOTTERY_HOUSING_UNITS]);
        if (city.isEmpty() || projectName.isEmpty() || totalUnits <= 0) {
            return null;
        }
        int localQuotaUnits = parseCount(f[COL_LOTTERY_NATIVE_HOUSING_UNITS]);
        int totalApplicants = parseCount(f[COL_SUBSCRIBERS]);
        int localApplicants = parseCount(f[COL_SUBSCRIBERS_BNEY_MAKOM]);
        int totalWinners = parseNullableCount(f[COL_WINNERS]);
        int localWinners = parseNullableCount(f[COL_WINNERS_BNEY_MAKOM]);
        double pricePerSqm = parsePrice(f[COL_PRICE_PER_METER]);
        String projectId = f.length > COL_PROJECT_ID ? f[COL_PROJECT_ID].trim() : "";
        return new LotteryProject(city, projectName, projectId, totalUnits, localQuotaUnits, totalApplicants,
                localApplicants, totalWinners, localWinners, pricePerSqm);
    }

    private static double parsePrice(String raw) {
        String s = raw.trim().replace(",", "");
        if (s.isEmpty() || s.equals("-")) {
            return 0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseCount(String raw) {
        String s = raw.trim().replace(",", "");
        if (s.isEmpty() || s.equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // כמו parseCount, אבל "-" פירושו "לא ידוע" (-1) ולא אפס - כי אפס זכיות הוא נתון שונה מנתון שלא פורסם
    private static int parseNullableCount(String raw) {
        String s = raw.trim().replace(",", "");
        if (s.isEmpty() || s.equals("-")) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
