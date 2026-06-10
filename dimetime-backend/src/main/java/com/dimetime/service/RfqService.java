package com.dimetime.service;

import com.dimetime.entity.*;
import com.dimetime.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RfqService {

    @Autowired
    private RfqRepository rfqRepository;

    @Autowired
    private RfqAssignmentRepository rfqAssignmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public Rfq createRfq(Rfq rfq) {
        long currentCount = rfqRepository.count();
        String rfqNumber = String.format("RFQ-2026-%03d", currentCount + 1);
        rfq.setRfqNumber(rfqNumber);
        rfq.setStatus("CREATED");

        // Map detailed fields to legacy fields for compatibility
        if (rfq.getMaterialName() != null) {
            rfq.setMaterial(rfq.getMaterialName());
        }
        if (rfq.getMaterialGrade() != null) {
            rfq.setGrade(rfq.getMaterialGrade());
        }
        if (rfq.getRequiredDimension() != null) {
            rfq.setDimension(rfq.getRequiredDimension());
        } else if (rfq.getThickness() != null && rfq.getWidth() != null && rfq.getLength() != null) {
            rfq.setDimension(String.format("%s mm x %s mm x %s mm", rfq.getThickness(), rfq.getWidth(), rfq.getLength()));
        }
        if (rfq.getQuantityValue() != null && rfq.getQuantityUnit() != null) {
            rfq.setQuantity(rfq.getQuantityValue() + " " + rfq.getQuantityUnit());
        }

        Rfq saved = rfqRepository.save(rfq);
        auditLogService.logActivity("Created RFQ Draft: " + rfqNumber + " for " + rfq.getMaterial(), rfq.getCreatedBy());
        return saved;
    }

    @Transactional
    public Rfq broadcastRfq(String rfqNumber, String operator) {
        Optional<Rfq> rfqOpt = rfqRepository.findByRfqNumber(rfqNumber);
        if (rfqOpt.isEmpty()) {
            throw new IllegalArgumentException("RFQ not found: " + rfqNumber);
        }
        Rfq rfq = rfqOpt.get();

        // 1. Fetch all active manufacturers from database
        List<User> manufacturers = userRepository.findAll().stream()
                .filter(u -> "MANUFACTURER".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());

        // 2. Create assignments and notifications
        for (User m : manufacturers) {
            // Check if assignment already exists
            Optional<RfqAssignment> existingOpt = rfqAssignmentRepository.findByRfqNumberAndManufacturer(rfqNumber, m.getUsername());
            if (existingOpt.isEmpty()) {
                RfqAssignment assignment = new RfqAssignment(rfqNumber, m.getUsername(), "PENDING");
                rfqAssignmentRepository.save(assignment);
            }

            // Create notification record
            Notification notification = new Notification(
                    m.getUsername(),
                    "New Procurement RFQ Broadcasted: " + rfqNumber + " - " + rfq.getMaterialName() + " (" + rfq.getMaterialGrade() + ")",
                    false
            );
            notificationRepository.save(notification);
        }

        // 3. Update RFQ status to BROADCASTED
        rfq.setStatus("BROADCASTED");
        Rfq saved = rfqRepository.save(rfq);

        auditLogService.logActivity("Broadcasted RFQ: " + rfqNumber + " to " + manufacturers.size() + " manufacturers", operator);
        return saved;
    }

    public List<Rfq> getAllRfqs() {
        return rfqRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Rfq> getRfqByNumber(String rfqNumber) {
        return rfqRepository.findByRfqNumber(rfqNumber);
    }

    public List<Rfq> getRfqsByCreator(String username) {
        return rfqRepository.findByCreatedByOrderByCreatedAtDesc(username);
    }

    public List<Rfq> getAssignedRfqs(String manufacturer) {
        List<RfqAssignment> assignments = rfqAssignmentRepository.findByManufacturer(manufacturer);
        List<String> rfqNumbers = assignments.stream().map(RfqAssignment::getRfqNumber).collect(Collectors.toList());
        return rfqRepository.findByRfqNumberInOrderByCreatedAtDesc(rfqNumbers);
    }

    @Transactional
    public Rfq updateStatus(String rfqNumber, String status, String operator) {
        Optional<Rfq> rfqOpt = rfqRepository.findByRfqNumber(rfqNumber);
        if (rfqOpt.isEmpty()) {
            throw new IllegalArgumentException("RFQ not found: " + rfqNumber);
        }
        Rfq rfq = rfqOpt.get();
        rfq.setStatus(status);
        Rfq saved = rfqRepository.save(rfq);

        auditLogService.logActivity("Updated RFQ " + rfqNumber + " status to " + status, operator);
        return saved;
    }
}
