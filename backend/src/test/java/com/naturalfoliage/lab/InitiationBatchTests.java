package com.naturalfoliage.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalfoliage.lab.repository.MediaCompositionRepository;
import com.naturalfoliage.lab.repository.MotherBottleRepository;
import com.naturalfoliage.lab.repository.PlantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:initiationbatch;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InitiationBatchTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PlantRepository plants;
    @Autowired MediaCompositionRepository media;
    @Autowired MotherBottleRepository mothers;

    @Test void createsSeparateScannableRecordsAndRejectsInvalidBatches() throws Exception {
        long plantId = plants.findAll().getFirst().getId();
        long mediaId = media.findAll().getFirst().getId();
        long before = mothers.count();
        var staff = user("staff").authorities(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ACCESS_MOTHER_BOTTLES"));

        for (int count : new int[] {0, 101}) {
            mvc.perform(post("/api/mother-bottles/batch").with(staff)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(plantId, mediaId, count)))
                .andExpect(status().isBadRequest());
        }
        assertThat(mothers.count()).isEqualTo(before);

        var response = mvc.perform(post("/api/mother-bottles/batch").with(staff)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(plantId, mediaId, 3)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var created = json.readTree(response);
        assertThat(created.size()).isEqualTo(3);
        var barcodes = new HashSet<String>();
        for (var item : created) {
            assertThat(item.get("plantCount").asInt()).isEqualTo(5);
            assertThat(item.get("cycle").asInt()).isEqualTo(2);
            assertThat(item.get("technician").asText()).isEqualTo("staff");
            assertThat(item.get("printed").asBoolean()).isFalse();
            String barcode = item.get("barcode").asText();
            assertThat(barcodes.add(barcode)).isTrue();
            assertThat(mothers.findByBarcode(barcode)).isPresent();
        }
        assertThat(mothers.count()).isEqualTo(before + 3);

        mvc.perform(post("/api/mother-bottles/batch").with(staff)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(plantId, -1, 3)))
            .andExpect(status().isBadRequest());
        assertThat(mothers.count()).isEqualTo(before + 3);

        var nextResponse = mvc.perform(post("/api/mother-bottles/batch").with(staff)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(plantId, mediaId, 1)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(barcodes.add(json.readTree(nextResponse).get(0).get("barcode").asText())).isTrue();
    }

    private String payload(long plantId, long mediaId, int bottleCount) {
        return """
            {"plantId":%d,"mediaId":%d,"plantCount":5,"cycle":2,"laminaFlow":"LF-01","bottleCount":%d}
            """.formatted(plantId, mediaId, bottleCount);
    }
}
