package com.dimetime.service;

import com.dimetime.entity.Company;
import com.dimetime.entity.PlantLocation;
import com.dimetime.repository.CompanyRepository;
import com.dimetime.repository.PlantLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PlantLocationRepository plantLocationRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Optional<Company> getCompanyByName(String name) {
        return companyRepository.findByName(name);
    }

    @Transactional
    public Company createCompany(Company company) {
        if (companyRepository.existsByName(company.getName())) {
            throw new IllegalArgumentException("Company name already exists");
        }
        if (companyRepository.existsByGstNumber(company.getGstNumber())) {
            throw new IllegalArgumentException("GST Number already registered");
        }
        Company saved = companyRepository.save(company);
        auditLogService.logActivity("Registered Company profile: " + company.getName(), "SYSTEM");
        return saved;
    }

    public List<PlantLocation> getPlantsByCompanyId(Long companyId) {
        return plantLocationRepository.findByCompanyId(companyId);
    }

    @Transactional
    public PlantLocation addPlantLocation(Long companyId, String name, String address) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        PlantLocation plant = new PlantLocation(companyOpt.get(), name, address);
        PlantLocation saved = plantLocationRepository.save(plant);
        auditLogService.logActivity("Added Plant Location '" + name + "' for Company ID " + companyId, "SYSTEM");
        return saved;
    }
}
