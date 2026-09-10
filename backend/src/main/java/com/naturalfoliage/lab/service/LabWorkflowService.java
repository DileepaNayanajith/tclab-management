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
        int count; Plant plant; int cycle; int week; String sourceTechnician;
        var sub = subcultures.findByBarcode(barcode);
        if (sub.isPresent()) {
            var bottle = sub.get();
            if (bottle.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
            bottle.setStatus(BottleStatus.DISCARDED); count = bottle.getPlantCount(); plant = bottle.getParent().getPlant();
            cycle = bottle.getCycle(); week = bottle.getSubcultureWeek(); sourceTechnician = bottle.getTechnician();
        }
        else {
            var mother = mothers.findByBarcode(barcode).orElseThrow(() -> new IllegalArgumentException("Bottle not found"));
            if (mother.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
            mother.setStatus(BottleStatus.DISCARDED); count = mother.getPlantCount();
            plant = mother.getPlant(); cycle = mother.getCycle(); week = mother.getCultureWeek(); sourceTechnician = mother.getTechnician();
        }
        var record = new DiscardRecord(); record.setBarcode(barcode); record.setReason(reason); record.setTechnician(technician); record.setPlantCount(count);
        record.setPlantCode(plant.getCode()); record.setPlantName(plant.getName()); record.setCycle(cycle);
        record.setCultureWeek(week); record.setSourceTechnician(sourceTechnician);
        return discards.save(record);
    }
}
