package com.naturalfoliage.lab.service;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class LabWorkflowService {
    private final MotherBottleRepository mothers;
    private final SubcultureRepository subcultures;
    private final DiscardRepository discards;
    public LabWorkflowService(MotherBottleRepository mothers, SubcultureRepository subcultures, DiscardRepository discards) {
        this.mothers = mothers; this.subcultures = subcultures; this.discards = discards;
    }
    @Transactional
    public DiscardRecord discard(String barcode, String reason, String technician) {
        if (discards.existsByBarcode(barcode)) throw new IllegalStateException("Barcode was already discarded");
        int count;
        var sub = subcultures.findByBarcode(barcode);
        if (sub.isPresent()) { sub.get().setStatus(BottleStatus.DISCARDED); count = sub.get().getPlantCount(); }
        else {
            var mother = mothers.findByBarcode(barcode).orElseThrow(() -> new IllegalArgumentException("Bottle not found"));
            mother.setStatus(BottleStatus.DISCARDED); count = mother.getPlantCount();
        }
        var record = new DiscardRecord(); record.setBarcode(barcode); record.setReason(reason); record.setTechnician(technician); record.setPlantCount(count);
        return discards.save(record);
    }
}
