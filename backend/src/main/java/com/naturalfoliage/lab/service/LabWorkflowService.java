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
    private final BarcodeResolverService barcodes;
    public LabWorkflowService(MotherBottleRepository mothers, SubcultureRepository subcultures, DiscardRepository discards, BarcodeResolverService barcodes) {
        this.mothers = mothers; this.subcultures = subcultures; this.discards = discards; this.barcodes = barcodes;
    }
    @Transactional
    public DiscardRecord discard(String barcode, String reason, String technician) {
        int count; Plant plant; int cycle; int week; String sourceTechnician; String sourceLaminaFlow;
        String storedBarcode;
        var sub = barcodes.findSubculture(barcode);
        if (sub.isPresent()) {
            var bottle = sub.get();
            storedBarcode = bottle.getBarcode();
            if (bottle.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
            bottle.setStatus(BottleStatus.DISCARDED); count = bottle.getPlantCount(); plant = bottle.getParent().getPlant();
            cycle = bottle.getCycle(); week = bottle.getSubcultureWeek(); sourceTechnician = bottle.getTechnician();
            sourceLaminaFlow = bottle.getLaminaFlow();
        }
        else {
            var mother = barcodes.findMother(barcode).orElseThrow(() -> new IllegalArgumentException("Bottle not found"));
            storedBarcode = mother.getBarcode();
            if (mother.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
            mother.setStatus(BottleStatus.DISCARDED); count = mother.getPlantCount();
            plant = mother.getPlant(); cycle = mother.getCycle(); week = mother.getCultureWeek(); sourceTechnician = mother.getTechnician();
            sourceLaminaFlow = mother.getLaminaFlow();
        }
        if (discards.existsByBarcode(storedBarcode)) throw new IllegalStateException("Barcode was already discarded");
        var record = new DiscardRecord(); record.setBarcode(storedBarcode); record.setReason(reason); record.setTechnician(technician); record.setPlantCount(count);
        record.setPlantCode(plant.getCode()); record.setPlantName(plant.getName()); record.setCycle(cycle);
        record.setCultureWeek(week); record.setSourceTechnician(sourceTechnician);
        record.setSourceLaminaFlow(sourceLaminaFlow);
        return discards.save(record);
    }
}
