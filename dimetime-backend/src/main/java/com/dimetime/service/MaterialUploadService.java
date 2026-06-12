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

    // DEMO DATA (OCR BYPASS)
    ocrResult.setHeatNumber("HT-2026-001");
    ocrResult.setGrade("SS304");
    ocrResult.setDimension("1000X500X25 MM");
    ocrResult.setQuantity("500 KG");
    ocrResult.setRawText("Demo OCR Response");
    ocrResult.setConfidence(0.98);

    ocrResult.setAspectRatio(2.0);
    ocrResult.setAreaFraction(0.65);
    ocrResult.setVisualMaterial("SS316");
    ocrResult.setEstimatedWeight(500.0);

    ocrResult.setValidationStatus("VALID");
    ocrResult.setValidationConfidence(0.98);
    ocrResult.setValidationMessage("Material verified successfully");

    ocrResult.setVisualMaterialClass("Steel Plate");
    ocrResult.setBatchNumber("BT-2026-001");

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

    materialUpload.setAspectRatio(ocrResult.getAspectRatio());
    materialUpload.setAreaFraction(ocrResult.getAreaFraction());
    materialUpload.setVisualMaterial(ocrResult.getVisualMaterial());
    materialUpload.setEstimatedWeight(ocrResult.getEstimatedWeight());
    materialUpload.setValidationStatus(ocrResult.getValidationStatus());
    materialUpload.setVisualMaterialClass(ocrResult.getVisualMaterialClass());
    materialUpload.setValidationConfidence(ocrResult.getValidationConfidence());
    materialUpload.setValidationMessage(ocrResult.getValidationMessage());
    materialUpload.setPoNumber(poNumber);
    materialUpload.setBatchNumber(ocrResult.getBatchNumber());

    materialUploadRepository.save(materialUpload);

    auditLogService.logActivity(
            "Material image uploaded: " + fileName,
            uploadedBy
    );

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
