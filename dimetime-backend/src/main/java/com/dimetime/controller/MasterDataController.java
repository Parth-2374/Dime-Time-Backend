package com.dimetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/master-data")
@CrossOrigin(origins = "*")
public class MasterDataController {

    private final Map<String, List<Map<String, Object>>> dataStore = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> idCounters = new ConcurrentHashMap<>();

    public MasterDataController() {
        // Initialize ID counters
        idCounters.put("grades", new AtomicLong(1));
        idCounters.put("categories", new AtomicLong(1));
        idCounters.put("chemical", new AtomicLong(1));
        idCounters.put("mechanical", new AtomicLong(1));
        idCounters.put("dimensions", new AtomicLong(1));

        // Seed Material Grades
        List<Map<String, Object>> grades = new ArrayList<>();
        grades.add(createItem("grades", "SS304", "Stainless Steel 304 - Austenitic Chromium-Nickel alloy", "Active"));
        grades.add(createItem("grades", "SS316", "Stainless Steel 316 - High corrosion resistance molybdenum grade", "Active"));
        grades.add(createItem("grades", "ASTM A36", "Carbon Structural Steel - General purpose weldable grade", "Active"));
        grades.add(createItem("grades", "ASTM A516 Grade 70", "Pressure Vessel Carbon Steel plate for moderate/lower service", "Active"));
        dataStore.put("grades", grades);

        // Seed Material Categories
        List<Map<String, Object>> categories = new ArrayList<>();
        categories.add(createItem("categories", "Steel Plates", "Heavy-gauge hot-rolled structural steel plates", "Active"));
        categories.add(createItem("categories", "Metal Sheets", "Thin cold-rolled sheet metal", "Active"));
        categories.add(createItem("categories", "Pipes & Tubes", "Hollow structural steel sections", "Active"));
        categories.add(createItem("categories", "Carbon Bars", "Round, square, or hex solid steel bar stock", "Active"));
        dataStore.put("categories", categories);

        // Seed Chemical Standards
        List<Map<String, Object>> chemical = new ArrayList<>();
        chemical.add(createItem("chemical", "ASTM A240", "Standard spec for Cr & Ni stainless steel plate, sheet, strip", "Active"));
        chemical.add(createItem("chemical", "EN 10028-7", "Flat products made of steels for pressure purposes - Stainless steels", "Active"));
        chemical.add(createItem("chemical", "ASME SA-240", "Identical with ASTM A240 boiler & pressure vessel code", "Active"));
        dataStore.put("chemical", chemical);

        // Seed Mechanical Standards
        List<Map<String, Object>> mechanical = new ArrayList<>();
        mechanical.add(createItem("mechanical", "ASTM A370", "Standard test methods and definitions for mechanical testing", "Active"));
        mechanical.add(createItem("mechanical", "EN 10002", "Metallic materials - Tensile testing verification standards", "Active"));
        mechanical.add(createItem("mechanical", "JIS Z2241", "Method of tensile testing for metallic materials", "Active"));
        dataStore.put("mechanical", mechanical);

        // Seed Dimension Templates
        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(createItem("dimensions", "10 x 1500 x 6000 mm", "Standard Medium Plate dimensions", "Active"));
        dimensions.add(createItem("dimensions", "2 x 1220 x 2440 mm", "Standard Thin Sheet dimensions (4x8 ft)", "Active"));
        dimensions.add(createItem("dimensions", "50 x 2000 x 6000 mm", "Heavy Steel Slab Template", "Active"));
        dimensions.add(createItem("dimensions", "25 x 1500 x 3000 mm", "Thick structural plate template", "Active"));
        dataStore.put("dimensions", dimensions);
    }

    private Map<String, Object> createItem(String category, String name, String description, String status) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", idCounters.get(category).getAndIncrement());
        item.put("name", name);
        item.put("description", description);
        item.put("status", status);
        item.put("updatedAt", LocalDateTime.now().toString());
        return item;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getAllData() {
        return ResponseEntity.ok(dataStore);
    }

    @GetMapping("/{category}")
    public ResponseEntity<List<Map<String, Object>>> getCategoryData(@PathVariable("category") String category) {
        List<Map<String, Object>> data = dataStore.get(category.toLowerCase());
        if (data == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(data);
    }

    @PostMapping("/{category}")
    public ResponseEntity<?> createItem(@PathVariable("category") String category, @RequestBody Map<String, String> payload) {
        String catKey = category.toLowerCase();
        if (!dataStore.containsKey(catKey)) {
            return ResponseEntity.badRequest().body("Category not found: " + category);
        }
        String name = payload.get("name");
        String description = payload.get("description");
        String status = payload.getOrDefault("status", "Active");

        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }

        Map<String, Object> newItem = createItem(catKey, name, description, status);
        dataStore.get(catKey).add(newItem);
        return ResponseEntity.ok(newItem);
    }

    @PutMapping("/{category}/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable("category") String category,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> payload) {
        String catKey = category.toLowerCase();
        List<Map<String, Object>> data = dataStore.get(catKey);
        if (data == null) {
            return ResponseEntity.badRequest().body("Category not found");
        }

        for (Map<String, Object> item : data) {
            if (id.equals(item.get("id"))) {
                if (payload.containsKey("name")) {
                    item.put("name", payload.get("name"));
                }
                if (payload.containsKey("description")) {
                    item.put("description", payload.get("description"));
                }
                if (payload.containsKey("status")) {
                    item.put("status", payload.get("status"));
                }
                item.put("updatedAt", LocalDateTime.now().toString());
                return ResponseEntity.ok(item);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{category}/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable("category") String category, @PathVariable("id") Long id) {
        String catKey = category.toLowerCase();
        List<Map<String, Object>> data = dataStore.get(catKey);
        if (data == null) {
            return ResponseEntity.badRequest().body("Category not found");
        }

        boolean removed = data.removeIf(item -> id.equals(item.get("id")));
        if (removed) {
            return ResponseEntity.ok("Item deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}
