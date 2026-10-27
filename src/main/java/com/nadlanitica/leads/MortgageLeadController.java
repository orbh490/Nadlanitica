package com.nadlanitica.leads;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

record LeadRequest(String fullName, String phone, String email, String message) {
}

@RestController
@RequestMapping("/api/leads")
class MortgageLeadController {

    private static final int MAX_FIELD_LENGTH = 300;

    private final MortgageLeadRepository repository;
    private final LeadRateLimiter rateLimiter;

    MortgageLeadController(MortgageLeadRepository repository, LeadRateLimiter rateLimiter) {
        this.repository = repository;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    void submit(@RequestBody LeadRequest req, HttpServletRequest request) {
        rateLimiter.checkAndRecord(request.getRemoteAddr());
        if (isBlank(req.fullName()) || isBlank(req.phone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "נא למלא שם וטלפון");
        }
        if (tooLong(req.fullName()) || tooLong(req.phone()) || tooLong(req.email()) || tooLong(req.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "אחד השדות ארוך מדי");
        }
        repository.save(new MortgageLead(req.fullName(), req.phone(), req.email(), req.message()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean tooLong(String value) {
        return value != null && value.length() > MAX_FIELD_LENGTH;
    }
}
