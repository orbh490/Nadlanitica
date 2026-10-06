package com.realestate.analytics.repository;

import com.realestate.analytics.model.MortgageLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MortgageLeadRepository extends JpaRepository<MortgageLead, Long> {
    List<MortgageLead> findAllByOrderByCreatedAtDesc();
}
