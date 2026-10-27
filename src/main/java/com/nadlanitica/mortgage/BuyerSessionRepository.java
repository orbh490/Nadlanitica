package com.nadlanitica.mortgage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuyerSessionRepository extends JpaRepository<BuyerSession, String> {
    List<BuyerSession> findByUserIdOrderByCreatedAtDesc(String userId);
    void deleteAllByUserId(String userId);
}
