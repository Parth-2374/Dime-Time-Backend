package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "po_number", nullable = false)
    private String poNumber;

    @Column(name = "grn_number", nullable = false)
    private String grnNumber;

    @Column(nullable = false)
    private String supplier;

    @Column(nullable = false)
    private String manufacturer;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(name = "material_grade", nullable = false)
    private String materialGrade;

    @Column(nullable = false)
    private String quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double gst;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate = LocalDateTime.now();

    @Column(nullable = false)
    private String status = "INVOICE_GENERATED"; // INVOICE_GENERATED, PAYMENT_PENDING, PAYMENT_COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Invoice() {
    }

    public Invoice(String invoiceNumber, String poNumber, String grnNumber, String supplier, String manufacturer,
                   String materialName, String materialGrade, String quantity, Double unitPrice, Double gst,
                   Double totalAmount, String status) {
        this.invoiceNumber = invoiceNumber;
        this.poNumber = poNumber;
        this.grnNumber = grnNumber;
        this.supplier = supplier;
        this.manufacturer = manufacturer;
        this.materialName = materialName;
        this.materialGrade = materialGrade;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.gst = gst;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getGrnNumber() {
        return grnNumber;
    }

    public void setGrnNumber(String grnNumber) {
        this.grnNumber = grnNumber;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialGrade() {
        return materialGrade;
    }

    public void setMaterialGrade(String materialGrade) {
        this.materialGrade = materialGrade;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getGst() {
        return gst;
    }

    public void setGst(Double gst) {
        this.gst = gst;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
