package com.nadlanitica.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    void deleteAllByUserId(String userId);
}
