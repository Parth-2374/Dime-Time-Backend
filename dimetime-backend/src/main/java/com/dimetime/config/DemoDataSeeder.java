package com.dimetime.config;

import com.dimetime.entity.Company;
import com.dimetime.entity.PurchaseOrder;
import com.dimetime.entity.User;
import com.dimetime.repository.CompanyRepository;
import com.dimetime.repository.PurchaseOrderRepository;
import com.dimetime.repository.UserRepository;
import com.dimetime.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting DimeTime Demo Data Seeder...");

        // Seed Companies
        Company supplierCompany = companyRepository.findByName("Test Supplier Corp").orElseGet(() -> {
            Company c = new Company("Test Supplier Corp", "GST-SUPPLIER123", "Supplier Street 1", "Supplier Contact", "supplier@test.com", "9876543210", "SUPPLIER");
            return companyRepository.save(c);
        });

        Company manufacturerCompany = companyRepository.findByName("Test Manufacturer Corp").orElseGet(() -> {
            Company c = new Company("Test Manufacturer Corp", "GST-MANUFACTURER123", "Manufacturer Avenue 2", "Manufacturer Contact", "manufacturer@test.com", "9876543211", "MANUFACTURER");
            return companyRepository.save(c);
        });

        Company adminCompany = companyRepository.findByName("DimeTime Logistics Ltd").orElseGet(() -> {
            Company c = new Company("DimeTime Logistics Ltd", "GST-ADMIN123", "Admin Blvd 3", "Admin Contact", "admin@test.com", "9876543212", "ADMIN");
            return companyRepository.save(c);
        });

        // Seed Supplier User
        User supplier = userRepository.findByUsername("supplier").orElse(null);
        if (supplier == null) {
            supplier = new User(
                    "Test Supplier User",
                    "supplier@test.com",
                    "+15550198",
                    "Test Supplier Corp",
                    "supplier",
                    passwordEncoder.encode("Supplier123"),
                    "SUPPLIER"
            );
            supplier.setCompany(supplierCompany);
            userRepository.save(supplier);
            System.out.println("Seeded Default Supplier User: supplier / Supplier123");
        } else {
            supplier.setPassword(passwordEncoder.encode("Supplier123"));
            supplier.setCompany(supplierCompany);
            userRepository.save(supplier);
            System.out.println("Updated Seeded Supplier User password to Supplier123");
        }

        // Seed Manufacturer User
        User manufacturer = userRepository.findByUsername("manufacturer").orElse(null);
        if (manufacturer == null) {
            manufacturer = new User(
                    "Test Manufacturer User",
                    "manufacturer@test.com",
                    "+15550197",
                    "Test Manufacturer Corp",
                    "manufacturer",
                    passwordEncoder.encode("Manufacturer123"),
                    "MANUFACTURER"
            );
            manufacturer.setCompany(manufacturerCompany);
            userRepository.save(manufacturer);
            System.out.println("Seeded Default Manufacturer User: manufacturer / Manufacturer123");
        } else {
            manufacturer.setPassword(passwordEncoder.encode("Manufacturer123"));
            manufacturer.setCompany(manufacturerCompany);
            userRepository.save(manufacturer);
            System.out.println("Updated Seeded Manufacturer User password to Manufacturer123");
        }

        // Seed Default Admin User
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "Admin Operator",
                    "admin@test.com",
                    "+15550199",
                    "DimeTime Logistics Ltd",
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ADMIN"
            );
            admin.setCompany(adminCompany);
            userRepository.save(admin);
            System.out.println("Seeded Default Admin User: admin / admin123");
            auditLogService.logActivity("Database seeded with default Admin user", "SYSTEM");
        } else {
            userRepository.findByUsername("admin").ifPresent(admin -> {
                admin.setEmail("admin@test.com"); // Ensure exact email matching request
                if (admin.getPassword() != null && !admin.getPassword().startsWith("$2a$")) {
                    admin.setPassword(passwordEncoder.encode(admin.getPassword()));
                }
                userRepository.save(admin);
                System.out.println("Updated existing admin user with email admin@test.com.");
            });
        }

        // 2. Seed Default POs
        if (poRepository.findByPoNumber("PO-2026-001").isEmpty()) {
            PurchaseOrder po1 = new PurchaseOrder(
                    "PO-2026-001",
                    "Test Supplier Corp",
                    "SS Round Bar",
                    "316L",
                    "25 MM",
                    "500 KG"
            );
            po1.setSupplierCompany(supplierCompany);
            po1.setManufacturerCompany(manufacturerCompany);
            userRepository.findByUsername("supplier").ifPresent(po1::setSupplierUser);
            userRepository.findByUsername("manufacturer").ifPresent(po1::setManufacturerUser);
            poRepository.save(po1);
            System.out.println("Seeded Purchase Order: PO-2026-001");
        }

        if (poRepository.findByPoNumber("PO-2026-029").isEmpty()) {
            PurchaseOrder po29 = new PurchaseOrder(
                    "PO-2026-029",
                    "Test Supplier Corp",
                    "SS Round Bar",
                    "316L",
                    "25 MM",
                    "500 KG"
            );
            po29.setSupplierCompany(supplierCompany);
            po29.setManufacturerCompany(manufacturerCompany);
            userRepository.findByUsername("supplier").ifPresent(po29::setSupplierUser);
            userRepository.findByUsername("manufacturer").ifPresent(po29::setManufacturerUser);
            poRepository.save(po29);
            System.out.println("Seeded Purchase Order: PO-2026-029");
        }

        System.out.println("DimeTime Demo Data Seeding completed successfully.");
    }
}
