package com.nadlanitica.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /** רשימה ולא Optional: ייתכנו כמה חשבונות על אותה תיבה שנפתחו לפני שהחסימה נוספה (V3__Mailbox_key). */
    List<AppUser> findByMailboxKey(String mailboxKey);
}
