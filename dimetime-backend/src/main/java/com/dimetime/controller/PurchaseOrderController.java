package com.dimetime.controller;

import com.dimetime.entity.PurchaseOrder;
import com.dimetime.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService poService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllOrders() {
        return ResponseEntity.ok(poService.getAllPurchaseOrders());
    }

    @GetMapping("/{poNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String poNumber) {
        return poService.getPurchaseOrder(poNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supplier/{username}")
    public ResponseEntity<List<PurchaseOrder>> getPOsBySupplier(@PathVariable String username) {
        return ResponseEntity.ok(poService.getPOsBySupplier(username));
    }

    @GetMapping("/manufacturer/{username}")
    public ResponseEntity<List<PurchaseOrder>> getPOsByManufacturer(@PathVariable String username) {
        return ResponseEntity.ok(poService.getPOsByManufacturer(username));
    }

    @GetMapping("/supplier/id/{id}")
    public ResponseEntity<List<PurchaseOrder>> getPOsBySupplierId(@PathVariable Long id) {
        return ResponseEntity.ok(poService.getPOsBySupplierId(id));
    }

    @GetMapping("/manufacturer/id/{id}")
    public ResponseEntity<List<PurchaseOrder>> getPOsByManufacturerId(@PathVariable Long id) {
        return ResponseEntity.ok(poService.getPOsByManufacturerId(id));
    }

    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(@RequestBody PurchaseOrder po) {
        try {
            PurchaseOrder result = poService.createPurchaseOrder(po);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{poNumber}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String poNumber,
            @RequestParam("status") String status,
            @RequestParam("operator") String operator) {
        try {
            PurchaseOrder result = poService.updateStatus(poNumber, status, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{poNumber}/dispatch")
    public ResponseEntity<?> updateDispatch(
            @PathVariable String poNumber,
            @RequestParam("carrier") String carrier,
            @RequestParam("trackingNumber") String trackingNumber,
            @RequestParam("operator") String operator) {
        try {
            PurchaseOrder result = poService.updateDispatch(poNumber, carrier, trackingNumber, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/{poNumber}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPurchaseOrderQr(@PathVariable String poNumber) {
        try {
            byte[] qrBytes = poService.generateQrCode(poNumber);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .body(qrBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping(value = "/{poNumber}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPurchaseOrderPdf(@PathVariable String poNumber) {
        try {
            byte[] pdfBytes = poService.generatePurchaseOrderPdf(poNumber);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + poNumber + ".pdf")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrder po,
            @RequestParam("operator") String operator) {
        try {
            PurchaseOrder result = poService.updatePurchaseOrder(id, po, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePurchaseOrder(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            poService.deletePurchaseOrder(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"Purchase Order deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
