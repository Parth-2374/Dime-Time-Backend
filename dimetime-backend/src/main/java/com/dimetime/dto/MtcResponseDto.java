package com.dimetime.dto;

public class MtcResponseDto {
    private String heatNumber;
    private String grade;
    private Double carbon;
    private Double chromium;
    private Double nickel;
    private Double molybdenum;
    private Double manganese;
    private Double silicon;
    private Double yieldStrength;
    private Double tensileStrength;
    private Double elongation;
    private Double hardness;
    private String batchNumber; // Added for 5-way reconciliation
    private String materialDescription;
    private String materialName;
    private Double confidence;
    private String quantity;
    private String dimension;
    private Double ocrMatchScore;
    private String validationResult;
    private String rawText;

    public MtcResponseDto() {
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
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

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
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

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }
}
