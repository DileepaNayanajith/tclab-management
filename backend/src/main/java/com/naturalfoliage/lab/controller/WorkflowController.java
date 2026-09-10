package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import com.naturalfoliage.lab.service.LabWorkflowService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

    public record AvailablePlant(String plantCode, String plantName, int multiply, int rooting, int total) {}
    public record OldCulture(int subcultureWeek, String plantCode, String variety,
        int bottles, int totalPlants, long ageWeeks) {}
    public record DashboardDetails(List<AvailablePlant> availablePlants, List<OldCulture> oldCultures) {}

    @GetMapping("/dashboard/details")
    DashboardDetails dashboardDetails() {
        var active = subcultures.findAll().stream()
            .filter(culture -> culture.getStatus() == BottleStatus.ACTIVE)
            .toList();

        record AvailableKey(String code, String name) {}
        var availableGroups = new LinkedHashMap<AvailableKey, int[]>();
        for (var culture : active) {
            var plant = culture.getParent().getPlant();
            var totals = availableGroups.computeIfAbsent(
                new AvailableKey(plant.getCode(), displayName(plant)), key -> new int[2]);
            totals[culture.isRooting() ? 1 : 0] += culture.getPlantCount();
        }
        var available = availableGroups.entrySet().stream()
            .map(entry -> new AvailablePlant(entry.getKey().code(), entry.getKey().name(),
                entry.getValue()[0], entry.getValue()[1], entry.getValue()[0] + entry.getValue()[1]))
            .sorted(Comparator.comparing(AvailablePlant::plantCode))
            .toList();

        record OldKey(int week, String code, String variety, long age) {}
        var oldGroups = new LinkedHashMap<OldKey, int[]>();
        for (var culture : active) {
            long age = ChronoUnit.WEEKS.between(culture.getCreatedDate(), LocalDate.now());
            if (age <= 6) continue;
            var plant = culture.getParent().getPlant();
            var key = new OldKey(culture.getSubcultureWeek(), plant.getCode(), displayName(plant), age);
            var totals = oldGroups.computeIfAbsent(key, ignored -> new int[2]);
            totals[0] += 1;
            totals[1] += culture.getPlantCount();
        }
        var oldCultures = oldGroups.entrySet().stream()
            .map(entry -> new OldCulture(entry.getKey().week(), entry.getKey().code(),
                entry.getKey().variety(), entry.getValue()[0], entry.getValue()[1], entry.getKey().age()))
            .sorted(Comparator.comparingLong(OldCulture::ageWeeks).reversed())
            .toList();
        return new DashboardDetails(available, oldCultures);
    }

    private String displayName(Plant plant) {
        return plant.getVariety() == null || plant.getVariety().isBlank()
            ? plant.getName() : plant.getName() + " — " + plant.getVariety();
    }
}
