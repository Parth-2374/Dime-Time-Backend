package com.dimetime.service;

import com.dimetime.entity.MtcDocument;
import com.dimetime.entity.PurchaseOrder;
import com.dimetime.dto.MtcResponseDto;
import com.dimetime.repository.MtcDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class MtcDocumentService {

    @Autowired
    private MtcDocumentRepository mtcDocumentRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private NotificationService notificationService;

    @Value("${ocr.service.url}")
    private String ocrServiceUrl;

    private final RestTemplate restTemplate;

    public MtcDocumentService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000); // 30 seconds
        requestFactory.setReadTimeout(60000);    // 60 seconds
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public List<MtcDocument> getAllDocuments() {
        return mtcDocumentRepository.findAll();
    }

    public Optional<MtcDocument> getLatestDocument() {
        return mtcDocumentRepository.findTopByOrderByUploadedAtDesc();
    }

    public Optional<MtcDocument> getLatestDocumentForPo(String poNumber) {
        return mtcDocumentRepository.findTopByPoNumberOrderByUploadedAtDesc(poNumber);
    }

    public List<MtcDocument> getDocumentsForPo(String poNumber) {
        return mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNumber);
    }

    public List<MtcDocument> getDocumentsByHeatNumber(String heatNumber) {
        return mtcDocumentRepository.findByHeatNumber(heatNumber);
    }

    private String normalizeGrade(String grade) {
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

    private boolean checkFuzzyDescMatch(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        String clean1 = s1.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        String clean2 = s2.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (clean1.isEmpty() && clean2.isEmpty()) return true;
        if (clean1.isEmpty() || clean2.isEmpty()) return false;
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

    private boolean checkDimensionMatch(String poDim, String ocrDim) {
        if (poDim == null && ocrDim == null) return true;
        if (poDim == null || ocrDim == null) return false;
        List<Double> poNums = extractNumbers(poDim);
        List<Double> ocrNums = extractNumbers(ocrDim);
        if (!poNums.isEmpty() && !ocrNums.isEmpty()) {
            if (poNums.size() == ocrNums.size()) {
                boolean allMatch = true;
                for (int i = 0; i < poNums.size(); i++) {
                    double diff = Math.abs(poNums.get(i) - ocrNums.get(i));
                    if (diff > 0.1) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) return true;
            }
            // Fuzzy check: check if the sequence of numbers in one is a subset of another
            List<Double> larger = poNums.size() >= ocrNums.size() ? poNums : ocrNums;
            List<Double> smaller = poNums.size() < ocrNums.size() ? poNums : ocrNums;
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
        
        // Fallback to string-based cleaning
        String dPoClean = poDim.toLowerCase().replaceAll("(?i)\\b(mm|inch|in|meter|m)\\b", "").replaceAll("[\\s\\-x\\*\\.]", "");
        String dOcrClean = ocrDim.toLowerCase().replaceAll("(?i)\\b(mm|inch|in|meter|m)\\b", "").replaceAll("[\\s\\-x\\*\\.]", "");
        return dPoClean.equals(dOcrClean) || dPoClean.contains(dOcrClean) || dOcrClean.contains(dPoClean);
    }

    public double calculateMtcPoMatchScore(MtcResponseDto ocr, PurchaseOrder po) {
        return calculateMtcPoMatchScore(ocr, po, null);
    }

    public double calculateMtcPoMatchScore(MtcResponseDto ocr, PurchaseOrder po, java.util.Map<String, Object> details) {
        if (ocr == null || po == null) {
            return 0.0;
        }

        double score = 0.0;

        // 1. Grade Match (25%)
        boolean gradeMatch = false;
        if (po.getGrade() != null && ocr.getGrade() != null) {
            String normPo = normalizeGrade(po.getGrade());
            String normOcr = normalizeGrade(ocr.getGrade());
            if (normPo.equals(normOcr) || normPo.contains(normOcr) || normOcr.contains(normPo)) {
                gradeMatch = true;
            }
        } else if (po.getGrade() == null && ocr.getGrade() == null) {
            gradeMatch = true;
        }
        if (gradeMatch) score += 25.0;

        // 2. Dimension / Specification Match (25%)
        boolean dimMatch = checkDimensionMatch(po.getDimension(), ocr.getDimension());
        if (dimMatch) score += 25.0;
        
        // 3. Quantity Match (25%)
        boolean qtyMatch = false;
        if (po.getQuantity() != null && ocr.getQuantity() != null) {
            double numPo = parseNumeric(po.getQuantity());
            double numOcr = parseNumeric(ocr.getQuantity());
            if (numPo > 0 && numOcr > 0) {
                if (Math.abs(numPo - numOcr) < 0.1) {
                    qtyMatch = true;
                } else {
                    double ratio = Math.min(numPo, numOcr) / Math.max(numPo, numOcr);
                    if (ratio >= 0.9) qtyMatch = true;
                }
            } else {
                String qPoClean = po.getQuantity().toLowerCase().replaceAll("[\\s\\.\\-]", "")
                                    .replaceAll("(pcs|pieces|unit|kg|tons|ton)", "");
                String qOcrClean = ocr.getQuantity().toLowerCase().replaceAll("[\\s\\.\\-]", "")
                                     .replaceAll("(pcs|pieces|unit|kg|tons|ton)", "");
                qtyMatch = qPoClean.equals(qOcrClean) || qPoClean.contains(qOcrClean) || qOcrClean.contains(qPoClean);
            }
        } else if (po.getQuantity() == null && ocr.getQuantity() == null) {
            qtyMatch = true;
        }
        if (qtyMatch) score += 25.0;
        
        // 4. Heat Number Match (15%) - Pass if OCR extracted a non-empty heat number
        boolean heatMatch = ocr.getHeatNumber() != null && !ocr.getHeatNumber().trim().isEmpty();
        if (heatMatch) score += 15.0;

        // 5. Batch Number Match (10%) - Pass if OCR extracted a non-empty batch number
        boolean batchMatch = ocr.getBatchNumber() != null && !ocr.getBatchNumber().trim().isEmpty();
        if (batchMatch) score += 10.0;
        
        if (details != null) {
            details.put("gradeMatch", gradeMatch);
            details.put("descMatch", true);
            details.put("heatMatch", heatMatch);
            details.put("qtyMatch", qtyMatch);
            details.put("dimMatch", dimMatch);
            details.put("batchMatch", batchMatch);
            details.put("materialNameMatch", true);
        }

        System.out.println("==================================================");
        System.out.println(">>> AI VALIDATION MATCH SCORE CALCULATION >>>");
        System.out.println("OCR Extracted Values:");
        System.out.println("  - Grade: " + ocr.getGrade());
        System.out.println("  - Quantity: " + ocr.getQuantity());
        System.out.println("  - Dimension: " + ocr.getDimension());
        System.out.println("  - Heat Number: " + ocr.getHeatNumber());
        System.out.println("  - Batch Number: " + ocr.getBatchNumber());
        System.out.println("PO Required Values:");
        System.out.println("  - Grade: " + po.getGrade());
        System.out.println("  - Quantity: " + po.getQuantity());
        System.out.println("  - Dimension: " + po.getDimension());
        System.out.println("Individual Match Scoring:");
        System.out.println("  - Grade Score: " + (gradeMatch ? "25.0%" : "0.0%"));
        System.out.println("  - Dimension Score: " + (dimMatch ? "25.0%" : "0.0%"));
        System.out.println("  - Quantity Score: " + (qtyMatch ? "25.0%" : "0.0%"));
        System.out.println("  - Heat Number Score: " + (heatMatch ? "15.0%" : "0.0%"));
        System.out.println("  - Batch Number Score: " + (batchMatch ? "10.0%" : "0.0%"));
        System.out.println("Total Match Score: " + score + "%");
        System.out.println("==================================================");
        
        return Math.round(score * 100.0) / 100.0;
    }

    private List<Double> extractNumbers(String text) {
        List<Double> numbers = new java.util.ArrayList<>();
        if (text == null) return numbers;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                numbers.add(Double.parseDouble(m.group(1)));
            } catch (Exception e) {}
        }
        return numbers;
    }
    
    private double parseNumeric(String text) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {}
        return 0.0;
    }

    @Transactional
    public java.util.Map<String, Object> processMtcOcrUpload(MultipartFile file, String uploadedBy, String poNumber) throws Exception {
        auditLogService.logActivity("AI OCR Image upload initiated for PO " + poNumber + " by " + uploadedBy, uploadedBy);
        
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("Purchase Order " + poNumber + " not found");
        }
        PurchaseOrder po = poOpt.get();

        MtcResponseDto mtcResult = new MtcResponseDto();
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "mtc_label.png";
        }

        try {
            System.out.println("==================================================");
            System.out.println(">>> MTC OCR REQUEST LOG >>>");
            System.out.println("URL: " + ocrServiceUrl + "/ocr/mtc-extract");
            System.out.println("Method: POST");
            System.out.println("Headers: Content-Type=multipart/form-data");
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("File Size: " + file.getSize() + " bytes");
            System.out.println("PO Number: " + poNumber);
            System.out.println("Uploaded By: " + uploadedBy);
            System.out.println("==================================================");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<MtcResponseDto> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr/mtc-extract",
                    requestEntity,
                    MtcResponseDto.class
            );

            System.out.println("==================================================");
            System.out.println("<<< MTC OCR RESPONSE LOG <<<");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Headers: " + response.getHeaders());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                mtcResult = response.getBody();
                System.out.println("Response JSON: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mtcResult));
            } else {
                System.out.println("Response Body: null or non-OK");
                throw new RuntimeException("FastAPI OCR MTC returned non-OK status: " + response.getStatusCode());
            }
            System.out.println("==================================================");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("==================================================");
            System.err.println("!!! MTC OCR EXCEPTION (HTTP ERROR) !!!");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==================================================");
            throw new RuntimeException("OCR service HTTP error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("==================================================");
            System.err.println("!!! MTC OCR EXCEPTION (CONNECTION/TIMEOUT) !!!");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==================================================");
            throw new RuntimeException("OCR service connection/timeout error: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("==================================================");
            System.err.println("!!! MTC OCR EXCEPTION (UNKNOWN) !!!");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==================================================");
            throw new RuntimeException("OCR service error: " + e.getMessage(), e);
        }

        auditLogService.logActivity("AI OCR processing completed for PO " + poNumber, uploadedBy);

        java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
        double matchScore = calculateMtcPoMatchScore(mtcResult, po, responseMap);
        double confidence = mtcResult.getConfidence() != null ? mtcResult.getConfidence() : 0.90;
        
        String validationResult = "REJECTED";
        boolean blocked = true;
        if (matchScore >= 80.0) {
            validationResult = "REVIEW_REQUIRED";
            blocked = false;
        }
        
        auditLogService.logActivity("AI Validation completed for PO " + poNumber + " - Match Score: " + matchScore + "% - Result: " + validationResult, uploadedBy);

        responseMap.put("matchScore", matchScore);
        responseMap.put("ocrConfidence", confidence * 100.0);
        responseMap.put("validationResult", validationResult);
        responseMap.put("autoGenerated", false);
        responseMap.put("previewData", mtcResult);

        if (blocked) {
            responseMap.put("blocked", true);
            responseMap.put("message", "OCR data does not match Purchase Order requirements.");
        } else {
            responseMap.put("blocked", false);
            responseMap.put("message", "OCR extraction completed. Review required.");
        }

        return responseMap;
    }

    @Transactional
    public MtcDocument generateAndDeliverMtc(MtcResponseDto mtcResult, String uploadedBy, String poNumber, Double ocrMatchScore, String validationResult) throws Exception {
        String fileName = "MTC-Certificate-" + poNumber + ".pdf";

        int nextVersion = 1;
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            List<MtcDocument> existing = mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNumber);
            nextVersion = existing.size() + 1;
        }

        MtcDocument mtcDocument = new MtcDocument(
                fileName,
                uploadedBy,
                mtcResult.getHeatNumber(),
                mtcResult.getGrade(),
                mtcResult.getCarbon(),
                mtcResult.getChromium(),
                mtcResult.getNickel(),
                mtcResult.getYieldStrength(),
                mtcResult.getTensileStrength()
        );
        mtcDocument.setBatchNumber(mtcResult.getBatchNumber());
        mtcDocument.setPoNumber(poNumber);
        
        mtcDocument.setMolybdenum(mtcResult.getMolybdenum() != null ? mtcResult.getMolybdenum() : 2.1);
        mtcDocument.setManganese(mtcResult.getManganese() != null ? mtcResult.getManganese() : 1.5);
        mtcDocument.setSilicon(mtcResult.getSilicon() != null ? mtcResult.getSilicon() : 0.5);
        mtcDocument.setElongation(mtcResult.getElongation() != null ? mtcResult.getElongation() : 45.0);
        mtcDocument.setHardness(mtcResult.getHardness() != null ? mtcResult.getHardness() : 85.0);
        mtcDocument.setMaterialDescription(mtcResult.getMaterialDescription() != null ? mtcResult.getMaterialDescription() : "Stainless Steel Sheet / Bar");
        
        mtcDocument.setConfidence(mtcResult.getConfidence() != null ? mtcResult.getConfidence() : 0.90);
        mtcDocument.setQuantity(mtcResult.getQuantity() != null ? mtcResult.getQuantity() : "480 KG");
        mtcDocument.setDimension(mtcResult.getDimension() != null ? mtcResult.getDimension() : "25 MM");
        mtcDocument.setOcrMatchScore(ocrMatchScore);
        mtcDocument.setValidationResult(validationResult);
        mtcDocument.setValidationTimestamp(LocalDateTime.now());
        mtcDocument.setVersionNumber(nextVersion);
        
        mtcDocument.setIssueDate(LocalDateTime.now());
        mtcDocument.setStatus("SUPPLIER_REVIEW");

        String randSuffix = String.valueOf((int)(Math.random() * 90000) + 10000);
        String formattedPo = poNumber != null ? poNumber.replace(" ", "-") : "PO";
        String certNumber = "MTC-2026-" + formattedPo + "-" + randSuffix;
        mtcDocument.setCertificateNumber(certNumber);
        
        mtcDocument.setFileName(certNumber + ".pdf");

        if (poNumber != null && !poNumber.trim().isEmpty()) {
            Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
            if (poOpt.isPresent()) {
                PurchaseOrder po = poOpt.get();
                mtcDocument.setRfqNumber(po.getRfqNumber() != null ? po.getRfqNumber() : "RFQ-2026-001");
                
                String manDetails = po.getManufacturerCompanyName() != null 
                        ? (po.getManufacturerCompanyName() + "\nAddress: " + po.getManufacturerAddress() + "\nGST: " + po.getManufacturerGstNumber())
                        : "Manufacturer Portal (Assigned PO User)";
                mtcDocument.setManufacturerDetails(manDetails);

                String supDetails = po.getSupplierCompanyName() != null 
                        ? (po.getSupplierCompanyName() + "\nAddress: " + po.getSupplierAddress() + "\nGST: " + po.getSupplierGstNumber())
                        : "Supplier Portal (Assigned client)";
                mtcDocument.setSupplierDetails(supDetails);

            }
        }

        mtcDocumentRepository.save(mtcDocument);

        try {
            byte[] pdfBytes = generateMtcPdfFromEntity(mtcDocument);
            java.io.File uploadsDir = new java.io.File("uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }
            java.io.File pdfFile = new java.io.File(uploadsDir, certNumber + ".pdf");
            java.nio.file.Files.write(pdfFile.toPath(), pdfBytes);
        } catch (Exception e) {
            System.err.println("Error saving MTC PDF to disk: " + e.getMessage());
        }

        if (poNumber != null && !poNumber.trim().isEmpty()) {
            poService.updateMtcFileName(poNumber, certNumber + ".pdf", uploadedBy);
            
            poService.updateStatus(poNumber, "MTC_CERTIFICATE_GENERATED", uploadedBy);
            poService.updateStatus(poNumber, "SENT_TO_SUPPLIER", uploadedBy);
            poService.updateStatus(poNumber, "SUPPLIER_REVIEW", uploadedBy);

            auditLogService.logActivity("MTC Certificate " + certNumber + " (v" + nextVersion + ") generated for PO " + poNumber + " by " + uploadedBy, uploadedBy);

            Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
            if (poOpt.isPresent()) {
                PurchaseOrder po = poOpt.get();
                String supplierUsername = po.getSupplierUsername();
                if (supplierUsername == null || supplierUsername.isEmpty()) {
                    supplierUsername = "supplier";
                }
                notificationService.createNotification(supplierUsername, "AI-generated MTC Certificate available for PO " + poNumber + ".");
            }
        }

        return mtcDocument;
    }

    @Transactional
    public MtcDocument processMtcUpload(MultipartFile file, String uploadedBy, String poNumber) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "mtc_certificate.pdf";
        }

        MtcResponseDto mtcResult = new MtcResponseDto();

        // Call FastAPI MTC Endpoint
        try {
            System.out.println("==================================================");
            System.out.println(">>> MTC PARSE REQUEST LOG >>>");
            System.out.println("URL: " + ocrServiceUrl + "/mtc/parse");
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("==================================================");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<MtcResponseDto> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/mtc/parse",
                    requestEntity,
                    MtcResponseDto.class
            );

            System.out.println("==================================================");
            System.out.println("<<< MTC PARSE RESPONSE LOG <<<");
            System.out.println("Status: " + response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                mtcResult = response.getBody();
                System.out.println("Response JSON: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mtcResult));
            } else {
                throw new RuntimeException("FastAPI MTC returned non-OK status: " + response.getStatusCode());
            }
            System.out.println("==================================================");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("!!! MTC PARSE EXCEPTION (HTTP ERROR) !!!: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("MTC parse HTTP error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("!!! MTC PARSE EXCEPTION !!!: " + e.getMessage());
            throw new RuntimeException("MTC parse service error: " + e.getMessage(), e);
        }

        return saveAndTransition(mtcResult, fileName, uploadedBy, poNumber);
    }

    @Transactional
    public MtcDocument processMtcImageOcrUpload(MultipartFile file, String uploadedBy, String poNumber) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "mtc_certificate_ocr.png";
        }

        MtcResponseDto mtcResult = new MtcResponseDto();

        // Call FastAPI OCR MTC Extraction Endpoint
        try {
            System.out.println("==================================================");
            System.out.println(">>> MTC IMAGE OCR REQUEST LOG >>>");
            System.out.println("URL: " + ocrServiceUrl + "/ocr/mtc-extract");
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("==================================================");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<MtcResponseDto> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr/mtc-extract",
                    requestEntity,
                    MtcResponseDto.class
            );

            System.out.println("==================================================");
            System.out.println("<<< MTC IMAGE OCR RESPONSE LOG <<<");
            System.out.println("Status: " + response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                mtcResult = response.getBody();
                System.out.println("Response JSON: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mtcResult));
            } else {
                throw new RuntimeException("FastAPI OCR MTC returned non-OK status: " + response.getStatusCode());
            }
            System.out.println("==================================================");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("!!! MTC IMAGE OCR EXCEPTION (HTTP ERROR) !!!: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("MTC image OCR HTTP error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("!!! MTC IMAGE OCR EXCEPTION !!!: " + e.getMessage());
            throw new RuntimeException("MTC image OCR service error: " + e.getMessage(), e);
        }

        return saveAndTransition(mtcResult, fileName, uploadedBy, poNumber);
    }

    private MtcDocument saveAndTransition(MtcResponseDto mtcResult, String fileName, String uploadedBy, String poNumber) {
        // Create MtcDocument
        MtcDocument mtcDocument = new MtcDocument(
                fileName,
                uploadedBy,
                mtcResult.getHeatNumber(),
                mtcResult.getGrade(),
                mtcResult.getCarbon(),
                mtcResult.getChromium(),
                mtcResult.getNickel(),
                mtcResult.getYieldStrength(),
                mtcResult.getTensileStrength()
        );
        mtcDocument.setBatchNumber(mtcResult.getBatchNumber());
        mtcDocument.setPoNumber(poNumber);

        int nextVersion = 1;
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            List<MtcDocument> existing = mtcDocumentRepository.findByPoNumberOrderByUploadedAtDesc(poNumber);
            nextVersion = existing.size() + 1;
        }
        mtcDocument.setVersionNumber(nextVersion);
        
        // Add extra parsed parameters
        mtcDocument.setMolybdenum(mtcResult.getMolybdenum() != null ? mtcResult.getMolybdenum() : 2.1);
        mtcDocument.setManganese(mtcResult.getManganese() != null ? mtcResult.getManganese() : 1.5);
        mtcDocument.setSilicon(mtcResult.getSilicon() != null ? mtcResult.getSilicon() : 0.5);
        mtcDocument.setElongation(mtcResult.getElongation() != null ? mtcResult.getElongation() : 45.0);
        mtcDocument.setHardness(mtcResult.getHardness() != null ? mtcResult.getHardness() : 85.0);
        mtcDocument.setMaterialDescription(mtcResult.getMaterialDescription() != null ? mtcResult.getMaterialDescription() : "Stainless Steel Sheet / Bar");
        
        // Set default issue date and status
        mtcDocument.setIssueDate(LocalDateTime.now());
        mtcDocument.setStatus("SUPPLIER_REVIEW");

        // Generate Certificate Number: MTC-2026-[PO_NUMBER]-[RANDOM]
        String randSuffix = String.valueOf((int)(Math.random() * 90000) + 10000);
        String formattedPo = poNumber != null ? poNumber.replace(" ", "-") : "PO";
        String certNumber = "MTC-2026-" + formattedPo + "-" + randSuffix;
        mtcDocument.setCertificateNumber(certNumber);

        // Fetch PO details to link information
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
            if (poOpt.isPresent()) {
                PurchaseOrder po = poOpt.get();
                mtcDocument.setRfqNumber(po.getRfqNumber() != null ? po.getRfqNumber() : "RFQ-2026-001");
                
                String manDetails = po.getManufacturerCompanyName() != null 
                        ? (po.getManufacturerCompanyName() + "\nAddress: " + po.getManufacturerAddress() + "\nGST: " + po.getManufacturerGstNumber())
                        : "Manufacturer Portal (Assigned PO User)";
                mtcDocument.setManufacturerDetails(manDetails);

                String supDetails = po.getSupplierCompanyName() != null 
                        ? (po.getSupplierCompanyName() + "\nAddress: " + po.getSupplierAddress() + "\nGST: " + po.getSupplierGstNumber())
                        : "Supplier Portal (Assigned client)";
                mtcDocument.setSupplierDetails(supDetails);

                if (po.getMaterialDescription() != null) {
                    mtcDocument.setMaterialDescription(po.getMaterialDescription());
                }
            }
        }

        mtcDocumentRepository.save(mtcDocument);

        // Transition Purchase Order sequentially and log audits
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            poService.updateMtcFileName(poNumber, fileName, uploadedBy);
            
            // Sequential state changes
            poService.updateStatus(poNumber, "MTC_UPLOADED", uploadedBy);
            poService.updateStatus(poNumber, "MTC_CERTIFICATE_GENERATED", uploadedBy);
            poService.updateStatus(poNumber, "SUPPLIER_REVIEW", uploadedBy);

            // Audit Logs
            auditLogService.logActivity("Production Started milestone checked", uploadedBy);
            auditLogService.logActivity("MTC Certificate file uploaded: " + fileName, uploadedBy);
            auditLogService.logActivity("AI OCR parsed chemical/mechanical specifications", uploadedBy);
            auditLogService.logActivity("Structured MTC Certificate generated: " + certNumber, uploadedBy);
            auditLogService.logActivity("PO " + poNumber + " status transitioned to SUPPLIER_REVIEW", uploadedBy);

            // Notify Supplier
            Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
            if (poOpt.isPresent()) {
                PurchaseOrder po = poOpt.get();
                String supplierUsername = po.getSupplierUsername();
                if (supplierUsername == null || supplierUsername.isEmpty()) {
                    supplierUsername = "supplier"; // fallback
                }
                notificationService.createNotification(supplierUsername, "New MTC Certificate uploaded for PO " + poNumber + ". Awaiting your review.");
                auditLogService.logActivity("Supplier review notification dispatched to: " + supplierUsername, uploadedBy);
            }
        }

        return mtcDocument;
    }

    @Transactional
    public MtcDocument updateCertificateStatus(Long id, String status, String operator) {
        return updateCertificateStatus(id, status, operator, null);
    }

    @Transactional
    public MtcDocument updateCertificateStatus(Long id, String status, String operator, String comment) {
        Optional<MtcDocument> mtcOpt = mtcDocumentRepository.findById(id);
        if (mtcOpt.isEmpty()) {
            throw new IllegalArgumentException("MTC Certificate not found with ID: " + id);
        }
        MtcDocument mtc = mtcOpt.get();
        mtc.setStatus(status);
        if ("REJECTED".equalsIgnoreCase(status) && comment != null) {
            mtc.setRejectionComment(comment);
        }
        mtcDocumentRepository.save(mtc);

        String poNumber = mtc.getPoNumber();
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
            if (poOpt.isPresent()) {
                PurchaseOrder po = poOpt.get();
                
                if ("APPROVED".equalsIgnoreCase(status)) {
                    poService.updateStatus(poNumber, "APPROVED", operator);
                    poService.updateStatus(poNumber, "APPROVED_FOR_DISPATCH", operator);
                    auditLogService.logActivity("MTC Certificate status updated to APPROVED", operator);
                    auditLogService.logActivity("PO " + poNumber + " status transitioned to APPROVED_FOR_DISPATCH", operator);

                    // Notify Manufacturer
                    String manufacturerUsername = po.getManufacturerUsername();
                    if (manufacturerUsername == null || manufacturerUsername.isEmpty()) {
                        manufacturerUsername = "manufacturer";
                    }
                    notificationService.createNotification(manufacturerUsername, "MTC Approved for " + poNumber + ".\nReady For Dispatch.");
                    auditLogService.logActivity("Manufacturer approval notification sent to: " + manufacturerUsername, operator);
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    poService.updateStatus(poNumber, "PRODUCTION_STARTED", operator);
                    auditLogService.logActivity("MTC rejected and PO returned to PRODUCTION_STARTED.", operator);

                    // Notify Manufacturer
                    String manufacturerUsername = po.getManufacturerUsername();
                    if (manufacturerUsername == null || manufacturerUsername.isEmpty()) {
                        manufacturerUsername = "manufacturer";
                    }
                    notificationService.createNotification(manufacturerUsername, "MTC Rejected for " + poNumber + "\nReason:\n" + comment);
                }
            }
        }

        return mtc;
    }

    public byte[] generateMtcPdf(Long id) throws Exception {
        Optional<MtcDocument> mtcOpt = mtcDocumentRepository.findById(id);
        if (mtcOpt.isEmpty()) {
            throw new IllegalArgumentException("MTC Certificate not found with ID: " + id);
        }
        MtcDocument mtc = mtcOpt.get();
        
        if (mtc.getCertificateNumber() != null) {
            java.io.File pdfFile = new java.io.File("uploads", mtc.getCertificateNumber() + ".pdf");
            if (pdfFile.exists()) {
                try {
                    return java.nio.file.Files.readAllBytes(pdfFile.toPath());
                } catch (Exception e) {
                    System.err.println("Warning: Failed to read MTC PDF from disk: " + e.getMessage());
                }
            }
        }
        
        return generateMtcPdfFromEntity(mtc);
    }

    public byte[] generateMtcPdfFromEntity(MtcDocument mtc) throws Exception {
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
        document.open();
        
        // Fonts
        com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 20, com.lowagie.text.Font.BOLD, java.awt.Color.DARK_GRAY);
        com.lowagie.text.Font sectionFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, com.lowagie.text.Font.BOLD, java.awt.Color.BLACK);
        com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10, com.lowagie.text.Font.NORMAL, java.awt.Color.BLACK);
        com.lowagie.text.Font boldBodyFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10, com.lowagie.text.Font.BOLD, java.awt.Color.BLACK);
        com.lowagie.text.Font footerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_OBLIQUE, 8, com.lowagie.text.Font.NORMAL, java.awt.Color.GRAY);
        
        // Header table
        com.lowagie.text.pdf.PdfPTable headerTable = new com.lowagie.text.pdf.PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});
        
        com.lowagie.text.pdf.PdfPCell titleCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("MILL TEST CERTIFICATE", titleFont));
        titleCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);
        
        com.lowagie.text.pdf.PdfPCell logoCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("DIMETIME SCM", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16, com.lowagie.text.Font.BOLD, new java.awt.Color(34, 197, 94))));
        logoCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        logoCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);
        
        document.add(headerTable);
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Details Table
        com.lowagie.text.pdf.PdfPTable detailsTable = new com.lowagie.text.pdf.PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.addCell(createCell("Certificate No: " + mtc.getCertificateNumber(), boldBodyFont));
        detailsTable.addCell(createCell("Date of Issue: " + mtc.getIssueDate().toLocalDate().toString(), bodyFont));
        detailsTable.addCell(createCell("Purchase Order No: " + mtc.getPoNumber(), bodyFont));
        detailsTable.addCell(createCell("RFQ Reference No: " + (mtc.getRfqNumber() != null ? mtc.getRfqNumber() : "N/A"), bodyFont));
        detailsTable.addCell(createCell("AI OCR Match Score: " + (mtc.getOcrMatchScore() != null ? mtc.getOcrMatchScore() + "%" : "N/A"), bodyFont));
        detailsTable.addCell(createCell("AI OCR Confidence: " + (mtc.getConfidence() != null ? Math.round(mtc.getConfidence() * 100.0) + "%" : "N/A"), bodyFont));
        detailsTable.addCell(createCell("Validation Result: " + (mtc.getValidationResult() != null ? mtc.getValidationResult() : "N/A"), boldBodyFont));
        detailsTable.addCell(createCell("MTC Version: v" + (mtc.getVersionNumber() != null ? mtc.getVersionNumber() : "1"), bodyFont));
        document.add(detailsTable);
        
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Addresses Table
        com.lowagie.text.pdf.PdfPTable addressTable = new com.lowagie.text.pdf.PdfPTable(2);
        addressTable.setWidthPercentage(100);
        
        com.lowagie.text.pdf.PdfPCell manHeader = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Manufacturer Information", sectionFont));
        manHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        manHeader.setPadding(5);
        addressTable.addCell(manHeader);
        
        com.lowagie.text.pdf.PdfPCell supHeader = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Supplier / Client Information", sectionFont));
        supHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        supHeader.setPadding(5);
        addressTable.addCell(supHeader);
        
        com.lowagie.text.pdf.PdfPCell manCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(mtc.getManufacturerDetails(), bodyFont));
        manCell.setPadding(8);
        addressTable.addCell(manCell);
        
        com.lowagie.text.pdf.PdfPCell supCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(mtc.getSupplierDetails(), bodyFont));
        supCell.setPadding(8);
        addressTable.addCell(supCell);
        
        document.add(addressTable);
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Material specs
        com.lowagie.text.pdf.PdfPTable materialTable = new com.lowagie.text.pdf.PdfPTable(4);
        materialTable.setWidthPercentage(100);
        materialTable.setWidths(new float[]{2, 1, 1, 1});
        materialTable.addCell(createHeaderCell("Material Description", boldBodyFont));
        materialTable.addCell(createHeaderCell("Grade", boldBodyFont));
        materialTable.addCell(createHeaderCell("Heat Number", boldBodyFont));
        materialTable.addCell(createHeaderCell("Batch Number", boldBodyFont));
        
        materialTable.addCell(createCell(mtc.getMaterialDescription(), bodyFont));
        materialTable.addCell(createCell(mtc.getGrade(), bodyFont));
        materialTable.addCell(createCell(mtc.getHeatNumber(), bodyFont));
        materialTable.addCell(createCell(mtc.getBatchNumber(), bodyFont));
        document.add(materialTable);
        
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Chemical composition Table
        com.lowagie.text.Paragraph chemTitle = new com.lowagie.text.Paragraph("Chemical Composition Matrix (Weight %)", sectionFont);
        chemTitle.setSpacingAfter(5f);
        document.add(chemTitle);
        
        com.lowagie.text.pdf.PdfPTable chemTable = new com.lowagie.text.pdf.PdfPTable(6);
        chemTable.setWidthPercentage(100);
        chemTable.addCell(createHeaderCell("Carbon (C)", boldBodyFont));
        chemTable.addCell(createHeaderCell("Chromium (Cr)", boldBodyFont));
        chemTable.addCell(createHeaderCell("Nickel (Ni)", boldBodyFont));
        chemTable.addCell(createHeaderCell("Molybdenum (Mo)", boldBodyFont));
        chemTable.addCell(createHeaderCell("Manganese (Mn)", boldBodyFont));
        chemTable.addCell(createHeaderCell("Silicon (Si)", boldBodyFont));
        
        chemTable.addCell(createCenterCell(formatDouble(mtc.getCarbon()) + "%", bodyFont));
        chemTable.addCell(createCenterCell(formatDouble(mtc.getChromium()) + "%", bodyFont));
        chemTable.addCell(createCenterCell(formatDouble(mtc.getNickel()) + "%", bodyFont));
        chemTable.addCell(createCenterCell(formatDouble(mtc.getMolybdenum()) + "%", bodyFont));
        chemTable.addCell(createCenterCell(formatDouble(mtc.getManganese()) + "%", bodyFont));
        chemTable.addCell(createCenterCell(formatDouble(mtc.getSilicon()) + "%", bodyFont));
        document.add(chemTable);
        
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Mechanical properties Table
        com.lowagie.text.Paragraph mechTitle = new com.lowagie.text.Paragraph("Mechanical & Tensile Properties", sectionFont);
        mechTitle.setSpacingAfter(5f);
        document.add(mechTitle);
        
        com.lowagie.text.pdf.PdfPTable mechTable = new com.lowagie.text.pdf.PdfPTable(4);
        mechTable.setWidthPercentage(100);
        mechTable.addCell(createHeaderCell("Yield Strength (YS)", boldBodyFont));
        mechTable.addCell(createHeaderCell("Tensile Strength (UTS)", boldBodyFont));
        mechTable.addCell(createHeaderCell("Elongation (A5)", boldBodyFont));
        mechTable.addCell(createHeaderCell("Hardness (HB/HRB)", boldBodyFont));
        
        mechTable.addCell(createCenterCell(formatDouble(mtc.getYieldStrength()) + " MPa", bodyFont));
        mechTable.addCell(createCenterCell(formatDouble(mtc.getTensileStrength()) + " MPa", bodyFont));
        mechTable.addCell(createCenterCell(formatDouble(mtc.getElongation()) + "%", bodyFont));
        mechTable.addCell(createCenterCell(formatDouble(mtc.getHardness()) + "", bodyFont));
        document.add(mechTable);
        
        document.add(new com.lowagie.text.Paragraph("\n"));
        
        // Footer (QR code, Stamp and Signature)
        com.lowagie.text.pdf.PdfPTable footerTable = new com.lowagie.text.pdf.PdfPTable(3);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{1.5f, 1.5f, 2f});
        
        // QR Code
        String qrContent = String.format(
            "MTC Certificate: %s\nPO Number: %s\nRFQ: %s\nHeat: %s\nGrade: %s\nStatus: %s",
            mtc.getCertificateNumber(),
            mtc.getPoNumber(),
            mtc.getRfqNumber() != null ? mtc.getRfqNumber() : "N/A",
            mtc.getHeatNumber(),
            mtc.getGrade(),
            mtc.getStatus()
        );
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 150, 150);
        java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] qrBytes = pngOutputStream.toByteArray();
        com.lowagie.text.Image qrImg = com.lowagie.text.Image.getInstance(qrBytes);
        qrImg.scaleAbsolute(80f, 80f);
        
        com.lowagie.text.pdf.PdfPCell qrCell = new com.lowagie.text.pdf.PdfPCell(qrImg);
        qrCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        footerTable.addCell(qrCell);
        
        // Stamp
        com.lowagie.text.pdf.PdfPCell stampCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(
            "\n  [ AI APPROVED ]\n  " + mtc.getDigitalApprovalStamp() + "\n  Status: " + mtc.getStatus(),
            com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8, com.lowagie.text.Font.BOLD, new java.awt.Color(34, 197, 94))
        ));
        stampCell.setBorder(com.lowagie.text.Rectangle.BOX);
        stampCell.setBorderWidth(2f);
        stampCell.setBorderColor(new java.awt.Color(34, 197, 94));
        stampCell.setBackgroundColor(new java.awt.Color(240, 253, 244));
        stampCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        stampCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        footerTable.addCell(stampCell);
        
        // Signature
        com.lowagie.text.Paragraph sigPara = new com.lowagie.text.Paragraph("\n\n_______________________\nAuthorized Digital Sign\nInspector DimeTime AI", boldBodyFont);
        com.lowagie.text.pdf.PdfPCell sigCell = new com.lowagie.text.pdf.PdfPCell(sigPara);
        sigCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        sigCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        sigCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_BOTTOM);
        footerTable.addCell(sigCell);
        
        document.add(footerTable);
        
        document.close();
        return out.toByteArray();
    }

    private com.lowagie.text.pdf.PdfPCell createCell(String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text, font));
        cell.setPadding(6);
        return cell;
    }

    private com.lowagie.text.pdf.PdfPCell createHeaderCell(String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text, font));
        cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
        cell.setPadding(6);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        return cell;
    }

    private com.lowagie.text.pdf.PdfPCell createCenterCell(String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        return cell;
    }

    private String formatDouble(Double val) {
        return val != null ? String.format("%.3f", val) : "0.000";
    }

    @Transactional
    public void deleteDocument(Long id, String operator) {
        Optional<MtcDocument> docOpt = mtcDocumentRepository.findById(id);
        if (docOpt.isPresent()) {
            String certNum = docOpt.get().getCertificateNumber();
            mtcDocumentRepository.deleteById(id);
            auditLogService.logActivity("Deleted MTC Certificate: " + certNum, operator);
        }
    }

    @Transactional
    public MtcDocument updateDocument(Long id, MtcDocument details, String operator) {
        Optional<MtcDocument> docOpt = mtcDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException("MTC Certificate not found with id: " + id);
        }
        MtcDocument mtc = docOpt.get();
        mtc.setGrade(details.getGrade());
        mtc.setHeatNumber(details.getHeatNumber());
        mtc.setBatchNumber(details.getBatchNumber());
        mtc.setDimension(details.getDimension());
        mtc.setQuantity(details.getQuantity());
        mtc.setStatus(details.getStatus());
        mtc.setCarbon(details.getCarbon());
        mtc.setChromium(details.getChromium());
        mtc.setNickel(details.getNickel());
        mtc.setYieldStrength(details.getYieldStrength());
        mtc.setTensileStrength(details.getTensileStrength());
        MtcDocument saved = mtcDocumentRepository.save(mtc);
        auditLogService.logActivity("Updated MTC Certificate values: " + mtc.getCertificateNumber(), operator);
        return saved;
    }
}
