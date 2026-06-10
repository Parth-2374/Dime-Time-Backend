package com.dimetime.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_assignments")
public class RfqAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false)
    private String rfqNumber;

    @Column(nullable = false)
    private String manufacturer; // Username of manufacturer

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, QUOTED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manufacturer_user_id")
    private User manufacturerUser;

    public RfqAssignment() {
    }

    public RfqAssignment(String rfqNumber, String manufacturer, String status) {
        this.rfqNumber = rfqNumber;
        this.manufacturer = manufacturer;
        this.status = status;
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

    public User getManufacturerUser() {
        return manufacturerUser;
    }

    public void setManufacturerUser(User manufacturerUser) {
        this.manufacturerUser = manufacturerUser;
    }
}
