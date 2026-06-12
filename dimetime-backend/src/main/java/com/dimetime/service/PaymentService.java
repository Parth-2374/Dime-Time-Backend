package com.dimetime.service;

import com.dimetime.entity.Invoice;
import com.dimetime.entity.Payment;
import com.dimetime.entity.PurchaseOrder;
import com.dimetime.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> getPaymentByNumber(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber);
    }

    public Optional<Payment> getPaymentByInvoiceNumber(String invoiceNumber) {
        return paymentRepository.findByInvoiceNumber(invoiceNumber);
    }
@Transactional
public Payment initiatePayment(String invoiceNumber,
                               String paymentMethod,
                               Double amount,
                               String operator) {

    Optional<Invoice> invOpt =
            invoiceService.getInvoiceByNumber(invoiceNumber);

    if (invOpt.isEmpty()) {
        throw new IllegalArgumentException(
                "Invoice not found: " + invoiceNumber
        );
    }

    Invoice invoice = invOpt.get();

    long currentCount = paymentRepository.count();
    String paymentNumber =
            String.format("PAY-2026-%04d", currentCount + 1);

    Payment payment = new Payment(
            paymentNumber,
            invoiceNumber,
            invoice.getPoNumber(),
            invoice.getGrnNumber(),
            amount,
            paymentMethod,
            "COD".equalsIgnoreCase(paymentMethod)
                    ? "PAYMENT_PENDING"
                    : "PAYMENT_IN_PROGRESS",
            null,
            null
    );

    Payment saved = paymentRepository.save(payment);

    invoiceService.updateStatus(
            invoiceNumber,
            "PAYMENT_PENDING"
    );

    auditLogService.logActivity(
            "Payment initiated: " + paymentNumber,
            operator
    );

    sendNotificationToParties(
            invoice.getPoNumber(),
            "Payment initiated for Invoice " + invoiceNumber
    );

    return saved;
}

    @Transactional
    public Payment completePayment(String paymentNumber, String transactionReference, String operator) {
        Optional<Payment> payOpt = paymentRepository.findByPaymentNumber(paymentNumber);
        if (payOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found: " + paymentNumber);
        }
        Payment payment = payOpt.get();
        if ("PAYMENT_COMPLETED".equals(payment.getPaymentStatus())) {
            return payment;
        }

        payment.setPaymentStatus("PAYMENT_COMPLETED");
        payment.setTransactionReference(transactionReference);
        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Update Invoice status
        invoiceService.updateStatus(payment.getInvoiceNumber(), "PAYMENT_COMPLETED");

        // Close PO & Order Status
        poService.updateStatus(payment.getPoNumber(), "CLOSED", operator);

        // Audit Trail Entries
        auditLogService.logActivity("Payment completed: " + paymentNumber + " - Transaction Ref: " + transactionReference, operator);
        auditLogService.logActivity("SCM Purchase Order " + payment.getPoNumber() + " status updated to CLOSED", operator);

        // Send Notifications
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(payment.getPoNumber());
        if (poOpt.isPresent()) {
            PurchaseOrder po = poOpt.get();
            try {
                String supplier = po.getSupplierUsername() != null ? po.getSupplierUsername() : "supplier";
                String manufacturer = po.getManufacturerUsername() != null ? po.getManufacturerUsername() : "manufacturer";

                notificationService.createNotification(supplier, "Payment received successfully for PO " + po.getPoNumber());
                notificationService.createNotification(manufacturer, "Payment completed successfully for PO " + po.getPoNumber());
                notificationService.createNotification(supplier, "Order closed successfully for PO " + po.getPoNumber());
                notificationService.createNotification(manufacturer, "Order closed successfully for PO " + po.getPoNumber());
            } catch (Exception e) {
                System.err.println("Failed to dispatch payment notifications: " + e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public Payment failPayment(String paymentNumber, String operator) {
        Optional<Payment> payOpt = paymentRepository.findByPaymentNumber(paymentNumber);
        if (payOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found: " + paymentNumber);
        }
        Payment payment = payOpt.get();
        payment.setPaymentStatus("PAYMENT_FAILED");
        Payment saved = paymentRepository.save(payment);

        invoiceService.updateStatus(payment.getInvoiceNumber(), "INVOICE_GENERATED");

        // Audit & Notification
        auditLogService.logActivity("Payment failed: " + paymentNumber, operator);
        sendNotificationToParties(payment.getPoNumber(), "Payment failed for Invoice " + payment.getInvoiceNumber());

        return saved;
    }

    private void sendNotificationToParties(String poNumber, String message) {
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
        if (poOpt.isPresent()) {
            PurchaseOrder po = poOpt.get();
            try {
                String supplier = po.getSupplierUsername() != null ? po.getSupplierUsername() : "supplier";
                String manufacturer = po.getManufacturerUsername() != null ? po.getManufacturerUsername() : "manufacturer";
                notificationService.createNotification(supplier, message);
                notificationService.createNotification(manufacturer, message);
            } catch (Exception e) {
                System.err.println("Failed to send notification: " + e.getMessage());
            }
        }
    }
}
