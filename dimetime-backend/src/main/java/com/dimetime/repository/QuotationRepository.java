package com.dimetime.repository;

import com.dimetime.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRfqNumber(String rfqNumber);
    List<Quotation> findByManufacturerOrderByCreatedAtDesc(String manufacturer);
    List<Quotation> findByRfqNumberAndManufacturer(String rfqNumber, String manufacturer);
}
