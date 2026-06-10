package com.dimetime.repository;

import com.dimetime.entity.RfqAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RfqAssignmentRepository extends JpaRepository<RfqAssignment, Long> {
    List<RfqAssignment> findByManufacturer(String manufacturer);
    List<RfqAssignment> findByRfqNumber(String rfqNumber);
    Optional<RfqAssignment> findByRfqNumberAndManufacturer(String rfqNumber, String manufacturer);
}
