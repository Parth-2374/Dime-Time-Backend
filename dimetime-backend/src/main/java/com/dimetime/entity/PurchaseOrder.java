package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true)
    private String poNumber;

    @Column(nullable = false)
    private String supplier;

    @Column(nullable = false)
    private String material;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String dimension;

    @Column(nullable = false)
    private String quantity;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PRODUCTION_START, COMPLETED_PRODUCTION, DISPATCHED, DELIVERED

    @Column(nullable = true)
    private String manufacturer;

    @Column(name = "dispatch_carrier", nullable = true)
    private String dispatchCarrier;

    @Column(name = "dispatch_tracking_number", nullable = true)
    private String dispatchTrackingNumber;

    @Column(name = "mtc_file_name", nullable = true)
    private String mtcFileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_company_id", nullable = true)
    private Company supplierCompany;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manufacturer_company_id", nullable = true)
    private Company manufacturerCompany;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = true)
    private User supplierUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manufacturer_id", nullable = true)
    private User manufacturerUser;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = "APPROVED"; // DRAFT, APPROVED, REJECTED

    // Detailed SCM PO fields
    @Column(name = "rfq_number")
    private String rfqNumber;

    @Column(name = "quotation_reference_number")
    private String quotationReferenceNumber;

    @Column(name = "supplier_company_name")
    private String supplierCompanyName;

    @Column(name = "supplier_address", length = 1000)
    private String supplierAddress;

    @Column(name = "supplier_gst_number")
    private String supplierGstNumber;

    @Column(name = "supplier_username")
    private String supplierUsername;

    @Column(name = "manufacturer_company_name")
    private String manufacturerCompanyName;

    @Column(name = "manufacturer_address", length = 1000)
    private String manufacturerAddress;

    @Column(name = "manufacturer_gst_number")
    private String manufacturerGstNumber;

    @Column(name = "manufacturer_username")
    private String manufacturerUsername;

    @Column(name = "material_name")
    private String materialName;

    @Column(name = "material_description", length = 1000)
    private String materialDescription;

    @Column(name = "material_grade")
    private String materialGrade;

    @Column(name = "material_type")
    private String materialType;

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

    @Column(name = "quantity_value")
    private Double quantityValue;

    @Column(name = "quantity_unit")
    private String quantityUnit;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "gst_tax")
    private Double gstTax = 18.0;

    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;

    @Column(name = "delivery_location")
    private String deliveryLocation;

    @Column(name = "delivery_terms")
    private String deliveryTerms;

    @Column(name = "mtc_required")
    private Boolean mtcRequired = false;

    @Lob
    @Column(name = "qr_code_image", columnDefinition = "LONGBLOB")
    private byte[] qrCodeImage;


    public PurchaseOrder() {
    }

    public PurchaseOrder(String poNumber, String supplier, String material, String grade, String dimension, String quantity) {
        this.poNumber = poNumber;
        this.supplier = supplier;
        this.material = material;
        this.grade = grade;
        this.dimension = dimension;
        this.quantity = quantity;
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

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDispatchCarrier() {
        return dispatchCarrier;
    }

    public void setDispatchCarrier(String dispatchCarrier) {
        this.dispatchCarrier = dispatchCarrier;
    }

    public String getDispatchTrackingNumber() {
        return dispatchTrackingNumber;
    }

    public void setDispatchTrackingNumber(String dispatchTrackingNumber) {
        this.dispatchTrackingNumber = dispatchTrackingNumber;
    }

    public String getMtcFileName() {
        return mtcFileName;
    }

    public void setMtcFileName(String mtcFileName) {
        this.mtcFileName = mtcFileName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Company getSupplierCompany() {
        return supplierCompany;
    }

    public void setSupplierCompany(Company supplierCompany) {
        this.supplierCompany = supplierCompany;
    }

    public Company getManufacturerCompany() {
        return manufacturerCompany;
    }

    public void setManufacturerCompany(Company manufacturerCompany) {
        this.manufacturerCompany = manufacturerCompany;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public String getQuotationReferenceNumber() {
        return quotationReferenceNumber;
    }

    public void setQuotationReferenceNumber(String quotationReferenceNumber) {
        this.quotationReferenceNumber = quotationReferenceNumber;
    }

    public String getSupplierCompanyName() {
        return supplierCompanyName;
    }

    public void setSupplierCompanyName(String supplierCompanyName) {
        this.supplierCompanyName = supplierCompanyName;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public void setSupplierAddress(String supplierAddress) {
        this.supplierAddress = supplierAddress;
    }

    public String getSupplierGstNumber() {
        return supplierGstNumber;
    }

    public void setSupplierGstNumber(String supplierGstNumber) {
        this.supplierGstNumber = supplierGstNumber;
    }

    public String getSupplierUsername() {
        return supplierUsername;
    }

    public void setSupplierUsername(String supplierUsername) {
        this.supplierUsername = supplierUsername;
    }

    public String getManufacturerCompanyName() {
        return manufacturerCompanyName;
    }

    public void setManufacturerCompanyName(String manufacturerCompanyName) {
        this.manufacturerCompanyName = manufacturerCompanyName;
    }

    public String getManufacturerAddress() {
        return manufacturerAddress;
    }

    public void setManufacturerAddress(String manufacturerAddress) {
        this.manufacturerAddress = manufacturerAddress;
    }

    public String getManufacturerGstNumber() {
        return manufacturerGstNumber;
    }

    public void setManufacturerGstNumber(String manufacturerGstNumber) {
        this.manufacturerGstNumber = manufacturerGstNumber;
    }

    public String getManufacturerUsername() {
        return manufacturerUsername;
    }

    public void setManufacturerUsername(String manufacturerUsername) {
        this.manufacturerUsername = manufacturerUsername;
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

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getGstTax() {
        return gstTax;
    }

    public void setGstTax(Double gstTax) {
        this.gstTax = gstTax;
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

    public String getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(String deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }

    public Boolean getMtcRequired() {
        return mtcRequired;
    }

    public void setMtcRequired(Boolean mtcRequired) {
        this.mtcRequired = mtcRequired;
    }

    public User getSupplierUser() {
        return supplierUser;
    }

    public void setSupplierUser(User supplierUser) {
        this.supplierUser = supplierUser;
    }

    public User getManufacturerUser() {
        return manufacturerUser;
    }

    public void setManufacturerUser(User manufacturerUser) {
        this.manufacturerUser = manufacturerUser;
    }

    public Long getSupplierId() {
        return supplierUser != null ? supplierUser.getId() : null;
    }

    public Long getManufacturerId() {
        return manufacturerUser != null ? manufacturerUser.getId() : null;
    }

    public byte[] getQrCodeImage() {
        return qrCodeImage;
    }

    public void setQrCodeImage(byte[] qrCodeImage) {
        this.qrCodeImage = qrCodeImage;
    }
}
