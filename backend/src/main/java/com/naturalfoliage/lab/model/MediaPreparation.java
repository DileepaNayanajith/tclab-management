package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
public class MediaPreparation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private MediaComposition media;
    @Min(1) private int bottleCount;
    @DecimalMin("0.0") private Double ph;
    @DecimalMin("0.0") private Double agar;
    private String sterilizationMethod;
    private String technician;
    private LocalDateTime preparedAt = LocalDateTime.now();
    public Long getId() { return id; }
    public MediaComposition getMedia() { return media; }
    public void setMedia(MediaComposition media) { this.media = media; }
    public int getBottleCount() { return bottleCount; }
    public void setBottleCount(int bottleCount) { this.bottleCount = bottleCount; }
    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }
    public Double getAgar() { return agar; }
    public void setAgar(Double agar) { this.agar = agar; }
    public String getSterilizationMethod() { return sterilizationMethod; }
    public void setSterilizationMethod(String sterilizationMethod) { this.sterilizationMethod = sterilizationMethod; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public LocalDateTime getPreparedAt() { return preparedAt; }
}
