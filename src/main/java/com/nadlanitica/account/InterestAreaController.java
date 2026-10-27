package com.nadlanitica.account;

import com.nadlanitica.auth.AppUser;
import com.nadlanitica.auth.AuthService;
import com.nadlanitica.auth.BearerToken;
import org.springframework.http.HttpStatus;
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
import java.util.UUID;

/** אזורי עניין של משתמש מחובר - רשימה, הוספה והסרה. רק שמירה; ההתראות עצמן ייבנו בהמשך. */
@RestController
@RequestMapping("/api/interest-areas")
class InterestAreaController {

    record InterestAreaRequest(String name) {
    }

    record InterestAreaResponse(String id, String name, Instant createdAt) {
    }

    private final InterestAreaRepository repository;
    private final AuthService authService;

    InterestAreaController(InterestAreaRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping
    List<InterestAreaResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(BearerToken.from(authorization));
        return repository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(a -> new InterestAreaResponse(a.getId(), a.getName(), a.getCreatedAt()))
                .toList();
    }

    @PostMapping
    InterestAreaResponse add(@RequestBody InterestAreaRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(BearerToken.from(authorization));
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty() || name.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "יש להזין שם אזור עד 60 תווים");
        }
        if (repository.existsByUserIdAndName(user.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "האזור הזה כבר ברשימה");
        }
        InterestArea area = repository.save(new InterestArea(UUID.randomUUID().toString(), user.getId(), name));
        return new InterestAreaResponse(area.getId(), area.getName(), area.getCreatedAt());
    }

    @DeleteMapping("/{id}")
    void remove(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(BearerToken.from(authorization));
        InterestArea area = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "האזור לא נמצא"));
        repository.delete(area);
    }
}
