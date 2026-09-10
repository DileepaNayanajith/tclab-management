package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

final class CrudControllers { private CrudControllers() {} }

@RestController @RequestMapping("/api/plants")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_PLANTS')")
class PlantController {
    private final PlantRepository repository;
    PlantController(PlantRepository repository) { this.repository = repository; }
    @GetMapping List<Plant> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Plant create(@Valid @RequestBody Plant plant) { return repository.save(plant); }
    @PutMapping("/{id}") Plant update(@PathVariable Long id, @Valid @RequestBody Plant input) {
        var plant = repository.findById(id).orElseThrow();
        plant.setCode(input.getCode()); plant.setName(input.getName()); plant.setVariety(input.getVariety()); plant.setDescription(input.getDescription());
        return repository.save(plant);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id) { repository.deleteById(id); }
}

@RestController @RequestMapping("/api/media")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_MEDIA')")
class MediaController {
    private final MediaCompositionRepository repository;
    MediaController(MediaCompositionRepository repository) { this.repository = repository; }
    @GetMapping List<MediaComposition> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) MediaComposition create(@Valid @RequestBody MediaComposition media) { return repository.save(media); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id) { repository.deleteById(id); }
}

@RestController @RequestMapping("/api/mother-bottles")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_MOTHER_BOTTLES')")
class MotherBottleController {
    private final MotherBottleRepository repository; private final PlantRepository plants; private final MediaCompositionRepository media;
    MotherBottleController(MotherBottleRepository repository, PlantRepository plants, MediaCompositionRepository media) { this.repository = repository; this.plants = plants; this.media = media; }
    record MotherRequest(@NotBlank String barcode, @NotNull Long plantId, @NotNull Long mediaId,
        @Min(1) int plantCount, @Min(0) int cycle, @Min(1) int cultureWeek, String laminaFlow) {}
    @GetMapping List<MotherBottle> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) MotherBottle create(@Valid @RequestBody MotherRequest input, Authentication auth) {
        var bottle = new MotherBottle(); bottle.setBarcode(input.barcode().trim());
        bottle.setPlant(plants.findById(input.plantId()).orElseThrow(() -> new IllegalArgumentException("Plant not found")));
        bottle.setMedia(media.findById(input.mediaId()).orElseThrow(() -> new IllegalArgumentException("Media not found")));
        bottle.setPlantCount(input.plantCount()); bottle.setCycle(input.cycle()); bottle.setCultureWeek(input.cultureWeek());
        bottle.setLaminaFlow(input.laminaFlow()); bottle.setTechnician(auth.getName()); return repository.save(bottle);
    }
}

@RestController @RequestMapping("/api/subcultures")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_SUBCULTURES')")
class SubcultureController {
    private final SubcultureRepository repository; private final MotherBottleRepository mothers; private final MediaCompositionRepository media;
    SubcultureController(SubcultureRepository repository, MotherBottleRepository mothers, MediaCompositionRepository media) { this.repository = repository; this.mothers = mothers; this.media = media; }
    record SubcultureRequest(@NotBlank String barcode, @NotNull Long parentId, @NotNull Long mediaId,
        @Min(1) int plantCount, @Min(1) int subcultureWeek, boolean rooting, String laminaFlow) {}
    @GetMapping List<Subculture> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Subculture create(@Valid @RequestBody SubcultureRequest input, Authentication auth) {
        var parent = mothers.findById(input.parentId()).orElseThrow(() -> new IllegalArgumentException("Mother bottle not found"));
        if (parent.getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Parent bottle is not active");
        var culture = new Subculture(); culture.setBarcode(input.barcode().trim()); culture.setParent(parent);
        culture.setMedia(media.findById(input.mediaId()).orElseThrow(() -> new IllegalArgumentException("Media not found")));
        culture.setPlantCount(input.plantCount()); culture.setCycle(parent.getCycle() + 1); culture.setSubcultureWeek(input.subcultureWeek());
        culture.setRooting(input.rooting()); culture.setLaminaFlow(input.laminaFlow()); culture.setOrigin("Subculture"); culture.setTechnician(auth.getName());
        return repository.save(culture);
    }
}
