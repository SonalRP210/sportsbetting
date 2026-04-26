package com.sonal.sportsbetting.smoke;

import com.sonal.sportsbetting.support.AbstractPostgresSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests: a minimal HTTP pass over the <strong>fully wired</strong> application (including real Postgres
 * via Testcontainers). Use after refactors or in CI to catch “app won’t start / routing broken / DB down”
 * before relying on slower integration scenarios.
 */
@AutoConfigureMockMvc
class ApplicationSmokeTest extends AbstractPostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsReachable() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void apiReturnsStructuredErrorForUnknownBet() throws Exception {
        mockMvc.perform(get("/api/v1/bets/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BET_NOT_FOUND"));
    }
}
