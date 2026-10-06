package com.realestate.analytics.repository;

import com.realestate.analytics.model.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTokenRepository extends JpaRepository<AccountToken, String> {
}
