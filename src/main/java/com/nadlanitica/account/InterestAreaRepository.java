package com.nadlanitica.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestAreaRepository extends JpaRepository<InterestArea, String> {
    List<InterestArea> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<InterestArea> findByIdAndUserId(String id, String userId);
    boolean existsByUserIdAndName(String userId, String name);
}
