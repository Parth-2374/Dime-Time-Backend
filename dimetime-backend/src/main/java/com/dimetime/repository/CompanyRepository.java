package com.dimetime.repository;

import com.dimetime.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    Optional<Company> findByGstNumber(String gstNumber);
    boolean existsByName(String name);
    boolean existsByGstNumber(String gstNumber);
}
