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
