package com.naturalfoliage.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalfoliage.lab.model.BottleStatus;
import com.naturalfoliage.lab.model.DiscardRecord;
import com.naturalfoliage.lab.model.MotherBottle;
import com.naturalfoliage.lab.model.Plant;
import com.naturalfoliage.lab.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WorkflowCalculationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PlantRepository plants;
    @Autowired MediaCompositionRepository media;
    @Autowired MotherBottleRepository mothers;
    @Autowired SubcultureRepository subcultures;
    @Autowired DiscardRepository discards;

    @Test
    void availablePlantsExcludesDiscardedBottlesEvenWhenLegacyStatusIsActive() throws Exception {
        var plant = new Plant();
        plant.setCode("DISCARD-" + UUID.randomUUID().toString().substring(0, 8));
        plant.setName("Discard dashboard test");
        var savedPlant = plants.save(plant);

        var mediaItem = media.findAll().getFirst();
        var discarded = new MotherBottle();
        discarded.setBarcode("DISCARDED-" + UUID.randomUUID());
        discarded.setPlant(savedPlant);
        discarded.setMedia(mediaItem);
        discarded.setPlantCount(7);
        discarded.setTechnician("admin");
        mothers.save(discarded);

        var history = new DiscardRecord();
        history.setBarcode(discarded.getBarcode());
        history.setReason("Legacy discard");
        discards.save(history);

        var available = new MotherBottle();
        available.setBarcode("AVAILABLE-" + UUID.randomUUID());
        available.setPlant(savedPlant);
        available.setMedia(mediaItem);
        available.setPlantCount(3);
        available.setTechnician("admin");
        mothers.save(available);

        var response = mvc.perform(get("/api/dashboard/details").with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matching = StreamSupport.stream(json.readTree(response).get("availablePlants").spliterator(), false)
            .filter(row -> savedPlant.getCode().equals(row.get("plantCode").asText())).toList();
        assertThat(matching).hasSize(1);
        assertThat(matching.getFirst().get("total").asInt()).isEqualTo(3);

        available.setStatus(BottleStatus.DISCARDED);
        mothers.save(available);
        response = mvc.perform(get("/api/dashboard/details").with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(StreamSupport.stream(json.readTree(response).get("availablePlants").spliterator(), false)
            .noneMatch(row -> savedPlant.getCode().equals(row.get("plantCode").asText()))).isTrue();
    }

    @Test
    void frontendLoadsWithoutBrowserAuthenticationChallenge() throws Exception {
        mvc.perform(get("/"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401))
            .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void preparationSubcultureExitAndDiscardKeepInventoryConsistent() throws Exception {
        var mediaResponse = mvc.perform(post("/api/media").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"CALC-TEST","basalMedia":"MS","hormones":"BAP 1 mg/L","ph":5.8,"agar":8}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long mediaId = json.readTree(mediaResponse).get("id").asLong();

        mvc.perform(post("/api/media-preparations").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"mediaId":%d,"bottleCount":10,"sterilizationMethod":"AUTOCLAVE"}
                    """.formatted(mediaId)))
            .andExpect(status().isCreated());
        assertThat(media.findById(mediaId).orElseThrow().getAvailableBottles()).isEqualTo(10);

        long plantsBeforeInitiation = dashboardPlantTotal();
        long plantId = plants.findAll().getFirst().getId();
        var initiationResponse = mvc.perform(post("/api/mother-bottles").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"plantId":%d,"mediaId":%d,"plantCount":6,"cycle":0,"laminaFlow":"LF-01"}
                    """.formatted(plantId, mediaId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String parentBarcode = json.readTree(initiationResponse).get("barcode").asText();
        long initiationId = json.readTree(initiationResponse).get("id").asLong();
        String shortScanCode = "91%08d".formatted(initiationId);
        assertThat(dashboardPlantTotal()).isEqualTo(plantsBeforeInitiation + 6);

        mvc.perform(get("/api/workflow/scan/{barcode}", shortScanCode).with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.barcode").value(parentBarcode));

        var subcultureResponse = mvc.perform(post("/api/subcultures").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentBarcode":"%s","mediaId":%d,"laminaFlow":"LF-01","technicianUsername":"admin","lines":[{"bottleCount":2,"plantsPerBottle":3,"cultureType":"ROOTING"}]}
                    """.formatted(shortScanCode, mediaId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var created = json.readTree(subcultureResponse);
        String exitBarcode = created.get(0).get("barcode").asText();
        String discardBarcode = created.get(1).get("barcode").asText();
        String exitScanCode = "92%08d".formatted(created.get(0).get("id").asLong());
        String discardScanCode = "92%08d".formatted(created.get(1).get("id").asLong());
        assertThat(media.findById(mediaId).orElseThrow().getAvailableBottles()).isEqualTo(8);
        assertThat(mothers.findByBarcode(parentBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.USED);
        assertThat(dashboardPlantTotal()).isEqualTo(plantsBeforeInitiation + 6);

        mvc.perform(get("/api/workflow/scan/{barcode}", exitScanCode).with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.barcode").value(exitBarcode));

        mvc.perform(post("/api/subcultures").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentBarcode":"%s","mediaId":%d,"laminaFlow":"LF-01","technicianUsername":"admin","lines":[{"bottleCount":1,"plantsPerBottle":1,"cultureType":"MULTIPLY"}]}
                    """.formatted(shortScanCode, mediaId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("This bottle has already been subcultured. Scan an active bottle instead."));

        mvc.perform(post("/api/plant-exits").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"barcode":"%s","quantity":2,"destination":"HARDENING","reference":"Test tray"}
                    """.formatted(exitScanCode)))
            .andExpect(status().isCreated());
        assertThat(subcultures.findByBarcode(exitBarcode).orElseThrow().getPlantCount()).isEqualTo(1);
        assertThat(subcultures.findByBarcode(exitBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.ACTIVE);

        mvc.perform(post("/api/discards").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"barcode":"%s","reason":"Bacterial"}
                    """.formatted(discardScanCode)))
            .andExpect(status().isOk());
        assertThat(subcultures.findByBarcode(discardBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.DISCARDED);
        assertThat(dashboardPlantTotal()).isEqualTo(plantsBeforeInitiation + 1);

        mvc.perform(post("/api/subcultures").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentBarcode":"%s","mediaId":%d,"laminaFlow":"LF-01","technicianUsername":"admin","lines":[{"bottleCount":1,"plantsPerBottle":1,"cultureType":"MULTIPLY"}]}
                    """.formatted(discardScanCode, mediaId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("This bottle has already been discarded and cannot be subcultured."));
    }

    private long dashboardPlantTotal() throws Exception {
        var response = mvc.perform(get("/api/dashboard/details").with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long total = 0;
        for (var plant : json.readTree(response).get("availablePlants")) {
            total += plant.get("total").asLong();
        }
        return total;
    }
}
