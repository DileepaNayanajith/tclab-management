package com.naturalfoliage.lab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class LabEnvironmentReading {
    @Id private Long id = 1L;
    private double temperatureC;
    private double humidityPercent;
    private Instant recordedAt;

    public Long getId() { return id; }
    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }
    public double getHumidityPercent() { return humidityPercent; }
    public void setHumidityPercent(double humidityPercent) { this.humidityPercent = humidityPercent; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
