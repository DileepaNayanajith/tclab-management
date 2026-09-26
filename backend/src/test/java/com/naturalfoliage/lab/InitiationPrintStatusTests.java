package com.naturalfoliage.lab;

import com.naturalfoliage.lab.repository.MotherBottleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:printstatus;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InitiationPrintStatusTests {
    @Autowired MockMvc mvc;
    @Autowired MotherBottleRepository mothers;

    @Test void persistsPrintStatusAndRejectsInvalidBatchesAndUnauthorizedStaff() throws Exception {
        var bottle = mothers.findAll().getFirst();
        bottle.setPrinted(false);
        mothers.saveAndFlush(bottle);
        long id = bottle.getId();
        mvc.perform(patch("/api/mother-bottles/print-status").with(user("staff").roles("TECHNICIAN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + id + "],\"printed\":true}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/mother-bottles/print-status").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + id + ",-1],\"printed\":true}"))
            .andExpect(status().isBadRequest());
        assertThat(mothers.findById(id).orElseThrow().isPrinted()).isFalse();
        mvc.perform(patch("/api/mother-bottles/print-status").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + id + "],\"printed\":true}"))
            .andExpect(status().isOk());
        assertThat(mothers.findById(id).orElseThrow().isPrinted()).isTrue();
    }
}
