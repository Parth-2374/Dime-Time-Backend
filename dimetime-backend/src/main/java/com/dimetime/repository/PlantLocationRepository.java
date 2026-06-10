package com.dimetime.repository;

import com.dimetime.entity.Company;
import com.dimetime.entity.PlantLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlantLocationRepository extends JpaRepository<PlantLocation, Long> {
    List<PlantLocation> findByCompany(Company company);
    List<PlantLocation> findByCompanyId(Long companyId);
}
