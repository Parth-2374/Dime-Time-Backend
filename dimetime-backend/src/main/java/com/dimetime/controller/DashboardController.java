package com.dimetime.controller;

import com.dimetime.dto.DashboardStatsDto;
import com.dimetime.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/supplier/stats")
    public ResponseEntity<Map<String, Object>> getSupplierStats(@RequestParam("username") String username) {
        return ResponseEntity.ok(dashboardService.getSupplierStats(username));
    }

    @GetMapping("/manufacturer/stats")
    public ResponseEntity<Map<String, Object>> getManufacturerStats(@RequestParam("username") String username) {
        return ResponseEntity.ok(dashboardService.getManufacturerStats(username));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }
}
