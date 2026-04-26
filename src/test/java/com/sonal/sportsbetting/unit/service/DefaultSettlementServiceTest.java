package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.EventSettlement;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.DefaultSettlementService;
import com.sonal.sportsbetting.service.ExposureService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSettlementServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private EventSettlementRepository eventSettlementRepository;

    @Mock
    private ExposureService exposureService;

    private DefaultSettlementService service;

    @BeforeEach
    void setUp() {
        service = new DefaultSettlementService(
                betRepository,
                eventSettlementRepository,
                exposureService,
                new SimpleMeterRegistry(),
                PropertyFixtures.moneyFormatting());
    }

    @Test
    void settleEventMarksStatusesAndReturnsSummary() {
        when(eventSettlementRepository.findById("evt")).thenReturn(Optional.empty());
        Bet winner = new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN);
        Bet loser = new Bet("BET-2", "u2", "evt", new BigDecimal("5.00"), new BigDecimal("3.00"), "away", BetStatus.OPEN);
        when(betRepository.findByEventIdAndStatusForUpdate("evt", BetStatus.OPEN)).thenReturn(List.of(winner, loser));
        when(exposureService.getTotalExposure()).thenReturn(new BigDecimal("0.00"));

        SettleEventResponse response = service.settleEvent("evt", "home");

        assertEquals(1, response.winners());
        assertEquals(1, response.losers());
        assertEquals(new BigDecimal("20.00"), response.totalPayout());
        assertEquals(BetStatus.WON, winner.getStatus());
        assertEquals(BetStatus.LOST, loser.getStatus());
        verify(exposureService).decreaseExposure(new BigDecimal("20.00"));
        verify(exposureService).decreaseExposure(new BigDecimal("15.00"));
        verify(eventSettlementRepository).save(any(EventSettlement.class));
    }

    @Test
    void settleEventIdempotentReplayReturnsLedgerSnapshot() {
        EventSettlement ledger = new EventSettlement("evt", "home", 1, 1, new BigDecimal("20.00"));
        when(eventSettlementRepository.findById("evt")).thenReturn(Optional.of(ledger));
        when(exposureService.getTotalExposure()).thenReturn(new BigDecimal("5.00"));

        SettleEventResponse response = service.settleEvent("evt", "home");

        assertEquals(1, response.winners());
        assertEquals(1, response.losers());
        assertEquals(new BigDecimal("20.00"), response.totalPayout());
        assertEquals(new BigDecimal("5.00"), response.globalExposure());
        verify(betRepository, never()).findByEventIdAndStatusForUpdate(any(), any());
    }

    @Test
    void settleEventThrowsWhenWinnerDiffersFromLedger() {
        EventSettlement ledger = new EventSettlement("evt", "home", 1, 1, new BigDecimal("20.00"));
        when(eventSettlementRepository.findById("evt")).thenReturn(Optional.of(ledger));

        assertThrows(SettlementConflictException.class, () -> service.settleEvent("evt", "away"));
    }
}
