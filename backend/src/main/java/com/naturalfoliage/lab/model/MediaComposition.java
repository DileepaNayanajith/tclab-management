package com.naturalfoliage.lab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class MediaComposition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true) private String code;
    @NotBlank private String basalMedia;
    private String hormones;
    private Double ph;
    private Double agar;
    @Min(0) private int availableBottles;
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getBasalMedia() { return basalMedia; }
    public void setBasalMedia(String basalMedia) { this.basalMedia = basalMedia; }
    public String getHormones() { return hormones; }
    public void setHormones(String hormones) { this.hormones = hormones; }
    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }
    public Double getAgar() { return agar; }
    public void setAgar(Double agar) { this.agar = agar; }
    public int getAvailableBottles() { return availableBottles; }
    public void setAvailableBottles(int availableBottles) { this.availableBottles = availableBottles; }
}
