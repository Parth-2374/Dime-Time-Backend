package com.dimetime.controller;

import com.dimetime.entity.PlateCalculation;
import com.dimetime.service.PlateCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/plate-calculations")
@CrossOrigin(origins = "*")
public class PlateCalculationController {

    @Autowired
    private PlateCalculationService calculationService;

    @PostMapping
    public ResponseEntity<?> calculatePlate(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("material") String material,
            @RequestParam("length") Double length,
            @RequestParam("width") Double width,
            @RequestParam("thickness") Double thickness,
            @RequestParam(value = "confidence", required = false) Double confidence,
            @RequestParam("calculatedBy") String calculatedBy,
            @RequestParam(value = "validationStatus", required = false, defaultValue = "VALID") String validationStatus,
            @RequestParam(value = "inferredDimensions", required = false) String inferredDimensions,
            @RequestParam(value = "validationConfidence", required = false) Double validationConfidence,
            @RequestParam(value = "materialClass", required = false, defaultValue = "Steel Plate") String materialClass,
            @RequestParam(value = "manualOverride", required = false, defaultValue = "false") Boolean manualOverride) {
        try {
            PlateCalculation result = calculationService.performCalculation(
                    file, material, length, width, thickness, confidence, calculatedBy,
                    validationStatus, inferredDimensions, validationConfidence, materialClass, manualOverride
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PlateCalculation>> getHistory() {
        return ResponseEntity.ok(calculationService.getHistory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCalculation(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(calculationService.getCalculation(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCalculation(@PathVariable("id") Long id, @RequestBody PlateCalculation details) {
        try {
            return ResponseEntity.ok(calculationService.updateCalculation(id, details));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCalculation(@PathVariable("id") Long id) {
        try {
            calculationService.deleteCalculation(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
