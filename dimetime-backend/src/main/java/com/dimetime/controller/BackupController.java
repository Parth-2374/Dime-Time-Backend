package com.dimetime.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/backups")
@CrossOrigin(origins = "*")
public class BackupController {

    private static final String BACKUP_DIR = "backups";
    private final List<Map<String, Object>> mockBackups = new ArrayList<>();

    public BackupController() {
        // Initialize with a few mock historical backups
        Map<String, Object> b1 = new HashMap<>();
        b1.put("filename", "dimetime_backup_20260501.json");
        b1.put("createdAt", "2026-05-01T12:00:00");
        b1.put("size", "1.2 MB");
        b1.put("status", "SUCCESS");
        b1.put("recordCount", 148);
        mockBackups.add(b1);

        Map<String, Object> b2 = new HashMap<>();
        b2.put("filename", "dimetime_backup_20260601.json");
        b2.put("createdAt", "2026-06-01T15:30:00");
        b2.put("size", "1.5 MB");
        b2.put("status", "SUCCESS");
        b2.put("recordCount", 185);
        mockBackups.add(b2);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBackups() {
        return ResponseEntity.ok(mockBackups);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBackup(@RequestParam(value = "operator", defaultValue = "Admin") String operator) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "dimetime_backup_" + timestamp + ".json";
            
            Map<String, Object> backup = new HashMap<>();
            backup.put("filename", filename);
            backup.put("createdAt", LocalDateTime.now().toString());
            backup.put("size", "1.8 MB");
            backup.put("status", "SUCCESS");
            backup.put("recordCount", 215);
            backup.put("createdBy", operator);
            
            mockBackups.add(0, backup); // Add to beginning
            
            return ResponseEntity.ok(backup);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable("filename") String filename) {
        try {
            // Return dummy json backup content
            String jsonContent = "{\n" +
                    "  \"system\": \"DimeTime SCM\",\n" +
                    "  \"version\": \"Enterprise 2.0\",\n" +
                    "  \"backupFile\": \"" + filename + "\",\n" +
                    "  \"exportedAt\": \"" + LocalDateTime.now() + "\",\n" +
                    "  \"entities\": {\n" +
                    "    \"users\": 15,\n" +
                    "    \"purchaseOrders\": 34,\n" +
                    "    \"rfqs\": 12,\n" +
                    "    \"grns\": 8\n" +
                    "  }\n" +
                    "}";
            
            byte[] fileBytes = jsonContent.getBytes();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/restore/{filename}")
    public ResponseEntity<?> restoreBackup(
            @PathVariable("filename") String filename,
            @RequestParam(value = "operator", defaultValue = "Admin") String operator) {
        try {
            boolean found = false;
            for (Map<String, Object> b : mockBackups) {
                if (filename.equals(b.get("filename"))) {
                    found = true;
                    b.put("status", "RESTORED");
                    break;
                }
            }
            if (!found) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Backup file not found: " + filename);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "System restored to state of " + filename);
            response.put("restoredAt", LocalDateTime.now().toString());
            response.put("restoredBy", operator);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
