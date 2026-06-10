package com.dimetime.controller;

import com.dimetime.entity.Rfq;
import com.dimetime.service.RfqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rfqs")
@CrossOrigin(origins = "*")
public class RfqController {

    @Autowired
    private RfqService rfqService;

    @PostMapping
    public ResponseEntity<?> createRfq(@RequestBody Rfq rfq) {
        try {
            Rfq result = rfqService.createRfq(rfq);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Rfq>> getAll() {
        return ResponseEntity.ok(rfqService.getAllRfqs());
    }

    @GetMapping("/{rfqNumber}")
    public ResponseEntity<?> getByNumber(@PathVariable String rfqNumber) {
        return rfqService.getRfqByNumber(rfqNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/creator/{username}")
    public ResponseEntity<List<Rfq>> getByCreator(@PathVariable String username) {
        return ResponseEntity.ok(rfqService.getRfqsByCreator(username));
    }

    @PutMapping("/{rfqNumber}/cancel")
    public ResponseEntity<?> cancelRfq(@PathVariable String rfqNumber, @RequestParam("operator") String operator) {
        try {
            Rfq result = rfqService.updateStatus(rfqNumber, "CANCELLED", operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{rfqNumber}/broadcast")
    public ResponseEntity<?> broadcastRfq(@PathVariable String rfqNumber, @RequestParam("operator") String operator) {
        try {
            Rfq result = rfqService.broadcastRfq(rfqNumber, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/assigned/{manufacturer}")
    public ResponseEntity<List<Rfq>> getAssignedRfqs(@PathVariable String manufacturer) {
        return ResponseEntity.ok(rfqService.getAssignedRfqs(manufacturer));
    }
}

