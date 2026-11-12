package com.naturalfoliage.lab;

import com.naturalfoliage.lab.model.DiscardRecord;
import com.naturalfoliage.lab.model.BottleStatus;
import com.naturalfoliage.lab.model.MotherBottle;
import com.naturalfoliage.lab.model.Plant;
import com.naturalfoliage.lab.model.Subculture;
import com.naturalfoliage.lab.repository.DiscardRepository;
import com.naturalfoliage.lab.repository.MediaCompositionRepository;
import com.naturalfoliage.lab.repository.MotherBottleRepository;
import com.naturalfoliage.lab.repository.PlantRepository;
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
    @Autowired PlantRepository plants;

    @Test
    void analyticsPermissionGrantsReportsButNotWorkflowRecords() throws Exception {
        var analyst = user("analyst").authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_ANALYTICS"));
        mvc.perform(get("/api/analytics/production")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/analytics/production").with(user("staff").roles("TECHNICIAN")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/production").with(analyst)).andExpect(status().isOk());
        mvc.perform(get("/api/analytics/output").with(user("staff").roles("TECHNICIAN")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/output").with(analyst)).andExpect(status().isOk());
        mvc.perform(get("/api/analytics/discards").with(analyst)).andExpect(status().isOk());
        mvc.perform(get("/api/discards").with(analyst)).andExpect(status().isForbidden());
        mvc.perform(get("/api/subcultures").with(analyst)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void monthlyAndYearlyOutputKeepInitiationAndSubcultureSeparate() throws Exception {
        var template = mothers.findAll().getFirst();
        var medium = media.findAll().getFirst();
        var december = initiation(template, medium, LocalDate.parse("2030-12-31"), 3);
        var january = initiation(template, medium, LocalDate.parse("2031-01-01"), 5);
        initiation(template, medium, LocalDate.parse("2031-01-02"), 7);
        var secondPlant = new Plant();
        secondPlant.setCode("VAR-" + UUID.randomUUID());
        secondPlant.setName("ZZZ Test variety");
        secondPlant.setVariety("Test cultivar");
        secondPlant = plants.save(secondPlant);
        var otherInitiation = initiation(secondPlant, medium, LocalDate.parse("2031-01-02"), 2);
        var discardedLater = bottle("output-test", LocalDate.parse("2031-01-03"), 11, january, medium);
        discardedLater.setStatus(BottleStatus.DISCARDED);
        subcultures.save(discardedLater);
        bottle("output-test", LocalDate.parse("2031-02-01"), 13, december, medium);
        bottle("output-test", LocalDate.parse("2031-01-04"), 3, otherInitiation, medium);

        var analyst = user("analyst").authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_ANALYTICS"));
        mvc.perform(get("/api/analytics/output").with(analyst).param("interval", "month"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].period").value("2031-02"))
            .andExpect(jsonPath("$.rows[0].initiationBottles").value(0))
            .andExpect(jsonPath("$.rows[0].subculturePlants").value(13))
            .andExpect(jsonPath("$.rows[1].period").value("2031-01"))
            .andExpect(jsonPath("$.rows[1].plantId").value(template.getPlant().getId().intValue()))
            .andExpect(jsonPath("$.rows[1].plantName").value(template.getPlant().getName()))
            .andExpect(jsonPath("$.rows[1].initiationBottles").value(2))
            .andExpect(jsonPath("$.rows[1].initiationPlants").value(12))
            .andExpect(jsonPath("$.rows[1].subcultureBottles").value(1))
            .andExpect(jsonPath("$.rows[1].subculturePlants").value(11))
            .andExpect(jsonPath("$.rows[2].period").value("2031-01"))
            .andExpect(jsonPath("$.rows[2].plantId").value(secondPlant.getId().intValue()))
            .andExpect(jsonPath("$.rows[2].plantVariety").value("Test cultivar"))
            .andExpect(jsonPath("$.rows[2].initiationPlants").value(2))
            .andExpect(jsonPath("$.rows[2].subculturePlants").value(3));
        mvc.perform(get("/api/analytics/output").with(analyst).param("interval", "year"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].period").value("2031"))
            .andExpect(jsonPath("$.rows[0].initiationPlants").value(12))
            .andExpect(jsonPath("$.rows[0].subculturePlants").value(24))
            .andExpect(jsonPath("$.rows[1].period").value("2031"))
            .andExpect(jsonPath("$.rows[1].plantId").value(secondPlant.getId().intValue()))
            .andExpect(jsonPath("$.rows[1].initiationPlants").value(2))
            .andExpect(jsonPath("$.rows[1].subculturePlants").value(3))
            .andExpect(jsonPath("$.rows[2].period").value("2030"))
            .andExpect(jsonPath("$.rows[2].initiationPlants").value(3));
    }

    private MotherBottle initiation(MotherBottle template,
                                    com.naturalfoliage.lab.model.MediaComposition medium,
                                    LocalDate date, int plants) {
        return initiation(template.getPlant(), medium, date, plants);
    }

    private MotherBottle initiation(Plant plant,
                                    com.naturalfoliage.lab.model.MediaComposition medium,
                                    LocalDate date, int plants) {
        var bottle = new MotherBottle();
        bottle.setBarcode("OUTPUT-" + UUID.randomUUID());
        bottle.setPlant(plant);
        bottle.setMedia(medium);
        bottle.setTechnician("output-test");
        bottle.setCreatedDate(date);
        bottle.setPlantCount(plants);
        return mothers.save(bottle);
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
