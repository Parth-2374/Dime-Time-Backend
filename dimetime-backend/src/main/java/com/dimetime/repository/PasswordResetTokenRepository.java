package com.dimetime.repository;

import com.dimetime.entity.PasswordResetToken;
import com.dimetime.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    List<PasswordResetToken> findByUserAndVerifiedFalse(User user);
    Optional<PasswordResetToken> findFirstByUserAndVerifiedFalseOrderByCreatedAtDesc(User user);
    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);
}
