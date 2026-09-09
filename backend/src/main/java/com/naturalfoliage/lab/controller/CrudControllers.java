package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

final class CrudControllers { private CrudControllers() {} }

@RestController @RequestMapping("/api/plants")
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
class MediaController {
    private final MediaCompositionRepository repository;
    MediaController(MediaCompositionRepository repository) { this.repository = repository; }
    @GetMapping List<MediaComposition> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) MediaComposition create(@Valid @RequestBody MediaComposition media) { return repository.save(media); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id) { repository.deleteById(id); }
}

@RestController @RequestMapping("/api/mother-bottles")
class MotherBottleController {
    private final MotherBottleRepository repository;
    MotherBottleController(MotherBottleRepository repository) { this.repository = repository; }
    @GetMapping List<MotherBottle> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) MotherBottle create(@Valid @RequestBody MotherBottle bottle) { return repository.save(bottle); }
}

@RestController @RequestMapping("/api/subcultures")
class SubcultureController {
    private final SubcultureRepository repository;
    SubcultureController(SubcultureRepository repository) { this.repository = repository; }
    @GetMapping List<Subculture> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Subculture create(@Valid @RequestBody Subculture culture) {
        if (culture.getParent().getStatus() != BottleStatus.ACTIVE) throw new IllegalStateException("Parent bottle is not active");
        culture.setCycle(culture.getParent().getCycle() + 1);
        return repository.save(culture);
    }
}
