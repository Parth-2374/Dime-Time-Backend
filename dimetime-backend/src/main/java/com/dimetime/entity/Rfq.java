package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "rfqs")
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false, unique = true)
    private String rfqNumber;

    // Legacy fields for backward compatibility
    @Column(nullable = true)
    private String material;

    @Column(nullable = true)
    private String grade;

    @Column(nullable = true)
    private String dimension;

    @Column(nullable = true)
    private String quantity;

    @Column(nullable = false)
    private String status = "CREATED"; // CREATED, BROADCASTED, QUOTED, QUOTATION_SELECTED, PO_GENERATED, CANCELLED

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // New Detailed Industrial RFQ Fields
    @Column(name = "material_name")
    private String materialName;

    @Column(name = "material_description", length = 1000)
    private String materialDescription;

    @Column(name = "material_grade")
    private String materialGrade;

    @Column(name = "material_type")
    private String materialType;

    // Dimensions details
    @Column(name = "thickness")
    private Double thickness;

    @Column(name = "width")
    private Double width;

    @Column(name = "length")
    private Double length;

    @Column(name = "diameter")
    private Double diameter;

    @Column(name = "required_dimension")
    private String requiredDimension;

    // Quantity Information
    @Column(name = "quantity_value")
    private Double quantityValue;

    @Column(name = "quantity_unit")
    private String quantityUnit; // KG, TON, PCS, METER

    // Delivery Information
    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;

    @Column(name = "delivery_location")
    private String deliveryLocation;

    // Additional Information
    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    @Column(name = "drawing_url")
    private String drawingUrl;

    @Column(name = "tech_spec_url")
    private String techSpecUrl;

    // Quality requirements
    @Column(name = "mtc_required")
    private Boolean mtcRequired = false;

    @Column(name = "third_party_inspection_required")
    private Boolean thirdPartyInspectionRequired = false;

    public Rfq() {
    }

    public Rfq(String rfqNumber, String material, String grade, String dimension, String quantity, String status, String createdBy) {
        this.rfqNumber = rfqNumber;
        this.material = material;
        this.grade = grade;
        this.dimension = dimension;
        this.quantity = quantity;
        this.status = status;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public String getMaterialGrade() {
        return materialGrade;
    }

    public void setMaterialGrade(String materialGrade) {
        this.materialGrade = materialGrade;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public Double getThickness() {
        return thickness;
    }

    public void setThickness(Double thickness) {
        this.thickness = thickness;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getDiameter() {
        return diameter;
    }

    public void setDiameter(Double diameter) {
        this.diameter = diameter;
    }

    public String getRequiredDimension() {
        return requiredDimension;
    }

    public void setRequiredDimension(String requiredDimension) {
        this.requiredDimension = requiredDimension;
    }

    public Double getQuantityValue() {
        return quantityValue;
    }

    public void setQuantityValue(Double quantityValue) {
        this.quantityValue = quantityValue;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public LocalDate getRequiredDeliveryDate() {
        return requiredDeliveryDate;
    }

    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) {
        this.requiredDeliveryDate = requiredDeliveryDate;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public String getDrawingUrl() {
        return drawingUrl;
    }

    public void setDrawingUrl(String drawingUrl) {
        this.drawingUrl = drawingUrl;
    }

    public String getTechSpecUrl() {
        return techSpecUrl;
    }

    public void setTechSpecUrl(String techSpecUrl) {
        this.techSpecUrl = techSpecUrl;
    }

    public Boolean getMtcRequired() {
        return mtcRequired;
    }

    public void setMtcRequired(Boolean mtcRequired) {
        this.mtcRequired = mtcRequired;
    }

    public Boolean getThirdPartyInspectionRequired() {
        return thirdPartyInspectionRequired;
    }

    public void setThirdPartyInspectionRequired(Boolean thirdPartyInspectionRequired) {
        this.thirdPartyInspectionRequired = thirdPartyInspectionRequired;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
}
