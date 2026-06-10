package com.dimetime.controller;

import com.dimetime.entity.Quotation;
import com.dimetime.service.QuotationService;
import com.dimetime.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*")
public class QuotationController {

    @Autowired
    private QuotationService quotationService;

    @PostMapping
    public ResponseEntity<?> submitQuotation(@RequestBody Quotation quotation) {
        try {
            Quotation result = quotationService.submitQuotation(quotation);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/rfq/{rfqNumber}")
    public ResponseEntity<List<Quotation>> getByRfq(@PathVariable String rfqNumber) {
        return ResponseEntity.ok(quotationService.getQuotationsForRfq(rfqNumber));
    }

    @GetMapping("/manufacturer/{username}")
    public ResponseEntity<List<Quotation>> getByManufacturer(@PathVariable String username) {
        return ResponseEntity.ok(quotationService.getQuotationsByManufacturer(username));
    }

    @PutMapping("/{id}/select")
    public ResponseEntity<?> selectQuotation(@PathVariable Long id, @RequestParam("operator") String operator) {
        try {
            Quotation result = quotationService.selectQuotation(id, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Quotation>> getAll() {
        return ResponseEntity.ok(quotationService.getAllQuotations());
    }
}
