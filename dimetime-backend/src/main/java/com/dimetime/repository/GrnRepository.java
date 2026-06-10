package com.dimetime.repository;

import com.dimetime.entity.Grn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrnRepository extends JpaRepository<Grn, Long> {
    Optional<Grn> findByGrnNumber(String grnNumber);
    List<Grn> findByPoNumber(String poNumber);
    long countByStatus(String status);
}
