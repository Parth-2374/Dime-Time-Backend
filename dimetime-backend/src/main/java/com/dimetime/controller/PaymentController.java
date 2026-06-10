package com.dimetime.controller;

import com.dimetime.entity.Payment;
import com.dimetime.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> payload) {
        try {
            String invoiceNumber = (String) payload.get("invoiceNumber");
            String paymentMethod = (String) payload.get("paymentMethod");
            Double amount = Double.valueOf(payload.get("amount").toString());
            String operator = (String) payload.get("operator");

            Payment payment = paymentService.initiatePayment(invoiceNumber, paymentMethod, amount, operator);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completePayment(@RequestBody Map<String, Object> payload) {
        try {
            String paymentNumber = (String) payload.get("paymentNumber");
            String transactionReference = (String) payload.get("transactionReference");
            String operator = (String) payload.get("operator");

            Payment payment = paymentService.completePayment(paymentNumber, transactionReference, operator);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/fail")
    public ResponseEntity<?> failPayment(@RequestBody Map<String, Object> payload) {
        try {
            String paymentNumber = (String) payload.get("paymentNumber");
            String operator = (String) payload.get("operator");

            Payment payment = paymentService.failPayment(paymentNumber, operator);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
