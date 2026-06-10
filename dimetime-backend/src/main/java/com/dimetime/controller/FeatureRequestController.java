package com.dimetime.controller;

import com.dimetime.entity.FeatureRequest;
import com.dimetime.repository.FeatureRequestRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/feature-requests")
@CrossOrigin(origins = "*")
public class FeatureRequestController {

    @Autowired
    private FeatureRequestRepository requestRepository;

    @PostConstruct
    public void init() {
        if (requestRepository.count() == 0) {
            requestRepository.save(new FeatureRequest("supplier", "HIGH", "Integrate chemical scanner layout OCR for automated standard matching", "PENDING"));
            requestRepository.save(new FeatureRequest("manufacturer", "MEDIUM", "Add mechanical test reports PDF generator utility", "IN_REVIEW"));
            requestRepository.save(new FeatureRequest("admin", "LOW", "Real-time dispatch transit status maps tracker Integration", "APPROVED"));
        }
    }

    @GetMapping
    public ResponseEntity<List<FeatureRequest>> getAllRequests() {
        return ResponseEntity.ok(requestRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody FeatureRequest request) {
        try {
            FeatureRequest saved = requestRepository.save(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable("id") Long id, @RequestBody FeatureRequest details) {
        try {
            FeatureRequest request = requestRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
            request.setPriority(details.getPriority());
            request.setDescription(details.getDescription());
            request.setStatus(details.getStatus());
            FeatureRequest saved = requestRepository.save(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable("id") Long id) {
        try {
            requestRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
