package com.dimetime.controller;

import com.dimetime.entity.Feature;
import com.dimetime.repository.FeatureRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/features")
@CrossOrigin(origins = "*")
public class FeatureController {

    @Autowired
    private FeatureRepository featureRepository;

    @PostConstruct
    public void init() {
        if (featureRepository.count() == 0) {
            featureRepository.save(new Feature("Master Templates", "AdminPanel", "ENABLED", "admin"));
            featureRepository.save(new Feature("Supplier Performance", "Performance", "ENABLED", "admin"));
            featureRepository.save(new Feature("Manufacturer Performance", "Performance", "ENABLED", "admin"));
            featureRepository.save(new Feature("Reports Center", "Reports", "ENABLED", "admin"));
            featureRepository.save(new Feature("System Settings", "Settings", "ENABLED", "admin"));
            featureRepository.save(new Feature("Feature Requests", "Settings", "ENABLED", "admin"));
            featureRepository.save(new Feature("Plate Calculator", "Calculator", "ENABLED", "admin"));
            featureRepository.save(new Feature("GRN Registry", "GRN", "ENABLED", "admin"));
            featureRepository.save(new Feature("MTC Verification", "MTC", "ENABLED", "admin"));
            featureRepository.save(new Feature("Match Analytics", "Verification", "ENABLED", "admin"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Feature>> getAllFeatures() {
        return ResponseEntity.ok(featureRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createFeature(@RequestBody Feature feature) {
        try {
            if (featureRepository.findByName(feature.getName()).isPresent()) {
                return ResponseEntity.badRequest().body("Feature name already exists.");
            }
            Feature saved = featureRepository.save(feature);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeature(@PathVariable("id") Long id, @RequestBody Feature details) {
        try {
            Feature feature = featureRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Feature not found: " + id));
            feature.setName(details.getName());
            feature.setModule(details.getModule());
            feature.setStatus(details.getStatus());
            Feature saved = featureRepository.save(feature);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeature(@PathVariable("id") Long id) {
        try {
            featureRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
