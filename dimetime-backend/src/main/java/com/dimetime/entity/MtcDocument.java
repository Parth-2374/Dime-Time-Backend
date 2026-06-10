package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mtc_documents")
public class MtcDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    // Parsed MTC properties
    @Column(name = "heat_number")
    private String heatNumber;

    @Column(name = "grade")
    private String grade;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "po_number", nullable = true)
    private String poNumber;

    @Column(name = "carbon")
    private Double carbon;

    @Column(name = "chromium")
    private Double chromium;

    @Column(name = "nickel")
    private Double nickel;

    @Column(name = "molybdenum")
    private Double molybdenum;

    @Column(name = "manganese")
    private Double manganese;

    @Column(name = "silicon")
    private Double silicon;

    @Column(name = "yield_strength")
    private Double yieldStrength;

    @Column(name = "tensile_strength")
    private Double tensileStrength;

    @Column(name = "elongation")
    private Double elongation;

    @Column(name = "hardness")
    private Double hardness;

    // Certificate Specific Properties
    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "rfq_number")
    private String rfqNumber;

    @Column(name = "manufacturer_details", length = 1000)
    private String manufacturerDetails;

    @Column(name = "supplier_details", length = 1000)
    private String supplierDetails;

    @Column(name = "material_description", length = 1000)
    private String materialDescription;

    @Column(name = "issue_date")
    private LocalDateTime issueDate = LocalDateTime.now();

    @Column(name = "digital_approval_stamp")
    private String digitalApprovalStamp = "AI APPROVED - DIMETIME SCM";

    @Column(name = "status")
    private String status = "SUPPLIER_REVIEW";

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "dimension")
    private String dimension;

    @Column(name = "ocr_match_score")
    private Double ocrMatchScore;

    @Column(name = "validation_result")
    private String validationResult;

    @Column(name = "validation_timestamp")
    private LocalDateTime validationTimestamp;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "rejection_comment", length = 2000)
    private String rejectionComment;

    public MtcDocument() {
    }

    public MtcDocument(String fileName, String uploadedBy, String heatNumber, String grade, Double carbon, Double chromium, Double nickel, Double yieldStrength, Double tensileStrength) {
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
        this.heatNumber = heatNumber;
        this.grade = grade;
        this.carbon = carbon;
        this.chromium = chromium;
        this.nickel = nickel;
        this.yieldStrength = yieldStrength;
        this.tensileStrength = tensileStrength;
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

    public Double getCarbon() {
        return carbon;
    }

    public void setCarbon(Double carbon) {
        this.carbon = carbon;
    }

    public Double getChromium() {
        return chromium;
    }

    public void setChromium(Double chromium) {
        this.chromium = chromium;
    }

    public Double getNickel() {
        return nickel;
    }

    public void setNickel(Double nickel) {
        this.nickel = nickel;
    }

    public Double getMolybdenum() {
        return molybdenum;
    }

    public void setMolybdenum(Double molybdenum) {
        this.molybdenum = molybdenum;
    }

    public Double getManganese() {
        return manganese;
    }

    public void setManganese(Double manganese) {
        this.manganese = manganese;
    }

    public Double getSilicon() {
        return silicon;
    }

    public void setSilicon(Double silicon) {
        this.silicon = silicon;
    }

    public Double getYieldStrength() {
        return yieldStrength;
    }

    public void setYieldStrength(Double yieldStrength) {
        this.yieldStrength = yieldStrength;
    }

    public Double getTensileStrength() {
        return tensileStrength;
    }

    public void setTensileStrength(Double tensileStrength) {
        this.tensileStrength = tensileStrength;
    }

    public Double getElongation() {
        return elongation;
    }

    public void setElongation(Double elongation) {
        this.elongation = elongation;
    }

    public Double getHardness() {
        return hardness;
    }

    public void setHardness(Double hardness) {
        this.hardness = hardness;
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

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public String getManufacturerDetails() {
        return manufacturerDetails;
    }

    public void setManufacturerDetails(String manufacturerDetails) {
        this.manufacturerDetails = manufacturerDetails;
    }

    public String getSupplierDetails() {
        return supplierDetails;
    }

    public void setSupplierDetails(String supplierDetails) {
        this.supplierDetails = supplierDetails;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public String getDigitalApprovalStamp() {
        return digitalApprovalStamp;
    }

    public void setDigitalApprovalStamp(String digitalApprovalStamp) {
        this.digitalApprovalStamp = digitalApprovalStamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public Double getOcrMatchScore() {
        return ocrMatchScore;
    }

    public void setOcrMatchScore(Double ocrMatchScore) {
        this.ocrMatchScore = ocrMatchScore;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }

    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public void setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
    }
}
