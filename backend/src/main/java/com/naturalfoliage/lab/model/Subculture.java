package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
public class Subculture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true) private String barcode;
    @ManyToOne(optional = false) private MotherBottle parent;
    @ManyToOne(optional = false) private MediaComposition media;
    @Min(1) private int plantCount;
    private int cycle;
    @NotBlank private String technician;
    private boolean rooting;
    private LocalDate createdDate = LocalDate.now();
    @Enumerated(EnumType.STRING) private BottleStatus status = BottleStatus.ACTIVE;
    public Long getId() { return id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public MotherBottle getParent() { return parent; }
    public void setParent(MotherBottle parent) { this.parent = parent; }
    public MediaComposition getMedia() { return media; }
    public void setMedia(MediaComposition media) { this.media = media; }
    public int getPlantCount() { return plantCount; }
    public void setPlantCount(int plantCount) { this.plantCount = plantCount; }
    public int getCycle() { return cycle; }
    public void setCycle(int cycle) { this.cycle = cycle; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public boolean isRooting() { return rooting; }
    public void setRooting(boolean rooting) { this.rooting = rooting; }
    public LocalDate getCreatedDate() { return createdDate; }
    public BottleStatus getStatus() { return status; }
    public void setStatus(BottleStatus status) { this.status = status; }
}
