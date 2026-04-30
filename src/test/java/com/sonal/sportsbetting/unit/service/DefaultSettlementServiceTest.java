package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.domain.event.EventSettledPayload;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.EventSettlement;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.DefaultSettlementService;
import com.sonal.sportsbetting.service.ExposureService;
import com.sonal.sportsbetting.service.outbox.DomainEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private SimpleMeterRegistry meterRegistry;
    private DefaultSettlementService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new DefaultSettlementService(
                betRepository,
                eventSettlementRepository,
                exposureService,
                domainEventPublisher,
                PropertyFixtures.settlementRetry(),
                PropertyFixtures.settlement(),
                meterRegistry,
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
        ArgumentCaptor<EventSettledPayload> captor = ArgumentCaptor.forClass(EventSettledPayload.class);
        verify(domainEventPublisher).publish(eq(DomainEventType.EVENT_SETTLED), captor.capture());
        assertEquals(2, captor.getValue().releases().size());
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
        verify(domainEventPublisher, never()).publish(any(), any());
    }

    @Test
    void settleEventThrowsWhenWinnerDiffersFromLedger() {
        EventSettlement ledger = new EventSettlement("evt", "home", 1, 1, new BigDecimal("20.00"));
        when(eventSettlementRepository.findById("evt")).thenReturn(Optional.of(ledger));

        assertThrows(SettlementConflictException.class, () -> service.settleEvent("evt", "away"));
    }

    @Test
    void settleEventLockFailureIncrementsMetricAndThrows() {
        when(eventSettlementRepository.findById("evt")).thenReturn(Optional.empty());
        doThrow(new org.springframework.dao.PessimisticLockingFailureException("lock"))
                .when(betRepository).findByEventIdAndStatusForUpdate("evt", BetStatus.OPEN);

        assertThrows(IllegalStateException.class, () -> service.settleEvent("evt", "home"));
        Counter failures = Search.in(meterRegistry).name("settlement.lock.failures").counter();
        assertEquals(1.0, failures.count());
    }
}
