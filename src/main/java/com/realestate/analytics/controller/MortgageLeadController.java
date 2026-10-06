package com.realestate.analytics.controller;

import com.realestate.analytics.model.MortgageLead;
import com.realestate.analytics.repository.MortgageLeadRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record LeadRequest(String fullName, String phone, String email, String message) {
}

@RestController
@RequestMapping("/api/leads")
class MortgageLeadController {

    private final MortgageLeadRepository repository;

    MortgageLeadController(MortgageLeadRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    void submit(@RequestBody LeadRequest req) {
        repository.save(new MortgageLead(req.fullName(), req.phone(), req.email(), req.message()));
    }
}
