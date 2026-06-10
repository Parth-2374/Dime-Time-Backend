package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotations")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false)
    private String rfqNumber;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private double price; // Acts as total price

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, SUBMITTED, ACCEPTED, REJECTED

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // New Bidding details
    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "delivery_terms")
    private String deliveryTerms;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manufacturer_user_id")
    private User manufacturerUser;

    public Quotation() {
    }

    public Quotation(String rfqNumber, String manufacturer, double price, int leadTimeDays, String status, String remarks) {
        this.rfqNumber = rfqNumber;
        this.manufacturer = manufacturer;
        this.price = price;
        this.leadTimeDays = leadTimeDays;
        this.status = status;
        this.remarks = remarks;
    }

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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(String deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }

    public User getManufacturerUser() {
        return manufacturerUser;
    }

    public void setManufacturerUser(User manufacturerUser) {
        this.manufacturerUser = manufacturerUser;
    }
}
