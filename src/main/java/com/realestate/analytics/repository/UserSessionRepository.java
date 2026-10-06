package com.realestate.analytics.repository;

import com.realestate.analytics.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    void deleteAllByUserId(String userId);
}
