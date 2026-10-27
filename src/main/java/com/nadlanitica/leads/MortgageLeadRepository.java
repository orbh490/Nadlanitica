package com.nadlanitica.leads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MortgageLeadRepository extends JpaRepository<MortgageLead, Long> {
    List<MortgageLead> findAllByOrderByCreatedAtDesc();
}
