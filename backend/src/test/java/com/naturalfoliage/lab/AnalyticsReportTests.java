package com.naturalfoliage.lab;

import com.naturalfoliage.lab.model.DiscardRecord;
import com.naturalfoliage.lab.model.Subculture;
import com.naturalfoliage.lab.repository.DiscardRepository;
import com.naturalfoliage.lab.repository.MediaCompositionRepository;
import com.naturalfoliage.lab.repository.MotherBottleRepository;
import com.naturalfoliage.lab.repository.SubcultureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:analyticsreport;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AnalyticsReportTests {
    @Autowired MockMvc mvc;
    @Autowired SubcultureRepository subcultures;
    @Autowired DiscardRepository discards;
    @Autowired MotherBottleRepository mothers;
    @Autowired MediaCompositionRepository media;

    @Test
    void analyticsPermissionGrantsReportsButNotWorkflowRecords() throws Exception {
        var analyst = user("analyst").authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_ANALYTICS"));
        mvc.perform(get("/api/analytics/production")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/analytics/production").with(user("staff").roles("TECHNICIAN")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/production").with(analyst)).andExpect(status().isOk());
        mvc.perform(get("/api/analytics/discards").with(analyst)).andExpect(status().isOk());
        mvc.perform(get("/api/discards").with(analyst)).andExpect(status().isForbidden());
        mvc.perform(get("/api/subcultures").with(analyst)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void reportUsesCreatedBottleCohortsAndExcludesNonContaminationDiscards() throws Exception {
        String technician = "report-" + UUID.randomUUID();
        var parent = mothers.findAll().getFirst();
        var medium = media.findAll().getFirst();
        var first = bottle(technician, LocalDate.parse("2026-10-17"), 4, parent, medium);
        var second = bottle(technician, LocalDate.parse("2026-10-17"), 6, parent, medium);
        var third = bottle(technician, LocalDate.parse("2026-10-18"), 3, parent, medium);
        discard(first, "Bacterial contamination", LocalDate.parse("2026-10-19"));
        discard(second, "Plant dead", LocalDate.parse("2026-10-19"));

        var analyst = user("analyst").authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_ANALYTICS"));
        mvc.perform(get("/api/analytics/production").with(analyst)
                .param("technician", technician).param("interval", "day")
                .param("from", "2026-10-17").param("to", "2026-10-17"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.bottles").value(2))
            .andExpect(jsonPath("$.summary.plants").value(10))
            .andExpect(jsonPath("$.summary.contaminatedBottles").value(1))
            .andExpect(jsonPath("$.summary.contaminationRate").value(50.0))
            .andExpect(jsonPath("$.rows[0].period").value("2026-10-17"));
        mvc.perform(get("/api/analytics/production").with(analyst)
                .param("technician", technician).param("interval", "week"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.bottles").value(3))
            .andExpect(jsonPath("$.summary.plants").value(13))
            .andExpect(jsonPath("$.summary.contaminationRate").value(33.3));
        mvc.perform(get("/api/analytics/production").with(analyst)
                .param("technician", technician).param("interval", "month"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].period").value("2026-10"));
    }

    private Subculture bottle(String technician, LocalDate date, int plants,
                              com.naturalfoliage.lab.model.MotherBottle parent,
                              com.naturalfoliage.lab.model.MediaComposition medium) {
        var bottle = new Subculture();
        bottle.setBarcode("REPORT-" + UUID.randomUUID());
        bottle.setParent(parent);
        bottle.setMedia(medium);
        bottle.setTechnician(technician);
        bottle.setCreatedDate(date);
        bottle.setPlantCount(plants);
        return subcultures.save(bottle);
    }

    private void discard(Subculture bottle, String reason, LocalDate date) {
        var record = new DiscardRecord();
        record.setBarcode(bottle.getBarcode());
        record.setReason(reason);
        record.setDiscardedDate(date);
        discards.save(record);
    }
}
