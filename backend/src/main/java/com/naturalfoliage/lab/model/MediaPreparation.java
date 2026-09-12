package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
public class MediaPreparation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private MediaComposition media;
    @Min(1) private int bottleCount;
    private String technician;
    private LocalDateTime preparedAt = LocalDateTime.now();
    public Long getId() { return id; }
    public MediaComposition getMedia() { return media; }
    public void setMedia(MediaComposition media) { this.media = media; }
    public int getBottleCount() { return bottleCount; }
    public void setBottleCount(int bottleCount) { this.bottleCount = bottleCount; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public LocalDateTime getPreparedAt() { return preparedAt; }
}
