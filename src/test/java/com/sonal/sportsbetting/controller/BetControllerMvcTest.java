package com.sonal.sportsbetting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.dto.BetDetailResponse;
import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.config.CorrelationTestSupport;
import com.sonal.sportsbetting.exception.GlobalExceptionHandler;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BetController.class)
@Import({GlobalExceptionHandler.class, CorrelationTestSupport.class})
class BetControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BettingService bettingService;

    @Test
    void placeBetReturns201AndBody() throws Exception {
        when(bettingService.placeBet(any(PlaceBetRequest.class), nullable(String.class)))
                .thenReturn(new PlaceBetResponse("BET-1", new BigDecimal("1.50"), "OPEN"));

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlaceBetRequest("user-1", "evt-1", "home", new BigDecimal("10.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId").value("BET-1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.acceptedOdds").value(1.5));

        verify(bettingService).placeBet(any(PlaceBetRequest.class), nullable(String.class));
    }

    @Test
    void placeBetForwardsIdempotencyKeyHeader() throws Exception {
        when(bettingService.placeBet(any(PlaceBetRequest.class), eq("pay-42")))
                .thenReturn(new PlaceBetResponse("BET-2", new BigDecimal("2.00"), "OPEN"));

        mockMvc.perform(post("/api/v1/bets")
                        .header("Idempotency-Key", "pay-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlaceBetRequest("user-1", "evt-1", "away", new BigDecimal("5.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId").value("BET-2"));

        verify(bettingService).placeBet(any(PlaceBetRequest.class), eq("pay-42"));
    }

    @Test
    void placeBetInvalidRequestReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlaceBetRequest("", "evt-1", "home", new BigDecimal("10.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getBetReturns200() throws Exception {
        when(bettingService.getBetById("BET-1"))
                .thenReturn(new BetDetailResponse(
                        "BET-1", "user-1", "evt-1", "home",
                        new BigDecimal("10.00"), new BigDecimal("1.50"), "OPEN"));

        mockMvc.perform(get("/api/v1/bets/BET-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value("BET-1"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void cancelBetReturns200() throws Exception {
        when(bettingService.cancelBet("BET-1"))
                .thenReturn(new CancelBetResponse("BET-1", "CANCELLED", "Bet cancelled"));

        mockMvc.perform(post("/api/v1/bets/BET-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value("BET-1"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bettingService).cancelBet(eq("BET-1"));
    }
}
