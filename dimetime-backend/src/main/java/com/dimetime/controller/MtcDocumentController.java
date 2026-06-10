package com.dimetime.controller;

import com.dimetime.entity.MtcDocument;
import com.dimetime.service.MtcDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/mtc-documents")
@CrossOrigin(origins = "*")
public class MtcDocumentController {

    @Autowired
    private MtcDocumentService mtcService;

    @PostMapping
    public ResponseEntity<?> uploadMtc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "poNumber", required = false) String poNumber) {
        try {
            MtcDocument result = mtcService.processMtcUpload(file, uploadedBy, poNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<MtcDocument>> getAll(@RequestParam(value = "poNumber", required = false) String poNumber) {
        if (poNumber != null && !poNumber.trim().isEmpty()) {
            return ResponseEntity.ok(mtcService.getDocumentsForPo(poNumber));
        }
        return ResponseEntity.ok(mtcService.getAllDocuments());
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        return mtcService.getLatestDocument()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/ocr-upload")
    public ResponseEntity<?> uploadMtcOcrNew(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("poNumber") String poNumber) {
        try {
            Object result = mtcService.processMtcOcrUpload(file, uploadedBy, poNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateMtcManual(
            @RequestParam("poNumber") String poNumber,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestBody com.dimetime.dto.MtcResponseDto mtcResult) {
        try {
            MtcDocument result = mtcService.generateAndDeliverMtc(
                    mtcResult,
                    uploadedBy,
                    poNumber,
                    mtcResult.getOcrMatchScore() != null ? mtcResult.getOcrMatchScore() : 100.0,
                    mtcResult.getValidationResult() != null ? mtcResult.getValidationResult() : "APPROVED"
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam("operator") String operator,
            @RequestParam(value = "comment", required = false) String comment) {
        try {
            MtcDocument result = mtcService.updateCertificateStatus(id, status, operator, comment);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Long id) {
        try {
            byte[] pdfBytes = mtcService.generateMtcPdf(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "MTC-Certificate-" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody MtcDocument details,
            @RequestParam("operator") String operator) {
        try {
            MtcDocument result = mtcService.updateDocument(id, details, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            mtcService.deleteDocument(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"MTC Certificate deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
