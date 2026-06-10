package com.dimetime.repository;

import com.dimetime.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByPoNumber(String poNumber);
    Optional<Invoice> findByGrnNumber(String grnNumber);
    List<Invoice> findBySupplier(String supplier);
    List<Invoice> findByManufacturer(String manufacturer);
    long countByStatus(String status);
}
