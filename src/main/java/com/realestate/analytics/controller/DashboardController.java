package com.realestate.analytics.controller;

import com.realestate.analytics.model.ApartmentCategory;
import com.realestate.analytics.model.AppUser;
import com.realestate.analytics.model.BuyerSession;
import com.realestate.analytics.repository.BuyerSessionRepository;
import com.realestate.analytics.service.AuthService;
import com.realestate.analytics.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

record DashboardRequest(double availableEquity, double monthlyIncome, ApartmentCategory category,
        double marketValue, double purchasePrice, boolean subsidized, boolean usedBroker, int termYears) {
}

record DashboardResponse(String id, DashboardService.DashboardSummary summary) {
}

record SavedSessionSummary(String id, ApartmentCategory category, double purchasePrice, Instant createdAt) {
}

/**
 * שמירה בשרת קורית רק למשתמשים מחוברים (Authorization: Bearer <token>) - ראו "האזור האישי שלי".
 * משתמש אנונימי מקבל את החישוב חזרה (id=null) בלי שנשמר בצד השרת בכלל; ה-UI שומר עבורו רק ב-sessionStorage
 * של הדפדפן, כך שהמידע נעלם לבד כשסוגרים את הטאב/הדפדפן - לא נשאר בשרת אחרי שהמשתמש עוזב.
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController {

    private final BuyerSessionRepository repository;
    private final DashboardService dashboardService;
    private final AuthService authService;

    DashboardController(BuyerSessionRepository repository, DashboardService dashboardService, AuthService authService) {
        this.repository = repository;
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @PostMapping
    DashboardResponse save(@RequestBody DashboardRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<AppUser> user = authService.currentUser(AuthController.bearerToken(authorization));

        BuyerSession session = new BuyerSession(UUID.randomUUID().toString(), user.map(AppUser::getId).orElse(null),
                req.availableEquity(), req.monthlyIncome(), req.category(), req.marketValue(), req.purchasePrice(),
                req.subsidized(), req.usedBroker(), req.termYears());

        DashboardService.DashboardSummary summary = dashboardService.buildSummary(session);
        if (user.isEmpty()) {
            return new DashboardResponse(null, summary);
        }
        repository.save(session);
        return new DashboardResponse(session.getId(), summary);
    }

    @GetMapping("/{id}")
    DashboardResponse get(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(AuthController.bearerToken(authorization));
        BuyerSession session = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "לא נמצא חישוב שמור עם המזהה הזה"));
        if (!user.getId().equals(session.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "לא נמצא חישוב שמור עם המזהה הזה");
        }
        return new DashboardResponse(session.getId(), dashboardService.buildSummary(session));
    }

    @GetMapping("/mine")
    List<SavedSessionSummary> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(AuthController.bearerToken(authorization));
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(s -> new SavedSessionSummary(s.getId(), s.getCategory(), s.getPurchasePrice(), s.getCreatedAt()))
                .toList();
    }

    /** מוחק את כל החישובים השמורים של המשתמש המחובר. */
    @DeleteMapping("/mine")
    @Transactional
    void clearMine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(AuthController.bearerToken(authorization));
        repository.deleteAllByUserId(user.getId());
    }
}
