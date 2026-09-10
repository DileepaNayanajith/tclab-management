package com.naturalfoliage.lab.config;

import com.naturalfoliage.lab.model.*;
import com.naturalfoliage.lab.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;

@Configuration
@ConditionalOnProperty(name = "app.legacy-data.enabled", havingValue = "true", matchIfMissing = true)
public class LegacyDataSeeder {
    @Bean CommandLineRunner importLegacyLabData(PlantRepository plants,
        MediaCompositionRepository media, MotherBottleRepository mothers,
        SubcultureRepository subcultures) {
        return args -> {
            var phym = plants.findByCode("PHYM").orElseGet(() ->
                plants.save(plant("PHYM", "Philodendron moonlight yellow", "Philodendron")));
            plants.findByCode("PHYCG").orElseGet(() ->
                plants.save(plant("PHYCG", "Philodendron Crocodile Gold", "Philodendron")));

            var hb = media.findByCode("HB").orElseGet(() ->
                media.save(medium("HB", "MS", "BAP 0.05 mg/L")));
            media.findByCode("AQQ").orElseGet(() ->
                media.save(medium("AQQ", "MS", "BAP 1.0 mg/L")));

            var mother = mothers.findByBarcode("PHY1224").orElseGet(() -> {
                var bottle = new MotherBottle();
                bottle.setBarcode("PHY1224"); bottle.setPlant(phym); bottle.setMedia(hb);
                bottle.setPlantCount(0); bottle.setCycle(6); bottle.setTechnician("admin");
                bottle.setCreatedDate(LocalDate.parse("2026-06-07")); bottle.setStatus(BottleStatus.USED);
                bottle.setCultureWeek(24); bottle.setLaminaFlow("LF1"); bottle.setPrinted(true);
                return mothers.save(bottle);
            });

            culture(subcultures, mother, hb, "PHYM-23-001", 4, 2, 23, false, "2026-06-07");
            culture(subcultures, mother, hb, "PHYM-23-002", 4, 2, 23, false, "2026-06-07");
            culture(subcultures, mother, hb, "PHYM-23-003", 4, 2, 23, false, "2026-06-07");
            culture(subcultures, mother, hb, "PHYM-23-004", 4, 2, 23, false, "2026-06-07");
            culture(subcultures, mother, hb, "PHYM-23-005", 4, 2, 23, false, "2026-06-07");
            culture(subcultures, mother, hb, "PHYM-24-001", 4, 3, 24, true, "2026-06-08");
            culture(subcultures, mother, hb, "PHYM-24-002", 5, 4, 24, false, "2026-06-08");
            culture(subcultures, mother, hb, "PHYM-24-003", 5, 5, 24, false, "2026-06-08");
            culture(subcultures, mother, hb, "PHYM-24-004", 1, 6, 24, false, "2026-06-08");
        };
    }

    private Plant plant(String code, String name, String variety) {
        var plant = new Plant(); plant.setCode(code); plant.setName(name); plant.setVariety(variety); return plant;
    }

    private MediaComposition medium(String code, String basalMedia, String hormones) {
        var medium = new MediaComposition(); medium.setCode(code); medium.setBasalMedia(basalMedia); medium.setHormones(hormones); return medium;
    }

    private void culture(SubcultureRepository repository, MotherBottle parent,
        MediaComposition media, String barcode, int plants, int cycle, int week,
        boolean rooting, String date) {
        if (repository.findByBarcode(barcode).isPresent()) return;
        var culture = new Subculture();
        culture.setBarcode(barcode); culture.setParent(parent); culture.setMedia(media);
        culture.setPlantCount(plants); culture.setCycle(cycle); culture.setSubcultureWeek(week);
        culture.setTechnician("admin"); culture.setRooting(rooting);
        culture.setCreatedDate(LocalDate.parse(date)); culture.setStatus(BottleStatus.ACTIVE);
        culture.setOrigin("Subculture"); culture.setLaminaFlow("LF1");
        repository.save(culture);
    }
}
