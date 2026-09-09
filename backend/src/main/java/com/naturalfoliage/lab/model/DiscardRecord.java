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
}
