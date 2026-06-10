package com.dimetime.repository;

import com.dimetime.entity.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Long> {
    Optional<Rfq> findByRfqNumber(String rfqNumber);
    List<Rfq> findByCreatedByOrderByCreatedAtDesc(String username);
    List<Rfq> findByStatusOrderByCreatedAtDesc(String status);
    List<Rfq> findAllByOrderByCreatedAtDesc();
    List<Rfq> findByRfqNumberInOrderByCreatedAtDesc(List<String> rfqNumbers);
}

