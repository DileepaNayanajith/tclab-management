package com.naturalfoliage.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalfoliage.lab.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class OperationalCleanupTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PlantRepository plants;
    @Autowired MediaCompositionRepository media;
    @Autowired UserRepository users;
    @Autowired MotherBottleRepository mothers;
    @Autowired SubcultureRepository subcultures;
    @Autowired PriceItemRepository prices;

    @Test
    void clearsOnlyOperationalRecordsAndRejectsStalePreview() throws Exception {
        long plantCount = plants.count();
        long mediaCount = media.count();
        long staffCount = users.count();
        assertThat(mothers.count()).isPositive();
        assertThat(subcultures.count()).isPositive();
        assertThat(prices.count()).isPositive();
        var preview = json.readTree(mvc.perform(get("/api/admin/data/operational-preview")
                .with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var counts = new LinkedHashMap<String, Long>();
        for (var key : new String[] {"sales", "discards", "plantExits", "subcultures", "initiations", "mediaPreparations", "prices"}) {
            counts.put(key, preview.get(key).asLong());
        }
        var stale = new LinkedHashMap<>(counts);
        stale.put("initiations", -1L);
        mvc.perform(delete("/api/admin/data/operational").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("confirmation", "CLEAR OPERATIONAL DATA", "expectedCounts", stale))))
            .andExpect(status().isBadRequest());
        assertThat(mothers.count()).isPositive();
        mvc.perform(delete("/api/admin/data/operational").with(httpBasic("admin", "ChangeMe123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("confirmation", "CLEAR OPERATIONAL DATA", "expectedCounts", counts))))
            .andExpect(status().isOk());
        assertThat(mothers.count()).isZero();
        assertThat(subcultures.count()).isZero();
        assertThat(prices.count()).isZero();
        assertThat(plants.count()).isEqualTo(plantCount);
        assertThat(media.count()).isEqualTo(mediaCount);
        assertThat(users.count()).isEqualTo(staffCount);
        assertThat(media.findAll()).allMatch(item -> item.getAvailableBottles() == 0);
    }
}
