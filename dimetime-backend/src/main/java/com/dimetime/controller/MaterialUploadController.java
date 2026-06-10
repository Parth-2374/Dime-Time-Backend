package com.dimetime.controller;

import com.dimetime.entity.MaterialUpload;
import com.dimetime.service.MaterialUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/material-uploads")
@CrossOrigin(origins = "*")
public class MaterialUploadController {

    @Autowired
    private MaterialUploadService uploadService;

    @PostMapping
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "poNumber", required = false) String poNumber) {
        try {
            MaterialUpload result = uploadService.processMaterialUpload(file, uploadedBy, poNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<MaterialUpload>> getAll() {
        return ResponseEntity.ok(uploadService.getAllUploads());
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        return uploadService.getLatestUpload()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/po/{poNumber}")
    public ResponseEntity<?> getLatestForPo(@PathVariable String poNumber) {
        return uploadService.getLatestUploadForPo(poNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUpload(
            @PathVariable Long id,
            @RequestBody MaterialUpload details,
            @RequestParam("operator") String operator) {
        try {
            MaterialUpload result = uploadService.updateUpload(id, details, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<?> reprocessUpload(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            MaterialUpload result = uploadService.reprocessUpload(id, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUpload(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            uploadService.deleteUpload(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"Material Upload deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
