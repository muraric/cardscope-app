package com.shomuran.cardscope.repository;

import com.shomuran.cardscope.model.PasswordResetToken;
import com.shomuran.cardscope.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(UserProfile user);
}
