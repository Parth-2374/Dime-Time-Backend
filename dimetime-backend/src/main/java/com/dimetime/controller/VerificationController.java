package com.dimetime.controller;

import com.dimetime.entity.VerificationResult;
import com.dimetime.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/verification")
@CrossOrigin(origins = "*")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @PostMapping
    public ResponseEntity<?> verify(
            @RequestParam("poNumber") String poNumber,
            @RequestParam("verifiedBy") String verifiedBy) {
        try {
            VerificationResult result = verificationService.performVerification(poNumber, verifiedBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VerificationResult>> getAll() {
        return ResponseEntity.ok(verificationService.getAllVerifications());
    }

    @GetMapping("/po/{poNumber}")
    public ResponseEntity<List<VerificationResult>> getByPo(@PathVariable String poNumber) {
        return ResponseEntity.ok(verificationService.getVerificationsForPo(poNumber));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVerification(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            verificationService.deleteVerificationResult(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"Verification result deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
