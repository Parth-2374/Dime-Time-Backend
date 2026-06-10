package com.dimetime.controller;

import com.dimetime.entity.AuditLog;
import com.dimetime.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @PostMapping
    public ResponseEntity<?> createLog(
            @RequestParam("activity") String activity,
            @RequestParam("username") String username,
            @RequestParam(value = "role", defaultValue = "ADMIN") String role,
            @RequestParam(value = "ipAddress", defaultValue = "127.0.0.1") String ipAddress) {
        try {
            auditLogService.logSecurityActivity(activity, username, role, ipAddress);
            return ResponseEntity.ok().body("{\"message\": \"Audit Log created successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLog(@PathVariable Long id) {
        try {
            auditLogService.deleteLog(id);
            return ResponseEntity.ok().body("{\"message\": \"Audit Log deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
