package com.dimetime.service;

import com.dimetime.dto.DashboardStatsDto;
import com.dimetime.entity.*;
import com.dimetime.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private MaterialUploadRepository materialUploadRepository;

    @Autowired
    private MtcDocumentRepository mtcDocumentRepository;

    @Autowired
    private GrnRepository grnRepository;

    @Autowired
    private VerificationResultRepository verificationRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private PlateCalculationRepository plateCalculationRepository;

    @Autowired
    private RfqRepository rfqRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public DashboardStatsDto getDashboardStats() {
        long totalUploads = materialUploadRepository.count();
        long approvedGrns = grnRepository.countByStatus("APPROVED");
        long rejectedCount = verificationRepository.findByStatus("REJECTED").size();
        long totalPlateCalculations = plateCalculationRepository.count();

        List<PurchaseOrder> allPos = poRepository.findAll();
        List<VerificationResult> allVerifications = verificationRepository.findAll();
        
        Set<String> verifiedPoNumbers = new HashSet<>();
        for (VerificationResult vr : allVerifications) {
            if ("APPROVED".equalsIgnoreCase(vr.getStatus())) {
                verifiedPoNumbers.add(vr.getPoNumber());
            }
        }

        long pendingVerification = 0;
        for (PurchaseOrder po : allPos) {
            if (!verifiedPoNumbers.contains(po.getPoNumber())) {
                pendingVerification++;
            }
        }

        pendingVerification = Math.max(0, pendingVerification);

        return new DashboardStatsDto(
                totalUploads,
                approvedGrns,
                rejectedCount,
                pendingVerification,
                totalPlateCalculations
        );
    }

    public Map<String, Object> getSupplierStats(String supplierUsername) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Rfq> myRfqs = rfqRepository.findByCreatedByOrderByCreatedAtDesc(supplierUsername);
        stats.put("totalRfqs", myRfqs.size());

        long quotesCount = 0;
        for (Rfq rfq : myRfqs) {
            quotesCount += quotationRepository.findByRfqNumber(rfq.getRfqNumber()).size();
        }
        stats.put("quotationsReceived", quotesCount);

        Optional<User> userOpt = userRepository.findByUsername(supplierUsername);
        List<PurchaseOrder> myPos;
        if (userOpt.isPresent()) {
            myPos = poRepository.findBySupplierId(userOpt.get().getId());
        } else {
            myPos = poRepository.findBySupplierOrderByCreatedAtDesc(supplierUsername);
        }
        long activePOs = 0;
        long ordersInTransit = 0;
        long pendingDeliveries = 0;
        
        for (PurchaseOrder po : myPos) {
            String status = po.getStatus();
            if ("PENDING".equalsIgnoreCase(status) || "PRODUCTION_START".equalsIgnoreCase(status) || "COMPLETED_PRODUCTION".equalsIgnoreCase(status)) {
                activePOs++;
                pendingDeliveries++;
            } else if ("DISPATCHED".equalsIgnoreCase(status)) {
                ordersInTransit++;
                pendingDeliveries++;
            }
        }
        
        stats.put("activePurchaseOrders", activePOs);
        stats.put("ordersInTransit", ordersInTransit);
        stats.put("pendingDeliveries", pendingDeliveries);
        
        List<Grn> myGrns = grnRepository.findAll(); // Filter by supplier name or username
        long approvedGrns = myGrns.stream().filter(g -> "APPROVED".equalsIgnoreCase(g.getStatus()) && supplierUsername.equalsIgnoreCase(g.getGeneratedBy())).count();
        stats.put("approvedGrns", approvedGrns);

        List<VerificationResult> allVerifs = verificationRepository.findAll();
        long totalV = allVerifs.size();
        long approvedV = allVerifs.stream().filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus())).count();
        double successRate = totalV > 0 ? ((double) approvedV / totalV) * 100.0 : 100.0;
        stats.put("reconciliationSuccessRate", Math.round(successRate * 100.0) / 100.0);

        // Calculate MTC stats
        Set<String> poNumbers = new HashSet<>();
        long pendingMtcReviews = 0;
        for (PurchaseOrder po : myPos) {
            poNumbers.add(po.getPoNumber());
            if ("SUPPLIER_REVIEW".equalsIgnoreCase(po.getStatus())) {
                pendingMtcReviews++;
            }
        }
        
        long approvedMtcCertificates = 0;
        long rejectedMtcCertificates = 0;
        for (String poNum : poNumbers) {
            List<MtcDocument> docs = mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNum);
            for (MtcDocument doc : docs) {
                if ("APPROVED".equalsIgnoreCase(doc.getStatus())) {
                    approvedMtcCertificates++;
                } else if ("REJECTED".equalsIgnoreCase(doc.getStatus())) {
                    rejectedMtcCertificates++;
                }
            }
        }
        
        stats.put("pendingMtcReviews", pendingMtcReviews);
        stats.put("approvedMtcCertificates", approvedMtcCertificates);
        stats.put("rejectedMtcCertificates", rejectedMtcCertificates);

        long pendingPayments = 0;
        long completedPayments = 0;
        double totalAmountReceived = 0.0;
        
        List<Payment> allPayments = paymentRepository.findAll();
        for (Payment payment : allPayments) {
            if (poNumbers.contains(payment.getPoNumber())) {
                if ("PAYMENT_PENDING".equals(payment.getPaymentStatus())) {
                    pendingPayments++;
                } else if ("PAYMENT_COMPLETED".equals(payment.getPaymentStatus())) {
                    completedPayments++;
                    totalAmountReceived += payment.getAmount();
                }
            }
        }
        stats.put("pendingPayments", pendingPayments);
        stats.put("completedPayments", completedPayments);
        stats.put("totalAmountReceived", totalAmountReceived);

        return stats;
    }

    private boolean hasRejectedMtc(String poNumber) {
        List<MtcDocument> docs = mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNumber);
        return !docs.isEmpty() && "REJECTED".equalsIgnoreCase(docs.get(0).getStatus());
    }

    public Map<String, Object> getManufacturerStats(String manufacturerUsername) {
        Map<String, Object> stats = new HashMap<>();

        long rfqsReceived = rfqRepository.findAll().stream()
                .filter(r -> "CREATED".equalsIgnoreCase(r.getStatus()) || "QUOTED".equalsIgnoreCase(r.getStatus()))
                .count();
        stats.put("rfqsReceived", rfqsReceived);

        long quotationsSubmitted = quotationRepository.findByManufacturerOrderByCreatedAtDesc(manufacturerUsername).size();
        stats.put("quotationsSubmitted", quotationsSubmitted);

        Optional<User> userOpt = userRepository.findByUsername(manufacturerUsername);
        List<PurchaseOrder> myPOs;
        if (userOpt.isPresent()) {
            myPOs = poRepository.findByManufacturerId(userOpt.get().getId());
        } else {
            myPOs = poRepository.findByManufacturerOrderByCreatedAtDesc(manufacturerUsername);
        }
        stats.put("purchaseOrdersReceived", myPOs.size());

        long pendingProduction = myPOs.stream().filter(p -> 
            "PRODUCTION_START".equalsIgnoreCase(p.getStatus()) || 
            "PRODUCTION_STARTED".equalsIgnoreCase(p.getStatus()) || 
            "REJECTED".equalsIgnoreCase(p.getStatus())
        ).count();
        stats.put("pendingProductionOrders", pendingProduction);

        long pendingMtc = myPOs.stream().filter(p -> 
            ("PRODUCTION_START".equalsIgnoreCase(p.getStatus()) || 
             "PRODUCTION_STARTED".equalsIgnoreCase(p.getStatus()) || 
             "REJECTED".equalsIgnoreCase(p.getStatus())) && 
            (p.getMtcFileName() == null || hasRejectedMtc(p.getPoNumber()))
        ).count();
        stats.put("pendingMtcGeneration", pendingMtc);

        long dispatchReady = myPOs.stream().filter(p -> "COMPLETED_PRODUCTION".equalsIgnoreCase(p.getStatus())).count();
        stats.put("dispatchReadyOrders", dispatchReady);

        long completedDeliveries = myPOs.stream().filter(p -> "DELIVERED".equalsIgnoreCase(p.getStatus())).count();
        stats.put("completedDeliveries", completedDeliveries);

        // Calculate MTC stats
        Set<String> poNumbersMan = new HashSet<>();
        long pendingSupplierReviews = 0;
        long readyForDispatch = 0;
        for (PurchaseOrder po : myPOs) {
            poNumbersMan.add(po.getPoNumber());
            if ("SUPPLIER_REVIEW".equalsIgnoreCase(po.getStatus())) {
                pendingSupplierReviews++;
            }
            if ("APPROVED_FOR_DISPATCH".equalsIgnoreCase(po.getStatus())) {
                readyForDispatch++;
            }
        }
        
        long approvedCertificates = 0;
        long rejectedCertificates = 0;
        for (String poNum : poNumbersMan) {
            List<MtcDocument> docs = mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNum);
            for (MtcDocument doc : docs) {
                if ("APPROVED".equalsIgnoreCase(doc.getStatus())) {
                    approvedCertificates++;
                } else if ("REJECTED".equalsIgnoreCase(doc.getStatus())) {
                    rejectedCertificates++;
                }
            }
        }
        
        stats.put("pendingSupplierReviews", pendingSupplierReviews);
        stats.put("approvedCertificates", approvedCertificates);
        stats.put("rejectedCertificates", rejectedCertificates);
        stats.put("readyForDispatch", readyForDispatch);

        long pendingPaymentsMan = 0;
        double paidAmount = 0.0;
        double outstandingAmount = 0.0;
        
        List<Payment> allPaymentsMan = paymentRepository.findAll();
        for (Payment payment : allPaymentsMan) {
            if (poNumbersMan.contains(payment.getPoNumber())) {
                if ("PAYMENT_PENDING".equals(payment.getPaymentStatus())) {
                    pendingPaymentsMan++;
                } else if ("PAYMENT_COMPLETED".equals(payment.getPaymentStatus())) {
                    paidAmount += payment.getAmount();
                }
            }
        }
        
        List<Invoice> allInvoicesMan = invoiceRepository.findAll();
        for (Invoice invoice : allInvoicesMan) {
            if (poNumbersMan.contains(invoice.getPoNumber())) {
                if ("INVOICE_GENERATED".equals(invoice.getStatus()) || "PAYMENT_PENDING".equals(invoice.getStatus())) {
                    outstandingAmount += invoice.getTotalAmount();
                }
            }
        }
        
        stats.put("pendingPayments", pendingPaymentsMan);
        stats.put("paidAmount", paidAmount);
        stats.put("outstandingAmount", outstandingAmount);

        return stats;
    }

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalSuppliers = userRepository.findAll().stream().filter(u -> "SUPPLIER".equalsIgnoreCase(u.getRole())).count();
        long totalManufacturers = userRepository.findAll().stream().filter(u -> "MANUFACTURER".equalsIgnoreCase(u.getRole())).count();

        stats.put("totalUsers", totalUsers);
        stats.put("totalSuppliers", totalSuppliers);
        stats.put("totalManufacturers", totalManufacturers);

        stats.put("totalRfqs", rfqRepository.count());
        stats.put("totalQuotations", quotationRepository.count());
        
        List<PurchaseOrder> allPos = poRepository.findAll();
        stats.put("totalPurchaseOrders", (long) allPos.size());

        long activeProduction = allPos.stream().filter(po -> "PRODUCTION_START".equalsIgnoreCase(po.getStatus()) || "PRODUCTION_STARTED".equalsIgnoreCase(po.getStatus()) || "PRODUCTION".equalsIgnoreCase(po.getStatus()) || "COMPLETED_PRODUCTION".equalsIgnoreCase(po.getStatus())).count();
        stats.put("activeProductionOrders", activeProduction);

        List<MtcDocument> allMtcs = mtcDocumentRepository.findAll();
        long pendingMtc = allMtcs.stream().filter(doc -> "SUPPLIER_REVIEW".equalsIgnoreCase(doc.getStatus()) || "PENDING".equalsIgnoreCase(doc.getStatus())).count();
        long approvedMtc = allMtcs.stream().filter(doc -> "APPROVED".equalsIgnoreCase(doc.getStatus())).count();
        long rejectedMtc = allMtcs.stream().filter(doc -> "REJECTED".equalsIgnoreCase(doc.getStatus())).count();

        stats.put("pendingMtcReviews", pendingMtc);
        stats.put("approvedMtcCertificates", approvedMtc);
        stats.put("rejectedMtcCertificates", rejectedMtc);

        long inTransit = allPos.stream().filter(po -> "DISPATCHED".equalsIgnoreCase(po.getStatus()) || "IN_TRANSIT".equalsIgnoreCase(po.getStatus())).count();
        stats.put("materialsInTransit", inTransit);

        long delivered = allPos.stream().filter(po -> "DELIVERED".equalsIgnoreCase(po.getStatus())).count();
        stats.put("deliveredOrders", delivered);

        // Pending GRN: PO status is DELIVERED
        stats.put("pendingGrn", delivered);

        long totalGrns = grnRepository.count();
        stats.put("generatedGrn", totalGrns);

        long reconciled = allPos.stream().filter(po -> "RECONCILED".equalsIgnoreCase(po.getStatus())).count();
        stats.put("reconciledOrders", reconciled);

        long completed = allPos.stream().filter(po -> "COMPLETED".equalsIgnoreCase(po.getStatus()) || "CLOSED".equalsIgnoreCase(po.getStatus())).count();
        stats.put("completedOrders", completed);

        long totalLogs = auditLogRepository.count();
        stats.put("totalAuditLogs", totalLogs);

        List<VerificationResult> allVerifs = verificationRepository.findAll();
        double avgAccuracy = allVerifs.stream().mapToDouble(VerificationResult::getMatchPercentage).average().orElse(100.0);
        stats.put("aiValidationAccuracy", Math.round(avgAccuracy * 100.0) / 100.0);

        stats.put("totalPayments", paymentRepository.count());
        stats.put("successfulPayments", paymentRepository.countByPaymentStatus("PAYMENT_COMPLETED"));
        stats.put("failedPayments", paymentRepository.countByPaymentStatus("PAYMENT_FAILED"));
        stats.put("pendingCodPayments", paymentRepository.countByPaymentMethodAndPaymentStatus("COD", "PAYMENT_PENDING"));

        return stats;
    }
}
