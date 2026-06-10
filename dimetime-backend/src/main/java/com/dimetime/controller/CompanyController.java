package com.dimetime.controller;

import com.dimetime.entity.Company;
import com.dimetime.entity.PlantLocation;
import com.dimetime.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<Company>> getAll() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return companyService.getCompanyById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Company company) {
        try {
            Company result = companyService.createCompany(company);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/plants")
    public ResponseEntity<List<PlantLocation>> getPlants(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getPlantsByCompanyId(id));
    }

    @PostMapping("/{id}/plants")
    public ResponseEntity<?> addPlant(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("address") String address) {
        try {
            PlantLocation result = companyService.addPlantLocation(id, name, address);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
