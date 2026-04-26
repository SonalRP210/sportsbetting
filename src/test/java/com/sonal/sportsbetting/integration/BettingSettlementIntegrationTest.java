package com.sonal.sportsbetting.integration;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.BettingService;
import com.sonal.sportsbetting.support.AbstractPostgresSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BettingSettlementIntegrationTest extends AbstractPostgresSpringBootTest {

    @Autowired
    private BettingService bettingService;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private EventSettlementRepository eventSettlementRepository;

    @BeforeEach
    void setUp() {
        eventSettlementRepository.deleteAll();
        betRepository.deleteAll();
    }

    @Test
    void settleMarksWinnersAndLosers() {
        String eventId = "evt-" + UUID.randomUUID();
        seedOdds(eventId, "home", new BigDecimal("2.00"));
        seedOdds(eventId, "away", new BigDecimal("3.00"));

        bettingService.placeBet(new PlaceBetRequest("u1", eventId, "home", new BigDecimal("10.00")));
        bettingService.placeBet(new PlaceBetRequest("u2", eventId, "away", new BigDecimal("5.00")));

        SettleEventResponse first = bettingService.settleEvent(eventId, "home");
        assertEquals(1, first.winners());
        assertEquals(1, first.losers());

        assertEquals(BetStatus.WON, betRepository.findAll().stream().filter(b -> "u1".equals(b.getUserId())).findFirst().orElseThrow().getStatus());
        assertEquals(BetStatus.LOST, betRepository.findAll().stream().filter(b -> "u2".equals(b.getUserId())).findFirst().orElseThrow().getStatus());

        SettleEventResponse second = bettingService.settleEvent(eventId, "home");
        assertEquals(1, second.winners());
        assertEquals(1, second.losers());
        assertEquals(first.totalPayout(), second.totalPayout());
    }

    @Test
    void secondSettleWithDifferentWinnerThrowsConflict() {
        String eventId = "evt-" + UUID.randomUUID();
        seedOdds(eventId, "home", new BigDecimal("2.00"));
        seedOdds(eventId, "away", new BigDecimal("3.00"));
        bettingService.placeBet(new PlaceBetRequest("u1", eventId, "home", new BigDecimal("10.00")));
        bettingService.settleEvent(eventId, "home");

        assertThrows(SettlementConflictException.class, () -> bettingService.settleEvent(eventId, "away"));
    }

    @Test
    void placeBetWithoutOddsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> bettingService.placeBet(new PlaceBetRequest("u1", "unknown-event", "home", new BigDecimal("1.00"))));
    }

    private void seedOdds(String eventId, String selection, BigDecimal odds) {
        OddsUpdate u = new OddsUpdate();
        u.setEventId(eventId);
        u.setSelection(selection);
        u.setOdds(odds);
        bettingService.consumeOddsFeed(List.of(u));
    }
}
