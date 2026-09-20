package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Comparator;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import jakarta.transaction.Transactional;

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
class MediaController {
    private final MediaCompositionRepository repository;
    MediaController(MediaCompositionRepository repository) { this.repository = repository; }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    List<MediaComposition> all() { return repository.findAll(); }
    record MediaLookup(Long id, String code, String basalMedia, Double ph, Double agar, int availableBottles) {}
    @GetMapping("/options") @PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_MEDIA')")
    List<MediaLookup> options() { return repository.findAll().stream()
        .map(item -> new MediaLookup(item.getId(), item.getCode(), item.getBasalMedia(), item.getPh(), item.getAgar(), item.getAvailableBottles())).toList(); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    MediaComposition create(@Valid @RequestBody MediaComposition media) { return repository.save(media); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    MediaComposition update(@PathVariable Long id, @Valid @RequestBody MediaComposition input) {
        var item = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Media composition not found"));
        item.setCode(input.getCode());
        item.setBasalMedia(input.getBasalMedia());
        item.setHormones(input.getHormones());
        item.setPh(input.getPh());
        item.setAgar(input.getAgar());
        return repository.save(item);
    }
    record StockRequest(@Min(0) int availableBottles) {}
    @PatchMapping("/{id}/stock") @PreAuthorize("hasRole('ADMIN')") MediaComposition stock(@PathVariable Long id, @Valid @RequestBody StockRequest input) {
        var item = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Media composition not found"));
        item.setAvailableBottles(input.availableBottles()); return repository.save(item);
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id) { repository.deleteById(id); }
}

@RestController @RequestMapping("/api/media-preparations")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_MEDIA')")
class MediaPreparationController {
    private final MediaPreparationRepository repository; private final MediaCompositionRepository media;
    MediaPreparationController(MediaPreparationRepository repository, MediaCompositionRepository media) { this.repository = repository; this.media = media; }
    record PreparationRequest(@NotNull Long mediaId, @Min(1) int bottleCount,
        @DecimalMin("0.0") Double ph, @DecimalMin("0.0") Double agar,
        @NotBlank @Pattern(regexp = "AUTOCLAVE|CSUP") String sterilizationMethod) {}
    record PreparationUpdateRequest(@Min(1) int bottleCount,
        @DecimalMin("0.0") Double ph, @DecimalMin("0.0") Double agar,
        @NotBlank @Pattern(regexp = "AUTOCLAVE|CSUP") String sterilizationMethod) {}
    @GetMapping List<MediaPreparation> all(Authentication auth) {
        var all = repository.findAll();
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return all;
        return all.stream().filter(item -> auth.getName().equals(item.getTechnician()))
            .sorted(Comparator.comparing(MediaPreparation::getId).reversed()).limit(20).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional MediaPreparation create(@Valid @RequestBody PreparationRequest input, Authentication auth) {
        var composition = media.findById(input.mediaId()).orElseThrow(() -> new IllegalArgumentException("Media composition not found"));
        composition.setAvailableBottles(Math.addExact(composition.getAvailableBottles(), input.bottleCount()));
        media.save(composition);
        var preparation = new MediaPreparation(); preparation.setMedia(composition);
        preparation.setBottleCount(input.bottleCount());
        preparation.setPh(input.ph() == null ? composition.getPh() : input.ph());
        preparation.setAgar(input.agar() == null ? composition.getAgar() : input.agar());
        preparation.setSterilizationMethod(input.sterilizationMethod());
        preparation.setTechnician(auth.getName());
        return repository.save(preparation);
    }
    @PutMapping("/{id}") @Transactional
    MediaPreparation update(@PathVariable Long id, @Valid @RequestBody PreparationUpdateRequest input, Authentication auth) {
        var preparation = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Media preparation was not found"));
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !auth.getName().equals(preparation.getTechnician()))
            throw new IllegalStateException("You can edit only your own media preparations");
        if (!admin && input.bottleCount() != preparation.getBottleCount())
            throw new IllegalStateException("Administrator authority is required to change a past bottle quantity");
        if (admin && input.bottleCount() != preparation.getBottleCount()) {
            var composition = preparation.getMedia();
            int adjustedStock = composition.getAvailableBottles() + input.bottleCount() - preparation.getBottleCount();
            if (adjustedStock < 0) throw new IllegalStateException("Bottle quantity cannot be reduced below stock already used");
            composition.setAvailableBottles(adjustedStock); media.save(composition);
            preparation.setBottleCount(input.bottleCount());
        }
        preparation.setPh(input.ph()); preparation.setAgar(input.agar());
        preparation.setSterilizationMethod(input.sterilizationMethod());
        return repository.save(preparation);
    }
}

@RestController @RequestMapping("/api/mother-bottles")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_MOTHER_BOTTLES')")
class MotherBottleController {
    private final MotherBottleRepository repository; private final PlantRepository plants; private final MediaCompositionRepository media;
    MotherBottleController(MotherBottleRepository repository, PlantRepository plants, MediaCompositionRepository media) { this.repository = repository; this.plants = plants; this.media = media; }
    record MotherRequest(@NotNull Long plantId, @NotNull Long mediaId,
        @Min(1) int plantCount, @Min(0) int cycle, String laminaFlow) {}
    @GetMapping List<MotherBottle> all() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) MotherBottle create(@Valid @RequestBody MotherRequest input, Authentication auth) {
        var plant = plants.findById(input.plantId()).orElseThrow(() -> new IllegalArgumentException("Plant not found"));
        var bottle = new MotherBottle(); bottle.setBarcode(nextBarcode(plant.getCode()));
        bottle.setPlant(plant);
        bottle.setMedia(media.findById(input.mediaId()).orElseThrow(() -> new IllegalArgumentException("Media not found")));
        bottle.setPlantCount(input.plantCount()); bottle.setCycle(input.cycle());
        bottle.setCultureWeek(LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear()));
        bottle.setLaminaFlow(input.laminaFlow()); bottle.setTechnician(auth.getName()); return repository.save(bottle);
    }

    @GetMapping("/next-barcode") String nextBarcodePreview(@RequestParam Long plantId) {
        var plant = plants.findById(plantId).orElseThrow(() -> new IllegalArgumentException("Plant not found"));
        return nextBarcode(plant.getCode());
    }

    private String nextBarcode(String plantCode) {
        var now = LocalDate.now();
        var prefix = "%s-INIT-%02d%02d".formatted(plantCode.toUpperCase(), now.getYear() % 100,
            now.get(WeekFields.ISO.weekOfWeekBasedYear()));
        int sequence = 1;
        while (repository.findByBarcode("%s-%03d".formatted(prefix, sequence)).isPresent()) sequence++;
        return "%s-%03d".formatted(prefix, sequence);
    }
}

@RestController @RequestMapping("/api/subcultures")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_SUBCULTURES')")
class SubcultureController {
    private final SubcultureRepository repository; private final MotherBottleRepository mothers; private final MediaCompositionRepository media; private final UserRepository users;
    SubcultureController(SubcultureRepository repository, MotherBottleRepository mothers, MediaCompositionRepository media, UserRepository users) { this.repository = repository; this.mothers = mothers; this.media = media; this.users = users; }
    record LineRequest(@Min(1) int bottleCount, @Min(1) int plantsPerBottle, @Pattern(regexp = "MULTIPLY|ROOTING") String cultureType) {}
    record SubcultureRequest(@NotBlank String parentBarcode, @NotNull Long mediaId,
        @NotEmpty List<@Valid LineRequest> lines, @NotBlank String laminaFlow, String technicianUsername) {}
    @GetMapping List<Subculture> all(Authentication auth) {
        var all = repository.findAll();
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return all;
        return all.stream().filter(item -> auth.getName().equals(item.getTechnician()))
            .sorted(Comparator.comparing(Subculture::getId).reversed()).limit(20).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    List<Subculture> create(@Valid @RequestBody SubcultureRequest input, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        String technician = auth.getName();
        if (admin && input.technicianUsername() != null && !input.technicianUsername().isBlank()) {
            technician = users.findByUsername(input.technicianUsername()).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Selected technician was not found or is inactive")).getUsername();
        }
        var scannedSubculture = repository.findByBarcode(input.parentBarcode());
        var parent = scannedSubculture.map(Subculture::getParent).orElseGet(() -> mothers.findByBarcode(input.parentBarcode())
            .orElseThrow(() -> new IllegalArgumentException("Scanned parent barcode was not found")));
        var parentStatus = scannedSubculture.map(Subculture::getStatus).orElse(parent.getStatus());
        if (parentStatus != BottleStatus.ACTIVE) throw new IllegalStateException("Scanned parent bottle is not active");
        int parentCycle = scannedSubculture.map(Subculture::getCycle).orElse(parent.getCycle());
        int cycle = parentCycle + 1;
        int totalBottles = input.lines().stream().mapToInt(LineRequest::bottleCount).sum();
        var selectedMedia = media.findById(input.mediaId()).orElseThrow(() -> new IllegalArgumentException("Media not found"));
        if (selectedMedia.getAvailableBottles() < totalBottles) throw new IllegalStateException("Not enough media bottles. Available: " + selectedMedia.getAvailableBottles());
        selectedMedia.setAvailableBottles(selectedMedia.getAvailableBottles() - totalBottles); media.save(selectedMedia);
        var now = LocalDate.now(); int week = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        scannedSubculture.ifPresent(item -> item.setStatus(BottleStatus.USED));
        if (scannedSubculture.isEmpty()) parent.setStatus(BottleStatus.USED);
        var created = new java.util.ArrayList<Subculture>();
        var barcodePrefix = barcodePrefix(parent.getPlant().getCode(), cycle, week);
        int sequence = nextSequence(barcodePrefix);
        for (var line : input.lines()) for (int bottle = 0; bottle < line.bottleCount(); bottle++) {
            var culture = new Subculture(); culture.setBarcode("%s-%03d".formatted(barcodePrefix, sequence++)); culture.setParent(parent);
            culture.setMedia(selectedMedia); culture.setPlantCount(line.plantsPerBottle()); culture.setCycle(cycle); culture.setSubcultureWeek(week);
            culture.setRooting("ROOTING".equals(line.cultureType())); culture.setLaminaFlow(input.laminaFlow().trim()); culture.setOrigin("Subculture"); culture.setTechnician(technician);
            created.add(repository.save(culture));
        }
        repository.flush();
        return created;
    }

    private String barcodePrefix(String plantCode, int cycle, int week) {
        return "%s-%02d%02d-C%d".formatted(plantCode.toUpperCase(), LocalDate.now().getYear() % 100, week, cycle);
    }

    private int nextSequence(String prefix) {
        int sequence = 1;
        while (repository.findByBarcode("%s-%03d".formatted(prefix, sequence)).isPresent()) sequence++;
        return sequence;
    }
}
