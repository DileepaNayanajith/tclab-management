package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class DiscardRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String barcode;
    @Column(nullable = false) private String reason;
    private int plantCount;
    private String technician;
    private String plantCode;
    private String plantName;
    private int cycle;
    private int cultureWeek;
    private String sourceTechnician;
    private LocalDate discardedDate = LocalDate.now();
    public Long getId() { return id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getPlantCount() { return plantCount; }
    public void setPlantCount(int plantCount) { this.plantCount = plantCount; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public LocalDate getDiscardedDate() { return discardedDate; }
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }
    public int getCycle() { return cycle; }
    public void setCycle(int cycle) { this.cycle = cycle; }
    public int getCultureWeek() { return cultureWeek; }
    public void setCultureWeek(int cultureWeek) { this.cultureWeek = cultureWeek; }
    public String getSourceTechnician() { return sourceTechnician; }
    public void setSourceTechnician(String sourceTechnician) { this.sourceTechnician = sourceTechnician; }
}
