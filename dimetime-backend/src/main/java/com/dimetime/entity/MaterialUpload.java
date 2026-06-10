package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_uploads")
public class MaterialUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    // AI OCR Extracted Values
    @Column(name = "heat_number")
    private String heatNumber;

    @Column(name = "grade")
    private String grade;

    @Column(name = "dimension")
    private String dimension;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "aspect_ratio")
    private Double aspectRatio;

    @Column(name = "area_fraction")
    private Double areaFraction;

    @Column(name = "visual_material")
    private String visualMaterial;

    @Column(name = "estimated_weight")
    private Double estimatedWeight;

    @Column(name = "validation_status")
    private String validationStatus;

    @Column(name = "visual_material_class")
    private String visualMaterialClass;

    @Column(name = "validation_confidence")
    private Double validationConfidence;

    @Column(name = "validation_message")
    private String validationMessage;

    @Column(name = "po_number")
    private String poNumber;

    public MaterialUpload() {
    }

    public MaterialUpload(String fileName, String uploadedBy, String heatNumber, String grade, String dimension, String quantity, String rawText, Double confidence) {
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
        this.heatNumber = heatNumber;
        this.grade = grade;
        this.dimension = dimension;
        this.quantity = quantity;
        this.rawText = rawText;
        this.confidence = confidence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
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

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Double getAreaFraction() {
        return areaFraction;
    }

    public void setAreaFraction(Double areaFraction) {
        this.areaFraction = areaFraction;
    }

    public String getVisualMaterial() {
        return visualMaterial;
    }

    public void setVisualMaterial(String visualMaterial) {
        this.visualMaterial = visualMaterial;
    }

    public Double getEstimatedWeight() {
        return estimatedWeight;
    }

    public void setEstimatedWeight(Double estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getVisualMaterialClass() {
        return visualMaterialClass;
    }

    public void setVisualMaterialClass(String visualMaterialClass) {
        this.visualMaterialClass = visualMaterialClass;
    }

    public Double getValidationConfidence() {
        return validationConfidence;
    }

    public void setValidationConfidence(Double validationConfidence) {
        this.validationConfidence = validationConfidence;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }
}
