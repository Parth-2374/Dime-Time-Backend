package com.dimetime.repository;

import com.dimetime.entity.UsernameRecoveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsernameRecoveryLogRepository extends JpaRepository<UsernameRecoveryLog, Long> {
}
