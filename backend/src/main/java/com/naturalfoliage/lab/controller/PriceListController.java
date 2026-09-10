package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.PriceItem;
import com.naturalfoliage.lab.repository.PriceItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/price-list")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_PRICE_LIST')")
public class PriceListController {
    private final PriceItemRepository prices;
    public PriceListController(PriceItemRepository prices) { this.prices = prices; }
    @GetMapping public List<PriceItem> all() { return prices.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public PriceItem create(@Valid @RequestBody PriceItem item) {
        item.setLastUpdated(LocalDateTime.now()); return prices.save(item);
    }
    @PutMapping("/{id}") public PriceItem update(@PathVariable Long id, @Valid @RequestBody PriceItem input) {
        var item = prices.findById(id).orElseThrow();
        item.setPlantCode(input.getPlantCode()); item.setVarietyName(input.getVarietyName());
        item.setPrice(input.getPrice()); item.setLastUpdated(LocalDateTime.now()); return prices.save(item);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { prices.deleteById(id); }
}
