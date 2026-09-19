package com.naturalfoliage.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalfoliage.lab.model.BottleStatus;
import com.naturalfoliage.lab.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                    {"mediaId":%d,"bottleCount":10}
                    """.formatted(mediaId)))
            .andExpect(status().isCreated());
        assertThat(media.findById(mediaId).orElseThrow().getAvailableBottles()).isEqualTo(10);

        long plantId = plants.findAll().getFirst().getId();
        var initiationResponse = mvc.perform(post("/api/mother-bottles").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"plantId":%d,"mediaId":%d,"plantCount":6,"cycle":0,"laminaFlow":"LF-01"}
                    """.formatted(plantId, mediaId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String parentBarcode = json.readTree(initiationResponse).get("barcode").asText();

        var subcultureResponse = mvc.perform(post("/api/subcultures").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentBarcode":"%s","mediaId":%d,"laminaFlow":"LF-01","technicianUsername":"admin","lines":[{"bottleCount":2,"plantsPerBottle":3,"cultureType":"ROOTING"}]}
                    """.formatted(parentBarcode, mediaId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var created = json.readTree(subcultureResponse);
        String exitBarcode = created.get(0).get("barcode").asText();
        String discardBarcode = created.get(1).get("barcode").asText();
        assertThat(media.findById(mediaId).orElseThrow().getAvailableBottles()).isEqualTo(8);
        assertThat(mothers.findByBarcode(parentBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.USED);

        mvc.perform(post("/api/plant-exits").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"barcode":"%s","quantity":2,"destination":"HARDENING","reference":"Test tray"}
                    """.formatted(exitBarcode)))
            .andExpect(status().isCreated());
        assertThat(subcultures.findByBarcode(exitBarcode).orElseThrow().getPlantCount()).isEqualTo(1);
        assertThat(subcultures.findByBarcode(exitBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.ACTIVE);

        mvc.perform(post("/api/discards").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"barcode":"%s","reason":"Bacterial"}
                    """.formatted(discardBarcode)))
            .andExpect(status().isOk());
        assertThat(subcultures.findByBarcode(discardBarcode).orElseThrow().getStatus()).isEqualTo(BottleStatus.DISCARDED);
    }
}
