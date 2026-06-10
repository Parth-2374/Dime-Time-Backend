package com.dimetime.repository;

import com.dimetime.entity.PlateCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlateCalculationRepository extends JpaRepository<PlateCalculation, Long> {
    List<PlateCalculation> findAllByOrderByCalculatedAtDesc();
}
