package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import com.naturalfoliage.lab.service.LabWorkflowService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class WorkflowController {
    private final LabWorkflowService workflow; private final DiscardRepository discards;
    private final PlantRepository plants; private final MotherBottleRepository mothers; private final SubcultureRepository subcultures;
    public WorkflowController(LabWorkflowService workflow, DiscardRepository discards, PlantRepository plants, MotherBottleRepository mothers, SubcultureRepository subcultures) {
        this.workflow = workflow; this.discards = discards; this.plants = plants; this.mothers = mothers; this.subcultures = subcultures;
    }
    public record DiscardRequest(String barcode, String reason, String technician) {}
    @GetMapping("/discards") List<DiscardRecord> discards() { return discards.findAll(); }
    @PostMapping("/discards") DiscardRecord discard(@RequestBody DiscardRequest request) { return workflow.discard(request.barcode(), request.reason(), request.technician()); }
    @GetMapping("/dashboard") Map<String,Long> dashboard() {
        return Map.of("plants", plants.count(), "activeMothers", mothers.countByStatus(BottleStatus.ACTIVE), "activeSubcultures", subcultures.countByStatus(BottleStatus.ACTIVE), "discards", discards.count());
    }
}
