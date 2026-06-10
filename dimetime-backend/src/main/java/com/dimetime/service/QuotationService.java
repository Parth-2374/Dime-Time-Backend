package com.dimetime.service;

import com.dimetime.entity.*;
import com.dimetime.repository.QuotationRepository;
import com.dimetime.repository.RfqAssignmentRepository;
import com.dimetime.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class QuotationService {

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private RfqAssignmentRepository rfqAssignmentRepository;

    @Autowired
    private RfqService rfqService;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private static final Logger log = LoggerFactory.getLogger(QuotationService.class);

    @Transactional
    public Quotation submitQuotation(Quotation quotation) {
        quotation.setStatus("SUBMITTED");
        
        // Calculate total price if not provided
        if (quotation.getPrice() <= 0 && quotation.getUnitPrice() != null) {
            // Get RFQ quantity
            Optional<Rfq> rfqOpt = rfqService.getRfqByNumber(quotation.getRfqNumber());
            if (rfqOpt.isPresent() && rfqOpt.get().getQuantityValue() != null) {
                quotation.setPrice(quotation.getUnitPrice() * rfqOpt.get().getQuantityValue());
            } else {
                quotation.setPrice(quotation.getUnitPrice());
            }
        }
        
        Quotation saved = quotationRepository.save(quotation);

        // Update RfqAssignment status to QUOTED
        Optional<RfqAssignment> assignmentOpt = rfqAssignmentRepository.findByRfqNumberAndManufacturer(
                quotation.getRfqNumber(), quotation.getManufacturer());
        if (assignmentOpt.isPresent()) {
            RfqAssignment assignment = assignmentOpt.get();
            assignment.setStatus("QUOTED");
            rfqAssignmentRepository.save(assignment);
        }

        // Update RFQ status to QUOTED
        Optional<Rfq> rfqOpt = rfqService.getRfqByNumber(quotation.getRfqNumber());
        if (rfqOpt.isPresent()) {
            String currentStatus = rfqOpt.get().getStatus();
            if ("CREATED".equalsIgnoreCase(currentStatus) || "BROADCASTED".equalsIgnoreCase(currentStatus)) {
                rfqService.updateStatus(quotation.getRfqNumber(), "QUOTED", quotation.getManufacturer());
            }
        }

        // Notify Supplier
        if (rfqOpt.isPresent()) {
            notificationService.createNotification(
                    rfqOpt.get().getCreatedBy(),
                    "New Quotation submitted for RFQ: " + quotation.getRfqNumber() + " by " + quotation.getManufacturer()
            );
        }

        auditLogService.logActivity("Submitted Quote for RFQ: " + quotation.getRfqNumber() + " (Price: $" + quotation.getPrice() + ")", quotation.getManufacturer());
        return saved;
    }

    public List<Quotation> getQuotationsForRfq(String rfqNumber) {
        return quotationRepository.findByRfqNumber(rfqNumber);
    }

    public List<Quotation> getQuotationsByManufacturer(String username) {
        return quotationRepository.findByManufacturerOrderByCreatedAtDesc(username);
    }

    @Transactional
    public Quotation selectQuotation(Long id, String operator) {
        log.info("Starting quotation approval workflow. Quotation ID: {}, Operator username: {}", id, operator);

        // 1. Validate Operator exists
        Optional<User> operatorOpt = userRepository.findByUsername(operator);
        if (operatorOpt.isEmpty()) {
            log.warn("Validation failed: Operator '{}' not found in database", operator);
            throw new IllegalArgumentException("Operator not found");
        }
        log.info("Operator lookup result: Operator exists (role: {})", operatorOpt.get().getRole());

        // 2. Validate Quotation exists
        Optional<Quotation> quoteOpt = quotationRepository.findById(id);
        if (quoteOpt.isEmpty()) {
            log.warn("Validation failed: Quotation ID {} not found in database", id);
            throw new IllegalArgumentException("Quotation not found");
        }
        Quotation selectedQuote = quoteOpt.get();
        log.info("Quotation lookup result: Quotation ID {} exists. RFQ Number: {}, Manufacturer: {}, Offered Price: {}", 
                 selectedQuote.getId(), selectedQuote.getRfqNumber(), selectedQuote.getManufacturer(), selectedQuote.getPrice());

        // 3. Validate Selected Manufacturer exists
        Optional<User> manufacturerOpt = userRepository.findByUsername(selectedQuote.getManufacturer());
        if (manufacturerOpt.isEmpty()) {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if ("MANUFACTURER".equalsIgnoreCase(u.getRole()) && 
                    (selectedQuote.getManufacturer().equalsIgnoreCase(u.getCompanyName()) || 
                     (u.getCompany() != null && selectedQuote.getManufacturer().equalsIgnoreCase(u.getCompany().getName())))) {
                    manufacturerOpt = Optional.of(u);
                    break;
                }
            }
        }
        if (manufacturerOpt.isEmpty() || !"MANUFACTURER".equalsIgnoreCase(manufacturerOpt.get().getRole())) {
            log.warn("Validation failed: Manufacturer '{}' not found or has incorrect role", selectedQuote.getManufacturer());
            throw new IllegalArgumentException("Manufacturer not found");
        }
        log.info("Manufacturer lookup result: Manufacturer '{}' exists (company: {})", 
                 manufacturerOpt.get().getUsername(), manufacturerOpt.get().getCompanyName());

        // 4. Validate RFQ exists
        Optional<Rfq> rfqOpt = rfqService.getRfqByNumber(selectedQuote.getRfqNumber());
        if (rfqOpt.isEmpty()) {
            log.warn("Validation failed: RFQ '{}' not found in database", selectedQuote.getRfqNumber());
            throw new IllegalArgumentException("RFQ not found");
        }
        Rfq rfq = rfqOpt.get();
        log.info("RFQ lookup result: RFQ Number '{}' exists. Created by: {}, Status: {}", 
                 rfq.getRfqNumber(), rfq.getCreatedBy(), rfq.getStatus());

        // 5. Validate Required Fields & Apply Fallbacks
        String supplier = rfq.getCreatedBy();
        if (supplier == null || supplier.trim().isEmpty()) {
            log.warn("Validation failed: Required field supplier is null or empty");
            throw new IllegalArgumentException("Required field supplier is null");
        }

        String material = rfq.getMaterialName() != null ? rfq.getMaterialName() : rfq.getMaterial();
        if (material == null || material.trim().isEmpty()) {
            log.warn("Validation failed: Required field material is null or empty (both materialName and material are null)");
            throw new IllegalArgumentException("Required field material is null");
        }

        String grade = rfq.getMaterialGrade() != null ? rfq.getMaterialGrade() : rfq.getGrade();
        if (grade == null || grade.trim().isEmpty()) {
            log.warn("Validation failed: Required field grade is null or empty (both materialGrade and grade are null)");
            throw new IllegalArgumentException("Material grade is missing");
        }

        String dimension = rfq.getRequiredDimension() != null ? rfq.getRequiredDimension() : rfq.getDimension();
        if (dimension == null || dimension.trim().isEmpty()) {
            log.warn("Validation failed: Required field dimension is null or empty (both requiredDimension and dimension are null)");
            throw new IllegalArgumentException("Required field dimension is null");
        }

        String qtyStr = null;
        if (rfq.getQuantityValue() != null && rfq.getQuantityUnit() != null) {
            qtyStr = rfq.getQuantityValue() + " " + rfq.getQuantityUnit();
        } else {
            qtyStr = rfq.getQuantity();
        }
        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            log.warn("Validation failed: Required field quantity is null or empty");
            throw new IllegalArgumentException("Required field quantity is null");
        }

        String manufacturerUsername = selectedQuote.getManufacturer();
        if (manufacturerUsername == null || manufacturerUsername.trim().isEmpty()) {
            log.warn("Validation failed: Required field manufacturer is null or empty");
            throw new IllegalArgumentException("Required field manufacturer is null");
        }

        // Apply Status Updates
        log.info("Setting quotation status to ACCEPTED for quotation ID: {}", id);
        selectedQuote.setStatus("ACCEPTED");
        Quotation saved = quotationRepository.save(selectedQuote);

        // Reject other quotations for this RFQ
        log.info("Rejecting other quotations for RFQ Number: {}", selectedQuote.getRfqNumber());
        List<Quotation> allQuotes = quotationRepository.findByRfqNumber(selectedQuote.getRfqNumber());
        int rejectedCount = 0;
        for (Quotation q : allQuotes) {
            if (!q.getId().equals(id)) {
                q.setStatus("REJECTED");
                quotationRepository.save(q);
                rejectedCount++;
            }
        }
        log.info("Rejected {} other quotations", rejectedCount);

        // Update RFQ status to QUOTATION_SELECTED
        log.info("Updating RFQ status to QUOTATION_SELECTED for RFQ Number: {}", selectedQuote.getRfqNumber());
        rfqService.updateStatus(selectedQuote.getRfqNumber(), "QUOTATION_SELECTED", operator);

        // Auto Generate Purchase Order
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setMaterial(material);
        po.setGrade(grade);
        po.setDimension(dimension);
        po.setQuantity(qtyStr);
        po.setManufacturer(manufacturerUsername);
        po.setStatus("PENDING");
        
        // Populate Company Master Links and Approval Status
        po.setApprovalStatus("APPROVED");
        po.setRfqNumber(rfq.getRfqNumber());
        po.setQuotationReferenceNumber(String.valueOf(selectedQuote.getId()));
        
        // supplier company details
        Optional<User> supplierUserOpt = userRepository.findByUsername(supplier);
        if (supplierUserOpt.isEmpty()) {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if ("SUPPLIER".equalsIgnoreCase(u.getRole()) && 
                    (supplier.equalsIgnoreCase(u.getCompanyName()) || 
                     (u.getCompany() != null && supplier.equalsIgnoreCase(u.getCompany().getName())))) {
                    supplierUserOpt = Optional.of(u);
                    break;
                }
            }
        }
        if (supplierUserOpt.isPresent()) {
            User sUser = supplierUserOpt.get();
            po.setSupplierUser(sUser);
            po.setSupplierUsername(sUser.getUsername());
            if (sUser.getCompany() != null) {
                po.setSupplierCompany(sUser.getCompany());
                po.setSupplierCompanyName(sUser.getCompany().getName());
                po.setSupplierAddress(sUser.getCompany().getAddress());
                po.setSupplierGstNumber(sUser.getCompany().getGstNumber());
            } else {
                po.setSupplierCompanyName(sUser.getCompanyName());
                po.setSupplierAddress(sUser.getAddress());
                po.setSupplierGstNumber(sUser.getGstNumber());
            }
        }
        
        // manufacturer company details
        if (manufacturerOpt.isPresent()) {
            User mUser = manufacturerOpt.get();
            po.setManufacturerUser(mUser);
            po.setManufacturerUsername(mUser.getUsername());
            if (mUser.getCompany() != null) {
                po.setManufacturerCompany(mUser.getCompany());
                po.setManufacturerCompanyName(mUser.getCompany().getName());
                po.setManufacturerAddress(mUser.getCompany().getAddress());
                po.setManufacturerGstNumber(mUser.getCompany().getGstNumber());
            } else {
                po.setManufacturerCompanyName(mUser.getCompanyName());
                po.setManufacturerAddress(mUser.getAddress());
                po.setManufacturerGstNumber(mUser.getGstNumber());
            }
        }
        
        // material specifications
        po.setMaterialName(rfq.getMaterialName() != null ? rfq.getMaterialName() : rfq.getMaterial());
        po.setMaterialDescription(rfq.getMaterialDescription());
        po.setMaterialGrade(rfq.getMaterialGrade() != null ? rfq.getMaterialGrade() : rfq.getGrade());
        po.setMaterialType(rfq.getMaterialType());
        po.setThickness(rfq.getThickness());
        po.setWidth(rfq.getWidth());
        po.setLength(rfq.getLength());
        po.setDiameter(rfq.getDiameter());
        po.setRequiredDimension(rfq.getRequiredDimension() != null ? rfq.getRequiredDimension() : rfq.getDimension());
        po.setQuantityValue(rfq.getQuantityValue());
        po.setQuantityUnit(rfq.getQuantityUnit());
        
        // commercial details
        po.setUnitPrice(selectedQuote.getUnitPrice());
        po.setTotalPrice(selectedQuote.getPrice());
        po.setCurrency("INR");
        po.setGstTax(18.0);
        po.setDeliveryTerms(selectedQuote.getDeliveryTerms());
        
        // delivery & quality requirements
        po.setRequiredDeliveryDate(rfq.getRequiredDeliveryDate());
        po.setDeliveryLocation(rfq.getDeliveryLocation());
        po.setMtcRequired(rfq.getMtcRequired() != null ? rfq.getMtcRequired() : false);

        log.info("Attempting to generate Purchase Order with detailed payload: Supplier={}, Material={}, Grade={}, Dimension={}, Quantity={}, Manufacturer={}, TotalPrice={}",
                 supplier, material, grade, dimension, qtyStr, manufacturerUsername, po.getTotalPrice());

        PurchaseOrder savedPo;
        try {
            savedPo = poService.createPurchaseOrder(po);
            log.info("Purchase Order generated successfully. PO Number: {}", savedPo.getPoNumber());
        } catch (Exception e) {
            log.error("Purchase Order generation failed with exception: ", e);
            throw new RuntimeException("PO generation failed: " + e.getMessage(), e);
        }

        // Update RFQ status to PO_GENERATED
        log.info("Updating RFQ status to PO_GENERATED for RFQ Number: {}", rfq.getRfqNumber());
        rfqService.updateStatus(rfq.getRfqNumber(), "PO_GENERATED", operator);

        // Notify Manufacturer
        log.info("Sending PO auto-release notification to manufacturer: {}", selectedQuote.getManufacturer());
        notificationService.createNotification(
                selectedQuote.getManufacturer(),
                "Purchase Order " + savedPo.getPoNumber() + " auto-released for RFQ " + rfq.getRfqNumber()
        );

        auditLogService.logActivity("Accepted Quotation for RFQ: " + selectedQuote.getRfqNumber() + " from manufacturer: " + selectedQuote.getManufacturer(), operator);
        return saved;
    }

    public List<Quotation> getAllQuotations() {
        return quotationRepository.findAll();
    }
}
