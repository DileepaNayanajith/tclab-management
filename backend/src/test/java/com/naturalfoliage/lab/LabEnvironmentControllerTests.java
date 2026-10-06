package com.naturalfoliage.lab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.lab-sensor.token=unit-test-device-token")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class LabEnvironmentControllerTests {
    @Autowired MockMvc mvc;

    @Test
    void deviceTokenIsRequiredAndLatestReadingIsVisibleToSignedInStaff() throws Exception {
        mvc.perform(get("/api/lab-environment"))
            .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/lab-environment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"temperatureC\":26.5,\"humidityPercent\":61}"))
            .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/lab-environment")
                .header("X-Device-Token", "unit-test-device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"temperatureC\":26.5,\"humidityPercent\":61}"))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/lab-environment").with(httpBasic("admin", "ChangeMe123!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.temperatureC").value(26.5))
            .andExpect(jsonPath("$.humidityPercent").value(61))
            .andExpect(jsonPath("$.recordedAt").isNotEmpty());
    }

    @Test
    void rejectsInvalidMeasurements() throws Exception {
        mvc.perform(post("/api/lab-environment")
                .header("X-Device-Token", "unit-test-device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"temperatureC\":26.5,\"humidityPercent\":150}"))
            .andExpect(status().isBadRequest());
    }
}
