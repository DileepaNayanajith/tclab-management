package com.naturalfoliage.lab.service;

import com.naturalfoliage.lab.model.MotherBottle;
import com.naturalfoliage.lab.model.Subculture;
import com.naturalfoliage.lab.repository.MotherBottleRepository;
import com.naturalfoliage.lab.repository.SubcultureRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class BarcodeResolverService {
    private static final String MOTHER_PREFIX = "91";
    private static final String SUBCULTURE_PREFIX = "92";
    private final MotherBottleRepository mothers;
    private final SubcultureRepository subcultures;

    public BarcodeResolverService(MotherBottleRepository mothers, SubcultureRepository subcultures) {
        this.mothers = mothers;
        this.subcultures = subcultures;
    }

    public Optional<MotherBottle> findMother(String scannedValue) {
        var id = decodeMotherId(scannedValue);
        return id.isPresent() ? mothers.findById(id.get()) : mothers.findByBarcode(scannedValue.trim());
    }

    public Optional<Subculture> findSubculture(String scannedValue) {
        var id = decodeId(scannedValue, SUBCULTURE_PREFIX);
        return id.isPresent() ? subcultures.findById(id.get()) : subcultures.findByBarcode(scannedValue.trim());
    }

    public String motherScanCode(Long id) {
        return MOTHER_PREFIX + "%08d".formatted(id);
    }

    public String subcultureScanCode(Long id) {
        return SUBCULTURE_PREFIX + "%08d".formatted(id);
    }

    private Optional<Long> decodeMotherId(String value) {
        return decodeId(value, MOTHER_PREFIX);
    }

    private Optional<Long> decodeId(String value, String prefix) {
        if (value == null) return Optional.empty();
        var normalized = value.trim();
        if (!normalized.matches(prefix + "\\d{8}")) return Optional.empty();
        return Optional.of(Long.parseLong(normalized.substring(prefix.length())));
    }
}
