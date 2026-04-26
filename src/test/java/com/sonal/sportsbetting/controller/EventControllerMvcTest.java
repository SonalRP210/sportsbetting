package com.sonal.sportsbetting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.dto.SettleEventRequest;
import com.sonal.sportsbetting.dto.SettleEventResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.config.CorrelationTestSupport;
import com.sonal.sportsbetting.exception.GlobalExceptionHandler;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import({GlobalExceptionHandler.class, CorrelationTestSupport.class})
class EventControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BettingService bettingService;

    @Test
    void settleReturns200() throws Exception {
        when(bettingService.settleEvent("evt-1", "home"))
                .thenReturn(new SettleEventResponse("evt-1", "home", 2, 1, new BigDecimal("30.00"), new BigDecimal("100.00")));

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SettleEventRequest("evt-1", "home"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.winningSelection").value("home"))
                .andExpect(jsonPath("$.winners").value(2))
                .andExpect(jsonPath("$.losers").value(1));

        verify(bettingService).settleEvent("evt-1", "home");
    }

    @Test
    void settleInvalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void settleConflictReturns409() throws Exception {
        doThrow(new SettlementConflictException("Event evt-1 already settled with selection home"))
                .when(bettingService).settleEvent("evt-1", "away");

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SettleEventRequest("evt-1", "away"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SETTLEMENT_CONFLICT"));
    }

    @Test
    void eventBetsReturns200() throws Exception {
        when(bettingService.getEventBets(eq("evt-1"), eq(0), eq(20)))
                .thenReturn(List.of(new UserBetSummaryResponse("BET-1", "evt-1", new BigDecimal("5.00"), new BigDecimal("2.00"), "OPEN")));

        mockMvc.perform(get("/api/v1/events/evt-1/bets").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].betId").value("BET-1"));

        verify(bettingService).getEventBets(eq("evt-1"), eq(0), eq(20));
    }

    @Test
    void eventBetsInvalidPageReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/events/evt-1/bets").param("page", "-1").param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
