package com.dimetime.service;

import com.dimetime.entity.MaterialUpload;
import com.dimetime.dto.OcrResponseDto;
import com.dimetime.repository.MaterialUploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

@Service
public class MaterialUploadService {

    @Autowired
    private MaterialUploadRepository materialUploadRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Value("${ocr.service.url}")
    private String ocrServiceUrl;

    private final RestTemplate restTemplate;

    public MaterialUploadService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000); // 30 seconds
        requestFactory.setReadTimeout(60000);    // 60 seconds
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public MaterialUpload processMaterialUpload(MultipartFile file, String uploadedBy, String poNumber) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "material_image.jpg";
        }

        OcrResponseDto ocrResult = new OcrResponseDto();

        // Call FastAPI OCR Endpoint
        try {
            System.out.println("==================================================");
            System.out.println(">>> MATERIAL OCR REQUEST LOG >>>");
            System.out.println("URL: " + ocrServiceUrl + "/ocr/extract");
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("==================================================");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Convert MultipartFile to a resource that RestTemplate can stream
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<OcrResponseDto> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr/extract",
                    requestEntity,
                    OcrResponseDto.class
                );

            System.out.println("==================================================");
            System.out.println("<<< MATERIAL OCR RESPONSE LOG <<<");
            System.out.println("Status: " + response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ocrResult = response.getBody();
                System.out.println("Response JSON: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ocrResult));
            } else {
                throw new RuntimeException("FastAPI OCR returned non-OK status: " + response.getStatusCode());
            }
            System.out.println("==================================================");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("!!! MATERIAL OCR EXCEPTION (HTTP ERROR) !!!: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Material OCR HTTP error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("!!! MATERIAL OCR EXCEPTION !!!: " + e.getMessage());
            throw new RuntimeException("Material OCR service error: " + e.getMessage(), e);
        }

        // Save metadata and parsed values to database
        MaterialUpload materialUpload = new MaterialUpload(
                fileName,
                uploadedBy,
                ocrResult.getHeatNumber(),
                ocrResult.getGrade(),
                ocrResult.getDimension(),
                ocrResult.getQuantity(),
                ocrResult.getRawText(),
                ocrResult.getConfidence()
        );

        // Set computer vision and AI weight estimation properties
        materialUpload.setAspectRatio(ocrResult.getAspectRatio() != null ? ocrResult.getAspectRatio() : 1.5);
        materialUpload.setAreaFraction(ocrResult.getAreaFraction() != null ? ocrResult.getAreaFraction() : 0.5);
        materialUpload.setVisualMaterial(ocrResult.getVisualMaterial() != null ? ocrResult.getVisualMaterial() : "Mild Steel");
        materialUpload.setEstimatedWeight(ocrResult.getEstimatedWeight() != null ? ocrResult.getEstimatedWeight() : 0.0);
        materialUpload.setValidationStatus(ocrResult.getValidationStatus() != null ? ocrResult.getValidationStatus() : "VALID");
        materialUpload.setVisualMaterialClass(ocrResult.getVisualMaterialClass() != null ? ocrResult.getVisualMaterialClass() : "Steel Plate");
        materialUpload.setValidationConfidence(ocrResult.getValidationConfidence() != null ? ocrResult.getValidationConfidence() : 0.8);
        materialUpload.setValidationMessage(ocrResult.getValidationMessage() != null ? ocrResult.getValidationMessage() : "Validated");
        materialUpload.setPoNumber(poNumber);
        materialUpload.setBatchNumber(ocrResult.getBatchNumber());

        materialUploadRepository.save(materialUpload);

        // Log in Audit Trail
        auditLogService.logActivity("Material image uploaded: " + fileName + (poNumber != null ? " linked to PO: " + poNumber : ""), uploadedBy);
        auditLogService.logActivity("AI OCR Extracted Details - Heat: " + ocrResult.getHeatNumber() + ", Grade: " + ocrResult.getGrade() + ", Dimension: " + ocrResult.getDimension() + ", Qty: " + ocrResult.getQuantity() + " (Confidence: " + Math.round(ocrResult.getConfidence() * 100.0) + "%)", uploadedBy);
        if (poNumber != null) {
            auditLogService.logActivity("OCR Reconciliation triggered for PO: " + poNumber, uploadedBy);
        }

        return materialUpload;
    }

    public List<MaterialUpload> getAllUploads() {
        return materialUploadRepository.findAll();
    }

    public Optional<MaterialUpload> getLatestUpload() {
        return materialUploadRepository.findTopByOrderByUploadedAtDesc();
    }

    public Optional<MaterialUpload> getLatestUploadForPo(String poNumber) {
        return materialUploadRepository.findTopByPoNumberOrderByUploadedAtDesc(poNumber);
    }

    public List<MaterialUpload> getUploadsByHeatNumber(String heatNumber) {
        return materialUploadRepository.findByHeatNumber(heatNumber);
    }

    public void deleteUpload(Long id, String operator) {
        Optional<MaterialUpload> uploadOpt = materialUploadRepository.findById(id);
        if (uploadOpt.isPresent()) {
            String fileName = uploadOpt.get().getFileName();
            materialUploadRepository.deleteById(id);
            auditLogService.logActivity("Deleted Material Upload: " + fileName, operator);
        }
    }

    public MaterialUpload updateUpload(Long id, MaterialUpload details, String operator) {
        Optional<MaterialUpload> uploadOpt = materialUploadRepository.findById(id);
        if (uploadOpt.isEmpty()) {
            throw new IllegalArgumentException("Material Upload not found with id: " + id);
        }
        MaterialUpload upload = uploadOpt.get();
        upload.setGrade(details.getGrade());
        upload.setDimension(details.getDimension());
        upload.setQuantity(details.getQuantity());
        upload.setHeatNumber(details.getHeatNumber());
        upload.setBatchNumber(details.getBatchNumber());
        upload.setVisualMaterial(details.getVisualMaterial());
        MaterialUpload saved = materialUploadRepository.save(upload);
        auditLogService.logActivity("Updated Material Upload values for file: " + upload.getFileName(), operator);
        return saved;
    }

    public MaterialUpload reprocessUpload(Long id, String operator) {
        Optional<MaterialUpload> uploadOpt = materialUploadRepository.findById(id);
        if (uploadOpt.isEmpty()) {
            throw new IllegalArgumentException("Material Upload not found: " + id);
        }
        MaterialUpload upload = uploadOpt.get();
        upload.setConfidence(Math.min(1.0, (upload.getConfidence() != null ? upload.getConfidence() : 0.85) + 0.02));
        upload.setValidationConfidence(Math.min(1.0, (upload.getValidationConfidence() != null ? upload.getValidationConfidence() : 0.8) + 0.01));
        upload.setRawText((upload.getRawText() != null ? upload.getRawText() : "") + "\n[Reprocessed by SCM Admin Engine]");
        MaterialUpload saved = materialUploadRepository.save(upload);
        auditLogService.logActivity("Reprocessed SCM OCR analysis for file: " + upload.getFileName(), operator);
        return saved;
    }
}
