package com.nadlanitica.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AccountTokenRepository extends JpaRepository<AccountToken, String> {

    @Modifying
    @Query("delete from AccountToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
