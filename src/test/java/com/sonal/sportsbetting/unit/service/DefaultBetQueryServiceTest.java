package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.service.DefaultBetQueryService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBetQueryServiceTest {

    @Mock
    private BetRepository betRepository;

    private DefaultBetQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultBetQueryService(betRepository, new SimpleMeterRegistry());
    }

    @Test
    void cancelBetChangesStatusToCancelled() {
        Bet bet = new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN);
        when(betRepository.findById("BET-1")).thenReturn(Optional.of(bet));

        var response = service.cancelBet("BET-1");

        assertEquals("CANCELLED", response.status());
        assertEquals(BetStatus.CANCELLED, bet.getStatus());
        verify(betRepository).save(any(Bet.class));
    }

    @Test
    void cancelBetThrowsForMissingBet() {
        when(betRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(BetNotFoundException.class, () -> service.cancelBet("missing"));
    }

    @Test
    void cancelBetThrowsWhenBetNotOpen() {
        Bet bet = new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.WON);
        when(betRepository.findById("BET-1")).thenReturn(Optional.of(bet));

        assertThrows(IllegalArgumentException.class, () -> service.cancelBet("BET-1"));
    }

    @Test
    void getUserSummaryReturnsMappedRows() {
        Bet bet = new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN);
        when(betRepository.findByUserId("u1")).thenReturn(List.of(bet));

        var rows = service.getUserSummary("u1");

        assertEquals(1, rows.size());
        assertEquals("BET-1", rows.getFirst().betId());
        assertEquals(new BigDecimal("10.00"), rows.getFirst().stake());
    }

    @Test
    void getBetByIdReturnsDetail() {
        Bet bet = new Bet("BET-x", "u1", "evt", new BigDecimal("5.00"), new BigDecimal("1.50"), "away", BetStatus.OPEN);
        when(betRepository.findById("BET-x")).thenReturn(Optional.of(bet));

        BetDetailResponse detail = service.getBetById("BET-x");

        assertEquals("BET-x", detail.getBetId());
        assertEquals("u1", detail.getUserId());
        assertEquals("away", detail.getSelection());
        assertEquals(new BigDecimal("5.00"), detail.getStake());
        assertEquals(new BigDecimal("1.50"), detail.getOdds());
    }

    @Test
    void getBetByIdThrowsWhenMissing() {
        when(betRepository.findById("none")).thenReturn(Optional.empty());
        assertThrows(BetNotFoundException.class, () -> service.getBetById("none"));
    }

    @Test
    void getUserSummaryPaginatesBySortedBetId() {
        when(betRepository.findByUserId("u1")).thenReturn(List.of(
                new Bet("BET-c", "u1", "e1", new BigDecimal("1.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN),
                new Bet("BET-a", "u1", "e2", new BigDecimal("2.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN),
                new Bet("BET-b", "u1", "e3", new BigDecimal("3.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN)
        ));

        var page0 = service.getUserSummary("u1", 0, 2);
        var page1 = service.getUserSummary("u1", 1, 2);

        assertEquals(2, page0.size());
        assertEquals("BET-a", page0.get(0).betId());
        assertEquals("BET-b", page0.get(1).betId());
        assertEquals(1, page1.size());
        assertEquals("BET-c", page1.get(0).betId());
    }

    @Test
    void getUserSummaryPageBeyondEndReturnsEmpty() {
        when(betRepository.findByUserId("u1")).thenReturn(List.of(
                new Bet("BET-only", "u1", "e1", BigDecimal.ONE, new BigDecimal("2.00"), "home", BetStatus.OPEN)
        ));
        assertEquals(0, service.getUserSummary("u1", 5, 10).size());
    }
}
