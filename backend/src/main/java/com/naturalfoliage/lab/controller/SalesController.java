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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_SALES')")
public class SalesController {
    private final SaleInvoiceRepository invoices;
    private final SubcultureRepository subcultures;
    private final PriceItemRepository prices;

    public SalesController(SaleInvoiceRepository invoices, SubcultureRepository subcultures,
        PriceItemRepository prices) {
        this.invoices = invoices; this.subcultures = subcultures; this.prices = prices;
    }

    public record RootedStock(String plantCode, String plantName, int rootedQuantity,
        int sellableQuantity, double unitPrice) {}
    public record SaleLine(@NotBlank String plantCode, @Min(1) int quantity) {}
    public record SaleRequest(@NotBlank String customerName, String customerContact,
        @NotEmpty List<@Valid SaleLine> items) {}

    @GetMapping("/inventory")
    public List<RootedStock> inventory() {
        record StockKey(String code, String name) {}
        var grouped = new LinkedHashMap<StockKey, Integer>();
        subcultures.findAll().stream()
            .filter(item -> item.getStatus() == BottleStatus.ACTIVE && item.isRooting())
            .forEach(item -> {
                var plant = item.getParent().getPlant();
                var key = new StockKey(plant.getCode(), plant.getName());
                grouped.merge(key, item.getPlantCount(), Integer::sum);
            });
        return grouped.entrySet().stream().map(entry -> {
            var price = prices.findByPlantCode(entry.getKey().code()).map(PriceItem::getPrice).orElse(0.0);
            return new RootedStock(entry.getKey().code(), entry.getKey().name(), entry.getValue(),
                (int) Math.floor(entry.getValue() * 0.8), price);
        }).sorted(Comparator.comparing(RootedStock::plantCode)).toList();
    }

    @GetMapping
    public List<SaleInvoice> all() {
        return invoices.findAll().stream()
            .sorted(Comparator.comparing(SaleInvoice::getId).reversed()).toList();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public SaleInvoice sell(@Valid @RequestBody SaleRequest input, Authentication auth) {
        var available = inventory().stream().collect(java.util.stream.Collectors.toMap(
            RootedStock::plantCode, item -> item));
        var invoice = new SaleInvoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setCustomerName(input.customerName());
        invoice.setCustomerContact(input.customerContact());
        invoice.setSoldBy(auth.getName());
        double total = 0;

        for (var line : input.items()) {
            var stock = Optional.ofNullable(available.get(line.plantCode()))
                .orElseThrow(() -> new IllegalArgumentException("No rooted stock for " + line.plantCode()));
            if (line.quantity() > stock.sellableQuantity()) {
                throw new IllegalStateException("Only " + stock.sellableQuantity() + " sellable plants are available for " + line.plantCode());
            }
            available.put(line.plantCode(), new RootedStock(stock.plantCode(), stock.plantName(),
                stock.rootedQuantity() - line.quantity(), stock.sellableQuantity() - line.quantity(), stock.unitPrice()));
            deductRootedStock(line.plantCode(), line.quantity());
            var item = new SaleInvoiceItem();
            item.setPlantCode(stock.plantCode()); item.setPlantName(stock.plantName());
            item.setQuantity(line.quantity()); item.setUnitPrice(stock.unitPrice());
            item.setLineTotal(stock.unitPrice() * line.quantity());
            total += item.getLineTotal(); invoice.addItem(item);
        }
        invoice.setTotalAmount(total);
        return invoices.save(invoice);
    }

    private void deductRootedStock(String plantCode, int requested) {
        int remaining = requested;
        var bottles = subcultures.findAll().stream()
            .filter(item -> item.getStatus() == BottleStatus.ACTIVE && item.isRooting()
                && item.getParent().getPlant().getCode().equalsIgnoreCase(plantCode))
            .sorted(Comparator.comparing(Subculture::getCreatedDate).thenComparing(Subculture::getId))
            .toList();
        for (var bottle : bottles) {
            if (remaining == 0) break;
            int quantity = Math.min(remaining, bottle.getPlantCount());
            bottle.setPlantCount(bottle.getPlantCount() - quantity);
            if (bottle.getPlantCount() == 0) bottle.setStatus(BottleStatus.EXITED);
            subcultures.save(bottle); remaining -= quantity;
        }
    }

    private String nextInvoiceNumber() {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + String.format("%04d", invoices.count() + 1);
    }
}
