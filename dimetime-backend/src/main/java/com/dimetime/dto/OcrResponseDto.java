package com.dimetime.dto;

public class OcrResponseDto {
    private String heatNumber;
    private String grade;
    private String dimension;
    private String quantity;
    private String rawText;
    private Double confidence;
    private Double aspectRatio;
    private Double areaFraction;
    private String visualMaterial;
    private Double estimatedWeight;
    private String validationStatus;
    private String visualMaterialClass;
    private Double validationConfidence;
    private String validationMessage;
    private String batchNumber;

    public OcrResponseDto() {
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
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
}
