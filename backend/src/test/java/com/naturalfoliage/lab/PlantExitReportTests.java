package com.naturalfoliage.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalfoliage.lab.model.PlantExit;
import com.naturalfoliage.lab.repository.PlantExitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlantExitReportTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PlantExitRepository exits;

    @Test
    @Transactional
    void hardeningReportUsesFullHistoryAndRespectsStaffAccess() throws Exception {
        String code = "EXIT-" + UUID.randomUUID().toString().substring(0, 8);
        String worker = "worker-" + UUID.randomUUID();
        for (int i = 0; i < 23; i++) addExit(code, worker, 3, LocalDateTime.of(2040, 1, 12, 10, 0));
        addExit(code, worker, 5, LocalDateTime.of(2040, 2, 1, 11, 0));
        addExit(code, worker, 5, LocalDateTime.of(2040, 2, 2, 11, 0));
        addExit(code, "other-worker", 99, LocalDateTime.of(2040, 1, 15, 11, 0));

        var staff = user(worker).authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_PLANT_EXIT"));
        mvc.perform(get("/api/plant-exits/summary"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/plant-exits/summary").with(user(worker).roles("TECHNICIAN")))
            .andExpect(status().isForbidden());
        var recent = mvc.perform(get("/api/plant-exits").with(staff))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(recent).size()).isEqualTo(20);

        var monthly = report(staff, "month");
        assertThat(monthly.get("exits").asInt()).isEqualTo(25);
        assertThat(monthly.get("plants").asInt()).isEqualTo(79);
        assertThat(row(monthly, code, "2040-01").get("exits").asInt()).isEqualTo(23);
        assertThat(row(monthly, code, "2040-01").get("plants").asInt()).isEqualTo(69);
        assertThat(row(monthly, code, "2040-02").get("plants").asInt()).isEqualTo(10);

        var yearly = report(staff, "year");
        assertThat(row(yearly, code, "2040").get("exits").asInt()).isEqualTo(25);
        assertThat(row(yearly, code, "2040").get("plants").asInt()).isEqualTo(79);

        var admin = report(user("admin").roles("ADMIN"), "month");
        assertThat(row(admin, code, "2040-01").get("exits").asInt()).isEqualTo(24);
        assertThat(row(admin, code, "2040-01").get("plants").asInt()).isEqualTo(168);
    }

    private JsonNode report(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor auth,
                            String interval) throws Exception {
        String response = mvc.perform(get("/api/plant-exits/summary").with(auth).param("interval", interval))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private JsonNode row(JsonNode report, String code, String period) {
        for (JsonNode item : report.get("rows")) {
            if (code.equals(item.get("plantCode").asText()) && period.equals(item.get("period").asText())) return item;
        }
        throw new AssertionError("Missing report row for " + code + " in " + period);
    }

    private void addExit(String code, String technician, int quantity, LocalDateTime date) {
        var exit = new PlantExit();
        exit.setBarcode("EXIT-" + UUID.randomUUID());
        exit.setPlantCode(code);
        exit.setPlantName("Report test plant");
        exit.setTechnician(technician);
        exit.setDestination("HARDENING");
        exit.setQuantity(quantity);
        ReflectionTestUtils.setField(exit, "exitedAt", date);
        exits.save(exit);
    }
}
