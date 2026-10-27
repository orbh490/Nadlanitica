package com.nadlanitica.lottery;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.OptionalDouble;

record ProjectSummary(String city, String projectName, String projectId, String liveStatus) {
}

record LotteryOddsResponse(String city, String projectName, boolean localResident, double winProbabilityPercent,
        Double actualHistoricalWinRatePercent, boolean peripheryGrantEligibleCity, int poolUnits, int poolApplicants) {
}

record LiveStatusResponse(boolean hasLiveData, LotteryLiveService.LiveLotteryProject liveProject) {
}

@RestController
@RequestMapping("/api/lottery")
class LotteryController {

    private final LotteryOddsEngine engine;
    private final PeripheryGrantCities peripheryGrantCities;
    private final LotteryLiveService liveService;

    LotteryController(LotteryOddsEngine engine, PeripheryGrantCities peripheryGrantCities, LotteryLiveService liveService) {
        this.engine = engine;
        this.peripheryGrantCities = peripheryGrantCities;
        this.liveService = liveService;
    }

    /** האם יש כרגע הרשמה פתוחה/עתידית לפרויקט ההיסטורי הזה, חי ממשרד הבינוי והשיכון - מותאם לפי מזהה פרויקט. */
    @GetMapping("/live-status")
    LiveStatusResponse liveStatus(@RequestParam String projectId) {
        return liveService.findForProjectId(projectId)
                .map(p -> new LiveStatusResponse(true, p))
                .orElse(new LiveStatusResponse(false, null));
    }

    @GetMapping("/projects")
    List<ProjectSummary> projects() {
        return engine.allProjects().stream()
                .map(p -> new ProjectSummary(p.getCity(), p.getProjectName(), p.getProjectId(), liveStatusFor(p.getProjectId())))
                .toList();
    }

    /** "OPEN" / "NOT_YET_OPEN" / "NONE" (אין נתון חי תואם, כולל אם כבר נסגר) - לסינון ברשימת החיפוש. */
    private String liveStatusFor(String projectId) {
        return liveService.findForProjectId(projectId)
                .map(p -> p.status().name())
                .orElse("NONE");
    }

    @GetMapping("/odds")
    LotteryOddsResponse odds(@RequestParam String city, @RequestParam String project, @RequestParam boolean localResident) {
        LotteryProject p = engine.findProject(city, project)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "פרויקט לא נמצא: " + city + " / " + project));
        OptionalDouble historical = engine.actualHistoricalOddsPercent(p, localResident);
        LotteryOddsEngine.OddsPool pool = engine.poolFor(p, localResident);
        return new LotteryOddsResponse(p.getCity(), p.getProjectName(), localResident,
                engine.calculateOddsPercent(p, localResident),
                historical.isPresent() ? historical.getAsDouble() : null,
                peripheryGrantCities.isEligible(p.getCity()), pool.units(), pool.applicants());
    }

    @GetMapping("/city-comparison")
    List<LotteryOddsEngine.CityStats> cityComparison(@RequestParam boolean localResident,
            @RequestParam(defaultValue = "3") int minProjects, @RequestParam(defaultValue = "15") int limit) {
        return engine.compareCities(localResident, minProjects, limit);
    }
}
