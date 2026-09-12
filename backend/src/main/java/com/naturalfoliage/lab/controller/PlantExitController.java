package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/plant-exits")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_PLANT_EXIT')")
public class PlantExitController {
    private final PlantExitRepository exits; private final SubcultureRepository subcultures;
    public PlantExitController(PlantExitRepository exits, SubcultureRepository subcultures) { this.exits = exits; this.subcultures = subcultures; }
    public record ExitRequest(@NotBlank String barcode, @Min(1) int quantity,
        @NotBlank @Pattern(regexp = "HARDENING") String destination, String reference) {}
    @GetMapping public List<PlantExit> all(Authentication auth) {
        var all = exits.findAll();
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return all;
        return all.stream().filter(item -> auth.getName().equals(item.getTechnician()))
            .sorted(Comparator.comparing(PlantExit::getId).reversed()).limit(20).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public PlantExit create(@Valid @RequestBody ExitRequest input, Authentication auth) {
        var bottle = subcultures.findByBarcode(input.barcode()).orElseThrow(() -> new IllegalArgumentException("Subculture bottle not found"));
        if (bottle.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Bottle is not active");
        if (input.quantity() > bottle.getPlantCount()) throw new IllegalStateException("Only " + bottle.getPlantCount() + " plants are available in this bottle");
        var plant = bottle.getParent().getPlant();
        bottle.setPlantCount(bottle.getPlantCount() - input.quantity());
        if (bottle.getPlantCount() == 0) bottle.setStatus(BottleStatus.EXITED);
        subcultures.save(bottle);
        var exit = new PlantExit(); exit.setBarcode(input.barcode()); exit.setPlantCode(plant.getCode()); exit.setPlantName(plant.getName());
        exit.setQuantity(input.quantity()); exit.setDestination(input.destination()); exit.setReference(input.reference()); exit.setTechnician(auth.getName());
        return exits.save(exit);
    }
}
