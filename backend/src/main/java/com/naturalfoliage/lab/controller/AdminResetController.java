package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.Role;
import com.naturalfoliage.lab.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/data")
@PreAuthorize("hasRole('ADMIN')")
public class AdminResetController {
    private static final String CONFIRMATION = "DELETE ALL LAB DATA";
    private final SaleInvoiceRepository sales;
    private final DiscardRepository discards;
    private final PlantExitRepository exits;
    private final SubcultureRepository subcultures;
    private final MotherBottleRepository mothers;
    private final MediaPreparationRepository preparations;
    private final PriceItemRepository prices;
    private final MediaCompositionRepository media;
    private final PlantRepository plants;
    private final UserRepository users;

    public AdminResetController(SaleInvoiceRepository sales, DiscardRepository discards,
        PlantExitRepository exits, SubcultureRepository subcultures,
        MotherBottleRepository mothers, MediaPreparationRepository preparations,
        PriceItemRepository prices, MediaCompositionRepository media,
        PlantRepository plants, UserRepository users) {
        this.sales = sales; this.discards = discards; this.exits = exits;
        this.subcultures = subcultures; this.mothers = mothers;
        this.preparations = preparations; this.prices = prices;
        this.media = media; this.plants = plants; this.users = users;
    }

    public record ResetRequest(String confirmation) {}
    public record OperationalResetRequest(String confirmation, Map<String, Long> expectedCounts) {}

    private Map<String, Long> operationalCounts() {
        var counts = new LinkedHashMap<String, Long>();
        counts.put("sales", sales.count());
        counts.put("discards", discards.count());
        counts.put("plantExits", exits.count());
        counts.put("subcultures", subcultures.count());
        counts.put("initiations", mothers.count());
        counts.put("mediaPreparations", preparations.count());
        counts.put("prices", prices.count());
        return counts;
    }

    @GetMapping("/operational-preview")
    public Map<String, Long> operationalPreview() {
        var counts = operationalCounts();
        counts.put("preservedPlants", plants.count());
        counts.put("preservedMediaCompositions", media.count());
        counts.put("preservedStaffAccounts", users.count());
        return counts;
    }

    @DeleteMapping("/operational")
    @Transactional
    public Map<String, Long> clearOperationalData(@RequestBody OperationalResetRequest input) {
        if (input == null || !"CLEAR OPERATIONAL DATA".equals(input.confirmation())) {
            throw new IllegalArgumentException("Operational cleanup confirmation phrase did not match");
        }
        var counts = operationalCounts();
        if (!counts.equals(input.expectedCounts())) {
            throw new IllegalStateException("Operational data changed since preview; cleanup cancelled");
        }
        sales.deleteAll();
        discards.deleteAllInBatch();
        exits.deleteAllInBatch();
        subcultures.deleteAllInBatch();
        mothers.deleteAllInBatch();
        preparations.deleteAllInBatch();
        prices.deleteAllInBatch();
        var compositions = media.findAll();
        compositions.forEach(item -> item.setAvailableBottles(0));
        media.saveAllAndFlush(compositions);
        return counts;
    }

    @DeleteMapping
    @Transactional
    public Map<String, Long> reset(@RequestBody ResetRequest input) {
        if (input == null || !CONFIRMATION.equals(input.confirmation())) {
            throw new IllegalArgumentException("Reset confirmation phrase did not match");
        }
        var admin = users.findByUsername("admin")
            .filter(user -> user.getRole() == Role.ADMIN)
            .orElseThrow(() -> new IllegalStateException("Main admin account was not found; reset cancelled"));

        var deleted = new LinkedHashMap<String, Long>();
        deleted.put("sales", sales.count());
        deleted.put("discards", discards.count());
        deleted.put("plantExits", exits.count());
        deleted.put("subcultures", subcultures.count());
        deleted.put("initiations", mothers.count());
        deleted.put("mediaPreparations", preparations.count());
        deleted.put("prices", prices.count());
        deleted.put("mediaCompositions", media.count());
        deleted.put("plants", plants.count());
        deleted.put("staffAccounts", users.count() - 1);

        sales.deleteAll();
        discards.deleteAllInBatch();
        exits.deleteAllInBatch();
        subcultures.deleteAllInBatch();
        mothers.deleteAllInBatch();
        preparations.deleteAllInBatch();
        prices.deleteAllInBatch();
        media.deleteAllInBatch();
        plants.deleteAllInBatch();
        users.findAll().stream().filter(user -> !user.getId().equals(admin.getId())).forEach(users::delete);
        users.flush();
        return deleted;
    }
}
