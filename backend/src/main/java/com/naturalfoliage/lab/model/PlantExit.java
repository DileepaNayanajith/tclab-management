package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PlantExit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String barcode;
    private String plantCode;
    private String plantName;
    private int quantity;
    private String destination;
    private String reference;
    private String technician;
    private LocalDateTime exitedAt = LocalDateTime.now();
    public Long getId() { return id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public LocalDateTime getExitedAt() { return exitedAt; }
}
