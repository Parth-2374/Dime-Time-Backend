package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plate_calculations")
public class PlateCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "material", nullable = false)
    private String material;

    @Column(name = "density", nullable = false)
    private Double density;

    @Column(name = "length", nullable = false)
    private Double length;

    @Column(name = "width", nullable = false)
    private Double width;

    @Column(name = "thickness", nullable = false)
    private Double thickness;

    @Column(name = "volume", nullable = false)
    private Double volume;

    @Column(name = "estimated_weight", nullable = false)
    private Double estimatedWeight;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "validation_status")
    private String validationStatus;

    @Column(name = "material_class")
    private String materialClass;

    @Column(name = "inferred_dimensions")
    private String inferredDimensions;

    @Column(name = "ai_estimated_weight")
    private Double aiEstimatedWeight;

    @Column(name = "calculated_weight")
    private Double calculatedWeight;

    @Column(name = "difference_percentage")
    private Double differencePercentage;

    @Column(name = "validation_confidence")
    private Double validationConfidence;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    @Column(name = "calculated_by", nullable = false)
    private String calculatedBy;

    public PlateCalculation() {
    }

    public PlateCalculation(String fileName, String material, Double density, Double length, Double width, Double thickness, Double volume, Double estimatedWeight, Double confidence, String calculatedBy) {
        this.fileName = fileName;
        this.material = material;
        this.density = density;
        this.length = length;
        this.width = width;
        this.thickness = thickness;
        this.volume = volume;
        this.estimatedWeight = estimatedWeight;
        this.confidence = confidence;
        this.calculatedBy = calculatedBy;
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

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Double getDensity() {
        return density;
    }

    public void setDensity(Double density) {
        this.density = density;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getThickness() {
        return thickness;
    }

    public void setThickness(Double thickness) {
        this.thickness = thickness;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getEstimatedWeight() {
        return estimatedWeight;
    }

    public void setEstimatedWeight(Double estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getCalculatedBy() {
        return calculatedBy;
    }

    public void setCalculatedBy(String calculatedBy) {
        this.calculatedBy = calculatedBy;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getMaterialClass() {
        return materialClass;
    }

    public void setMaterialClass(String materialClass) {
        this.materialClass = materialClass;
    }

    public String getInferredDimensions() {
        return inferredDimensions;
    }

    public void setInferredDimensions(String inferredDimensions) {
        this.inferredDimensions = inferredDimensions;
    }

    public Double getAiEstimatedWeight() {
        return aiEstimatedWeight;
    }

    public void setAiEstimatedWeight(Double aiEstimatedWeight) {
        this.aiEstimatedWeight = aiEstimatedWeight;
    }

    public Double getCalculatedWeight() {
        return calculatedWeight;
    }

    public void setCalculatedWeight(Double calculatedWeight) {
        this.calculatedWeight = calculatedWeight;
    }

    public Double getDifferencePercentage() {
        return differencePercentage;
    }

    public void setDifferencePercentage(Double differencePercentage) {
        this.differencePercentage = differencePercentage;
    }

    public Double getValidationConfidence() {
        return validationConfidence;
    }

    public void setValidationConfidence(Double validationConfidence) {
        this.validationConfidence = validationConfidence;
    }
}
