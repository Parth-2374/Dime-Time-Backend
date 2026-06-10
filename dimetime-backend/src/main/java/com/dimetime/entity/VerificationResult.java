package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_results")
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false)
    private String poNumber;

    @Column(name = "heat_number")
    private String heatNumber;

    @Column(name = "grade")
    private String grade;

    @Column(name = "dimension")
    private String dimension;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "heat_number_match")
    private Boolean heatNumberMatch;

    @Column(name = "grade_match")
    private Boolean gradeMatch;

    @Column(name = "dimension_match")
    private Boolean dimensionMatch;

    @Column(name = "quantity_match")
    private Boolean quantityMatch;

    @Column(name = "batch_number_match")
    private Boolean batchNumberMatch = true;

    @Column(name = "product_spec_match")
    private Boolean productSpecMatch = true;

    @Column(name = "match_percentage")
    private Double matchPercentage;

    @Column(nullable = false)
    private String status; // APPROVED, REVIEW_REQUIRED, or REJECTED

    @Column(name = "verified_at", nullable = false, updatable = false)
    private LocalDateTime verifiedAt = LocalDateTime.now();

    @Column(name = "verified_by")
    private String verifiedBy;

    public VerificationResult() {
    }

    public VerificationResult(String poNumber, String heatNumber, String grade, String dimension, String quantity, Boolean heatNumberMatch, Boolean gradeMatch, Boolean dimensionMatch, Boolean quantityMatch, Double matchPercentage, String status, String verifiedBy) {
        this.poNumber = poNumber;
        this.heatNumber = heatNumber;
        this.grade = grade;
        this.dimension = dimension;
        this.quantity = quantity;
        this.heatNumberMatch = heatNumberMatch;
        this.gradeMatch = gradeMatch;
        this.dimensionMatch = dimensionMatch;
        this.quantityMatch = quantityMatch;
        this.matchPercentage = matchPercentage;
        this.status = status;
        this.verifiedBy = verifiedBy;
        this.batchNumberMatch = true;
        this.productSpecMatch = true;
    }

    public VerificationResult(String poNumber, String heatNumber, String grade, String dimension, String quantity, Boolean heatNumberMatch, Boolean gradeMatch, Boolean dimensionMatch, Boolean quantityMatch, Boolean batchNumberMatch, Boolean productSpecMatch, Double matchPercentage, String status, String verifiedBy) {
        this.poNumber = poNumber;
        this.heatNumber = heatNumber;
        this.grade = grade;
        this.dimension = dimension;
        this.quantity = quantity;
        this.heatNumberMatch = heatNumberMatch;
        this.gradeMatch = gradeMatch;
        this.dimensionMatch = dimensionMatch;
        this.quantityMatch = quantityMatch;
        this.batchNumberMatch = batchNumberMatch;
        this.productSpecMatch = productSpecMatch;
        this.matchPercentage = matchPercentage;
        this.status = status;
        this.verifiedBy = verifiedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getHeatNumber() {
        return heatNumber;
    }

    public void setHeatNumber(String heatNumber) {
        this.heatNumber = heatNumber;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Boolean getHeatNumberMatch() {
        return heatNumberMatch;
    }

    public void setHeatNumberMatch(Boolean heatNumberMatch) {
        this.heatNumberMatch = heatNumberMatch;
    }

    public Boolean getGradeMatch() {
        return gradeMatch;
    }

    public void setGradeMatch(Boolean gradeMatch) {
        this.gradeMatch = gradeMatch;
    }

    public Boolean getDimensionMatch() {
        return dimensionMatch;
    }

    public void setDimensionMatch(Boolean dimensionMatch) {
        this.dimensionMatch = dimensionMatch;
    }

    public Boolean getQuantityMatch() {
        return quantityMatch;
    }

    public void setQuantityMatch(Boolean quantityMatch) {
        this.quantityMatch = quantityMatch;
    }

    public Boolean getBatchNumberMatch() {
        return batchNumberMatch;
    }

    public void setBatchNumberMatch(Boolean batchNumberMatch) {
        this.batchNumberMatch = batchNumberMatch;
    }

    public Boolean getProductSpecMatch() {
        return productSpecMatch;
    }

    public void setProductSpecMatch(Boolean productSpecMatch) {
        this.productSpecMatch = productSpecMatch;
    }

    public Double getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(Double matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
}
