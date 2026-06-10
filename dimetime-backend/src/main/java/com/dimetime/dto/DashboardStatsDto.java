package com.dimetime.dto;

public class DashboardStatsDto {
    private long totalMaterialUploads;
    private long approvedGrn;
    private long rejectedMaterials;
    private long pendingVerification;
    private long totalPlateCalculations;

    public DashboardStatsDto() {
    }

    public DashboardStatsDto(long totalMaterialUploads, long approvedGrn, long rejectedMaterials, long pendingVerification, long totalPlateCalculations) {
        this.totalMaterialUploads = totalMaterialUploads;
        this.approvedGrn = approvedGrn;
        this.rejectedMaterials = rejectedMaterials;
        this.pendingVerification = pendingVerification;
        this.totalPlateCalculations = totalPlateCalculations;
    }

    public long getTotalMaterialUploads() {
        return totalMaterialUploads;
    }

    public void setTotalMaterialUploads(long totalMaterialUploads) {
        this.totalMaterialUploads = totalMaterialUploads;
    }

    public long getApprovedGrn() {
        return approvedGrn;
    }

    public void setApprovedGrn(long approvedGrn) {
        this.approvedGrn = approvedGrn;
    }

    public long getRejectedMaterials() {
        return rejectedMaterials;
    }

    public void setRejectedMaterials(long rejectedMaterials) {
        this.rejectedMaterials = rejectedMaterials;
    }

    public long getPendingVerification() {
        return pendingVerification;
    }

    public void setPendingVerification(long pendingVerification) {
        this.pendingVerification = pendingVerification;
    }

    public long getTotalPlateCalculations() {
        return totalPlateCalculations;
    }

    public void setTotalPlateCalculations(long totalPlateCalculations) {
        this.totalPlateCalculations = totalPlateCalculations;
    }
}
