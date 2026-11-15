package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import com.naturalfoliage.lab.service.BarcodeResolverService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/plant-exits")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_PLANT_EXIT')")
public class PlantExitController {
    private final PlantExitRepository exits; private final SubcultureRepository subcultures; private final BarcodeResolverService barcodes;
    public PlantExitController(PlantExitRepository exits, SubcultureRepository subcultures, BarcodeResolverService barcodes) { this.exits = exits; this.subcultures = subcultures; this.barcodes = barcodes; }
    public record ExitRequest(@NotBlank String barcode, @Min(1) int quantity,
        @NotBlank @Pattern(regexp = "HARDENING") String destination, String reference) {}
    public record ExitSummaryRow(String period, String plantCode, String plantName, long exits, long plants) {}
    public record ExitSummary(List<ExitSummaryRow> rows, long exits, long plants) {}
    private record ExitGroup(String period, String plantCode) {}
    private static class ExitTotals { String plantName; long exits; long plants; }

    @GetMapping("/summary")
    public ExitSummary summary(@RequestParam(defaultValue = "month") String interval, Authentication auth) {
        if (!interval.equals("month") && !interval.equals("year"))
            throw new IllegalArgumentException("Group by month or year");
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        var grouped = new HashMap<ExitGroup, ExitTotals>();
        long totalExits = 0;
        long totalPlants = 0;
        for (var exit : exits.findAll()) {
            if (!"HARDENING".equals(exit.getDestination()) || exit.getExitedAt() == null ||
                (!admin && !auth.getName().equals(exit.getTechnician()))) continue;
            LocalDateTime date = exit.getExitedAt();
            String period = interval.equals("year") ? String.valueOf(date.getYear()) :
                "%d-%02d".formatted(date.getYear(), date.getMonthValue());
            var key = new ExitGroup(period, exit.getPlantCode());
            var totals = grouped.computeIfAbsent(key, ignored -> new ExitTotals());
            if (totals.plantName == null) totals.plantName = exit.getPlantName();
            totals.exits++;
            totals.plants += exit.getQuantity();
            totalExits++;
            totalPlants += exit.getQuantity();
        }
        var rows = grouped.entrySet().stream()
            .map(entry -> new ExitSummaryRow(entry.getKey().period(), entry.getKey().plantCode(),
                entry.getValue().plantName, entry.getValue().exits, entry.getValue().plants))
            .sorted(Comparator.comparing(ExitSummaryRow::period).reversed()
                .thenComparing(ExitSummaryRow::plantCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        return new ExitSummary(rows, totalExits, totalPlants);
    }

    @GetMapping public List<PlantExit> all(Authentication auth) {
        var all = exits.findAll();
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return all;
        return all.stream().filter(item -> auth.getName().equals(item.getTechnician()))
            .sorted(Comparator.comparing(PlantExit::getId).reversed()).limit(20).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public PlantExit create(@Valid @RequestBody ExitRequest input, Authentication auth) {
        var bottle = barcodes.findSubculture(input.barcode()).orElseThrow(() -> new IllegalArgumentException("Subculture bottle not found"));
        if (bottle.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
        if (input.quantity() > bottle.getPlantCount()) throw new IllegalStateException("Only " + bottle.getPlantCount() + " plants are available in this bottle");
        var plant = bottle.getParent().getPlant();
        bottle.setPlantCount(bottle.getPlantCount() - input.quantity());
        if (bottle.getPlantCount() == 0) bottle.setStatus(BottleStatus.EXITED);
        subcultures.save(bottle);
        var exit = new PlantExit(); exit.setBarcode(bottle.getBarcode()); exit.setPlantCode(plant.getCode()); exit.setPlantName(plant.getName());
        exit.setQuantity(input.quantity()); exit.setDestination(input.destination()); exit.setReference(input.reference()); exit.setTechnician(auth.getName());
        return exits.save(exit);
    }
}
