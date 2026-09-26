package com.naturalfoliage.lab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dashboardaccess;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DashboardAccessTests {
    @Autowired MockMvc mvc;

    @Test void dashboardOnlyStaffCanReadAllDashboardDetailsWithoutMediaManagementAccess() throws Exception {
        var staff = user("staff").authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
            new SimpleGrantedAuthority("ACCESS_DASHBOARD"));
        mvc.perform(get("/api/dashboard").with(staff)).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/details").with(staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$.availablePlants.length()", greaterThan(0)))
            .andExpect(jsonPath("$.oldCultures.length()", greaterThan(0)));
        mvc.perform(get("/api/dashboard/media-stock").with(staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").exists())
            .andExpect(jsonPath("$[0].availableBottles").isNumber())
            .andExpect(jsonPath("$[0].hormones").doesNotExist());
        mvc.perform(get("/api/media").with(staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/media/options").with(staff)).andExpect(status().isForbidden());
    }

    @Test void dashboardRoutesRemainProtected() throws Exception {
        for (var path : new String[] {"/api/dashboard", "/api/dashboard/details", "/api/dashboard/media-stock"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("staff").roles("TECHNICIAN"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        }
    }
}
