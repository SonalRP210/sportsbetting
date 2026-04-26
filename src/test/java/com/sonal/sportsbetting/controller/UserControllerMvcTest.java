package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.config.CorrelationTestSupport;
import com.sonal.sportsbetting.exception.GlobalExceptionHandler;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, CorrelationTestSupport.class})
class UserControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BettingService bettingService;

    @Test
    void userBetsReturns200() throws Exception {
        when(bettingService.getUserSummary(eq("user-1"), eq(0), eq(10)))
                .thenReturn(List.of(new UserBetSummaryResponse("BET-1", "evt-1", new BigDecimal("5.00"), new BigDecimal("2.00"), "OPEN")));

        mockMvc.perform(get("/api/v1/users/user-1/bets").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].betId").value("BET-1"));
    }

    @Test
    void userBetsInvalidSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1/bets").param("page", "0").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void userExposureReturns200() throws Exception {
        when(bettingService.getUserExposure("user-1"))
                .thenReturn(new UserExposureResponse("user-1", new BigDecimal("25.00"), 3));

        mockMvc.perform(get("/api/v1/users/user-1/exposure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.openBetCount").value(3))
                .andExpect(jsonPath("$.openExposure").value(25.0));
    }
}
