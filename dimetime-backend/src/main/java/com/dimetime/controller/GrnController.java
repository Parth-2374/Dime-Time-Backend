package com.dimetime.controller;

import com.dimetime.entity.Grn;
import com.dimetime.service.GrnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grns")
@CrossOrigin(origins = "*")
public class GrnController {

    @Autowired
    private GrnService grnService;

    @PostMapping
    public ResponseEntity<?> createGrn(
            @RequestParam("poNumber") String poNumber,
            @RequestParam("generatedBy") String generatedBy) {
        try {
            Grn result = grnService.generateGrn(poNumber, generatedBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Grn>> getAll() {
        return ResponseEntity.ok(grnService.getAllGrns());
    }

    @GetMapping("/{grnNumber}")
    public ResponseEntity<?> getByNumber(@PathVariable String grnNumber) {
        return grnService.getGrnByNumber(grnNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/manual")
    public ResponseEntity<?> createManualGrn(
            @RequestBody Grn grn,
            @RequestParam("operator") String operator) {
        try {
            Grn result = grnService.createManualGrn(grn, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGrn(
            @PathVariable Long id,
            @RequestBody Grn details,
            @RequestParam("operator") String operator) {
        try {
            Grn result = grnService.updateGrn(id, details, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = grnService.generateGrnPdf(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "GRN-Certificate-" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGrn(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            grnService.deleteGrn(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"GRN deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
