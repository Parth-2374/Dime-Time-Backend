package com.dimetime.repository;

import com.dimetime.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    Optional<Payment> findByInvoiceNumber(String invoiceNumber);
    List<Payment> findByPoNumber(String poNumber);
    long countByPaymentStatus(String paymentStatus);
    long countByPaymentMethodAndPaymentStatus(String paymentMethod, String paymentStatus);
}
