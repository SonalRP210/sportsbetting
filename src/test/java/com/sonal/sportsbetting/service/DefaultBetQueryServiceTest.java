package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.repository.BetRepository;
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
    void getUserSummaryReturnsMappedRows() {
        Bet bet = new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN);
        when(betRepository.findByUserId("u1")).thenReturn(List.of(bet));

        var rows = service.getUserSummary("u1");

        assertEquals(1, rows.size());
        assertEquals("BET-1", rows.getFirst().betId());
        assertEquals(new BigDecimal("10.00"), rows.getFirst().stake());
    }
}
