package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.DiscardRecord;
import com.naturalfoliage.lab.model.Subculture;
import com.naturalfoliage.lab.repository.DiscardRepository;
import com.naturalfoliage.lab.repository.SubcultureRepository;
import com.naturalfoliage.lab.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ACCESS_ANALYTICS')")
public class AnalyticsController {
    private final SubcultureRepository subcultures;
    private final DiscardRepository discards;
    private final UserRepository users;

    public AnalyticsController(SubcultureRepository subcultures, DiscardRepository discards, UserRepository users) {
        this.subcultures = subcultures;
        this.discards = discards;
        this.users = users;
    }

    public record TechnicianOption(String username, String name) {}
    public record Summary(long bottles, long plants, long contaminatedBottles, double contaminationRate) {}
    public record PeriodRow(String period, String technician, String technicianName, long bottles,
                            long plants, long contaminatedBottles, double contaminationRate) {}
    public record ProductionReport(List<TechnicianOption> technicians, List<PeriodRow> rows, Summary summary) {}

    private record GroupKey(String period, String technician) {}
    private static class Totals {
        long bottles;
        long plants;
        long contaminatedBottles;

        void add(Subculture bottle, boolean contaminated) {
            bottles++;
            plants += bottle.getPlantCount();
            if (contaminated) contaminatedBottles++;
        }

        Summary summary() {
            return new Summary(bottles, plants, contaminatedBottles, rate(contaminatedBottles, bottles));
        }
    }

    @GetMapping("/production")
    public ProductionReport production(@RequestParam(defaultValue = "day") String interval,
                                       @RequestParam(defaultValue = "") String technician,
                                       @RequestParam(required = false) LocalDate from,
                                       @RequestParam(required = false) LocalDate to) {
        if (!List.of("day", "week", "month").contains(interval)) {
            throw new IllegalArgumentException("Interval must be day, week, or month");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be on or before to date");
        }

        var names = new HashMap<String, String>();
        users.findAll().forEach(user -> names.put(user.getUsername(), user.getFullName()));
        var all = subcultures.findAll();
        all.forEach(bottle -> names.putIfAbsent(bottle.getTechnician(), bottle.getTechnician()));
        var options = names.entrySet().stream()
            .map(entry -> new TechnicianOption(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(TechnicianOption::name, String.CASE_INSENSITIVE_ORDER))
            .toList();

        Map<String, DiscardRecord> discardedByBarcode = new HashMap<>();
        discards.findAll().forEach(record -> discardedByBarcode.put(record.getBarcode(), record));
        var groups = new LinkedHashMap<GroupKey, Totals>();
        var overall = new Totals();
        for (var bottle : all) {
            LocalDate created = bottle.getCreatedDate();
            if (created == null || (from != null && created.isBefore(from)) ||
                (to != null && created.isAfter(to)) ||
                (!technician.isBlank() && !technician.equals(bottle.getTechnician()))) continue;
            boolean contaminated = isContamination(discardedByBarcode.get(bottle.getBarcode()));
            groups.computeIfAbsent(new GroupKey(period(created, interval), bottle.getTechnician()),
                ignored -> new Totals()).add(bottle, contaminated);
            overall.add(bottle, contaminated);
        }
        var rows = new ArrayList<PeriodRow>();
        groups.forEach((key, total) -> rows.add(new PeriodRow(key.period(), key.technician(),
            names.getOrDefault(key.technician(), key.technician()), total.bottles, total.plants,
            total.contaminatedBottles, rate(total.contaminatedBottles, total.bottles))));
        rows.sort(Comparator.comparing(PeriodRow::period).reversed()
            .thenComparing(PeriodRow::technicianName, String.CASE_INSENSITIVE_ORDER));
        return new ProductionReport(options, rows, overall.summary());
    }

    @GetMapping("/discards")
    public List<DiscardRecord> allDiscards() {
        return discards.findAll();
    }

    private static boolean isContamination(DiscardRecord record) {
        if (record == null || record.getReason() == null) return false;
        String reason = record.getReason().toLowerCase(Locale.ROOT);
        return reason.contains("bacter") || reason.contains("fung") || reason.contains("mold") ||
            reason.contains("mould") || reason.contains("mite") || reason.contains("contaminat");
    }

    private static String period(LocalDate date, String interval) {
        return switch (interval) {
            case "week" -> "%d-W%02d".formatted(date.get(WeekFields.ISO.weekBasedYear()),
                date.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case "month" -> "%d-%02d".formatted(date.getYear(), date.getMonthValue());
            default -> date.toString();
        };
    }

    private static double rate(long contaminated, long bottles) {
        return bottles == 0 ? 0 : Math.round(1000.0 * contaminated / bottles) / 10.0;
    }
}
