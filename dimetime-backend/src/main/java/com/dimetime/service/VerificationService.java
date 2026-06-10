package com.dimetime.service;

import com.dimetime.entity.*;
import com.dimetime.repository.VerificationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VerificationService {

    @Autowired
    private VerificationResultRepository verificationRepository;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private MaterialUploadService uploadService;

    @Autowired
    private MtcDocumentService mtcService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private GrnService grnService;

    public VerificationResult performVerification(String poNumber, String verifiedBy) {
        // 1. Fetch Purchase Order
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("Purchase Order " + poNumber + " not found");
        }
        PurchaseOrder po = poOpt.get();

        // 2. Fetch Latest Material Upload and MTC Document specific to this PO
        Optional<MaterialUpload> uploadOpt = uploadService.getLatestUploadForPo(poNumber);
        if (uploadOpt.isEmpty()) {
            uploadOpt = uploadService.getLatestUpload();
        }
        Optional<MtcDocument> mtcOpt = mtcService.getLatestDocumentForPo(poNumber);
        if (mtcOpt.isEmpty()) {
            mtcOpt = mtcService.getLatestDocument();
        }

        if (uploadOpt.isEmpty()) {
            throw new IllegalStateException("No material upload found. Please upload material image first.");
        }
        if (mtcOpt.isEmpty()) {
            throw new IllegalStateException("No MTC certificate found. Please upload MTC document first.");
        }

        MaterialUpload upload = uploadOpt.get();
        MtcDocument mtc = mtcOpt.get();

        // 3. Perform 5-Way Reconciliation (Grade, Dimension, Quantity, Heat Number, Batch Number)
        // 1. Grade Match (25%)
        boolean gradeMatch = false;
        if (po.getGrade() != null && upload.getGrade() != null && mtc.getGrade() != null) {
            String gPo = cleanGradeString(po.getGrade());
            String gOcr = cleanGradeString(upload.getGrade());
            String gMtc = cleanGradeString(mtc.getGrade());
            gradeMatch = gPo.equals(gOcr) && gOcr.equals(gMtc);
        }

        // 2. Dimension Match (25%)
        boolean dimensionMatch = false;
        if (po.getDimension() != null && upload.getDimension() != null && mtc.getDimension() != null) {
            dimensionMatch = checkDimensionMatch(po.getDimension(), upload.getDimension()) && 
                             checkDimensionMatch(upload.getDimension(), mtc.getDimension());
        }

        // 3. Quantity Match (25%)
        boolean quantityMatch = false;
        if (po.getQuantity() != null && upload.getQuantity() != null && mtc.getQuantity() != null) {
            quantityMatch = checkQuantityMatch(po.getQuantity(), upload.getQuantity()) &&
                            checkQuantityMatch(upload.getQuantity(), mtc.getQuantity());
        }

        // 4. Heat Number Match (15%)
        boolean heatMatch = false;
        if (upload.getHeatNumber() != null && mtc.getHeatNumber() != null) {
            String hOcr = upload.getHeatNumber().trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
            String hMtc = mtc.getHeatNumber().trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
            heatMatch = hOcr.equals(hMtc) || hOcr.contains(hMtc) || hMtc.contains(hOcr);
        }

        // 5. Batch Number Match (10%)
        boolean batchMatch = false;
        if (upload.getBatchNumber() != null && mtc.getBatchNumber() != null) {
            String bOcr = upload.getBatchNumber().trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
            String bMtc = mtc.getBatchNumber().trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
            batchMatch = bOcr.equals(bMtc) || bOcr.contains(bMtc) || bMtc.contains(bOcr);
        }

        double score = 0.0;
        if (gradeMatch) score += 25.0;
        if (dimensionMatch) score += 25.0;
        if (quantityMatch) score += 25.0;
        if (heatMatch) score += 15.0;
        if (batchMatch) score += 10.0;

        double finalPercentage = Math.round(score * 100.0) / 100.0;

        String status;
        if (gradeMatch && dimensionMatch && quantityMatch && heatMatch && batchMatch) {
            status = "APPROVED";
            finalPercentage = 100.0;
        } else {
            status = "REVIEW_REQUIRED";
        }

        VerificationResult result = new VerificationResult(
                poNumber,
                upload.getHeatNumber(),
                upload.getGrade(),
                upload.getDimension(),
                upload.getQuantity(),
                heatMatch,
                gradeMatch,
                dimensionMatch,
                quantityMatch,
                batchMatch,
                true,
                finalPercentage,
                status,
                verifiedBy
        );

        verificationRepository.save(result);

        // Transition PO Status
        if ("APPROVED".equalsIgnoreCase(status)) {
            try {
                grnService.generateGrn(poNumber, verifiedBy);
            } catch (Exception e) {
                System.err.println("Auto GRN generation failed: " + e.getMessage());
            }
        } else {
            poService.updateStatus(poNumber, "RECONCILED", verifiedBy);
        }

        // Log audit events
        auditLogService.logActivity("AI Reconciliation completed for PO: " + poNumber + " - Status: " + status + " (" + finalPercentage + "% Match)", verifiedBy);
        
        return result;
    }

    public List<VerificationResult> getVerificationsForPo(String poNumber) {
        return verificationRepository.findByPoNumberOrderByVerifiedAtDesc(poNumber);
    }

    public List<VerificationResult> getAllVerifications() {
        return verificationRepository.findAll();
    }

    public List<VerificationResult> getVerificationsByStatus(String status) {
        return verificationRepository.findByStatus(status);
    }

    private String cleanGradeString(String grade) {
        if (grade == null) return "";
        String clean = grade.toUpperCase().replaceAll("[^A-Z0-9]", "");
        while (clean.startsWith("GRADE") || clean.startsWith("GR") || clean.startsWith("SS")) {
            if (clean.startsWith("GRADE")) {
                clean = clean.substring(5);
            } else if (clean.startsWith("GR")) {
                clean = clean.substring(2);
            } else if (clean.startsWith("SS")) {
                clean = clean.substring(2);
            }
        }
        return clean;
    }

    private boolean checkDimensionMatch(String dim1, String dim2) {
        if (dim1 == null && dim2 == null) return true;
        if (dim1 == null || dim2 == null) return false;
        
        List<Double> nums1 = extractNumbers(dim1);
        List<Double> nums2 = extractNumbers(dim2);
        
        if (!nums1.isEmpty() && !nums2.isEmpty()) {
            if (nums1.size() == nums2.size()) {
                boolean allMatch = true;
                for (int i = 0; i < nums1.size(); i++) {
                    double diff = Math.abs(nums1.get(i) - nums2.get(i));
                    if (diff > 0.1) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) return true;
            }
            // Fuzzy check
            List<Double> larger = nums1.size() >= nums2.size() ? nums1 : nums2;
            List<Double> smaller = nums1.size() < nums2.size() ? nums1 : nums2;
            int matchCount = 0;
            int lastIdx = -1;
            for (Double s : smaller) {
                for (int i = lastIdx + 1; i < larger.size(); i++) {
                    if (Math.abs(larger.get(i) - s) < 0.1) {
                        matchCount++;
                        lastIdx = i;
                        break;
                    }
                }
            }
            double ratio = (double) matchCount / smaller.size();
            if (ratio >= 0.9) return true;
        }
        
        String clean1 = dim1.toLowerCase().replaceAll("(?i)\\b(mm|inch|in|meter|m)\\b", "").replaceAll("[\\s\\-x\\*\\.]", "");
        String clean2 = dim2.toLowerCase().replaceAll("(?i)\\b(mm|inch|in|meter|m)\\b", "").replaceAll("[\\s\\-x\\*\\.]", "");
        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }

    private List<Double> extractNumbers(String text) {
        List<Double> numbers = new java.util.ArrayList<>();
        if (text == null) return numbers;
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                numbers.add(Double.parseDouble(m.group(1)));
            } catch (Exception e) {}
        }
        return numbers;
    }

    private boolean checkQuantityMatch(String qty1, String qty2) {
        if (qty1 == null && qty2 == null) return true;
        if (qty1 == null || qty2 == null) return false;
        
        double num1 = parseNumeric(qty1);
        double num2 = parseNumeric(qty2);
        
        if (num1 > 0 && num2 > 0) {
            if (Math.abs(num1 - num2) < 0.1) {
                return true;
            }
            double ratio = Math.min(num1, num2) / Math.max(num1, num2);
            return ratio >= 0.9;
        }
        
        String clean1 = qty1.toLowerCase().replaceAll("[\\s\\.\\-]", "").replaceAll("(pcs|pieces|unit|kg|tons|ton|mt)", "");
        String clean2 = qty2.toLowerCase().replaceAll("[\\s\\.\\-]", "").replaceAll("(pcs|pieces|unit|kg|tons|ton|mt)", "");
        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }

    private boolean checkFuzzyDescMatch(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        String clean1 = s1.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        String clean2 = s2.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (clean1.contains(clean2) || clean2.contains(clean1)) {
            return true;
        }
        String[] w1 = clean1.split(" ");
        String[] w2 = clean2.split(" ");
        java.util.Set<String> set1 = new java.util.HashSet<>();
        for (String w : w1) {
            if (w.trim().length() > 2) set1.add(w.trim());
        }
        java.util.Set<String> set2 = new java.util.HashSet<>();
        for (String w : w2) {
            if (w.trim().length() > 2) set2.add(w.trim());
        }
        if (set1.isEmpty() || set2.isEmpty()) return false;
        int common = 0;
        for (String w : set1) {
            if (set2.contains(w)) {
                common++;
            }
        }
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        double jaccard = (double) common / union.size();
        double ratio = (double) common / Math.min(set1.size(), set2.size());
        return jaccard >= 0.3 || ratio >= 0.5;
    }

    private double parseNumeric(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {}
        return 0.0;
    }

    @Transactional
    public void deleteVerificationResult(Long id, String operator) {
        Optional<VerificationResult> vrOpt = verificationRepository.findById(id);
        if (vrOpt.isPresent()) {
            String poNum = vrOpt.get().getPoNumber();
            verificationRepository.deleteById(id);
            auditLogService.logActivity("Deleted Verification Result for PO: " + poNum, operator);
        }
    }
}
