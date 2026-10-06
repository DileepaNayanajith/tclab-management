package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.LabEnvironmentReading;
import com.naturalfoliage.lab.repository.LabEnvironmentReadingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@RestController
@RequestMapping("/api/lab-environment")
public class LabEnvironmentController {
    private final LabEnvironmentReadingRepository readings;
    private final String deviceToken;

    public LabEnvironmentController(LabEnvironmentReadingRepository readings,
                                    @Value("${app.lab-sensor.token:}") String deviceToken) {
        this.readings = readings;
        this.deviceToken = deviceToken;
    }

    public record Measurement(Double temperatureC, Double humidityPercent) {}
    public record LatestReading(Double temperatureC, Double humidityPercent, Instant recordedAt) {}

    @GetMapping
    LatestReading latest() {
        return readings.findById(1L)
            .map(reading -> new LatestReading(reading.getTemperatureC(), reading.getHumidityPercent(), reading.getRecordedAt()))
            .orElse(new LatestReading(null, null, null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void record(@RequestHeader(value = "X-Device-Token", required = false) String providedToken,
                @RequestBody Measurement measurement) {
        if (deviceToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Lab sensor is not configured");
        }
        if (providedToken == null || !MessageDigest.isEqual(
                deviceToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device token");
        }
        if (measurement == null || measurement.temperatureC() == null || measurement.humidityPercent() == null
                || !Double.isFinite(measurement.temperatureC()) || !Double.isFinite(measurement.humidityPercent())
                || measurement.temperatureC() < -40 || measurement.temperatureC() > 80
                || measurement.humidityPercent() < 0 || measurement.humidityPercent() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid temperature or humidity");
        }
        var reading = readings.findById(1L).orElseGet(LabEnvironmentReading::new);
        reading.setTemperatureC(measurement.temperatureC());
        reading.setHumidityPercent(measurement.humidityPercent());
        reading.setRecordedAt(Instant.now());
        readings.save(reading);
    }
}
