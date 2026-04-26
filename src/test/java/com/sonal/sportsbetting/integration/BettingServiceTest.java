package com.sonal.sportsbetting.integration;

import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.BettingService;
import com.sonal.sportsbetting.support.AbstractPostgresSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class BettingServiceTest extends AbstractPostgresSpringBootTest {
    @Autowired
    private BettingService service;

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
    void shouldPlaceBet() {
        String eventId = "e1-" + UUID.randomUUID();
        seedOdds(service, eventId, "home", new BigDecimal("1.50"));
        PlaceBetRequest req = new PlaceBetRequest("u1-" + UUID.randomUUID(), eventId, "home", new BigDecimal("20.00"));
        PlaceBetResponse response = placeAcceptedBet(service, req);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.betId());
        Assertions.assertNotNull(response.status());
    }

    @Test
    void shouldThrowWhenBetNotFound() {
        Assertions.assertThrows(BetNotFoundException.class, () -> service.getBetById("missing-bet-id"));
    }

    @Test
    void shouldCancelOpenBet() {
        String userId = "cancel-user-" + UUID.randomUUID();
        String eventId = "event-cancel-" + UUID.randomUUID();
        seedOdds(service, eventId, "home", new BigDecimal("1.50"));
        PlaceBetResponse placed = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventId, "home", new BigDecimal("10.00"))
        );

        CancelBetResponse cancelled = service.cancelBet(placed.betId());
        Assertions.assertEquals("CANCELLED", cancelled.status());
        Assertions.assertEquals(placed.betId(), cancelled.betId());
    }

    @Test
    void shouldComputeUserExposureOnlyForOpenBets() {
        String userId = "exposure-user-" + UUID.randomUUID();
        String eventOpen = "event-open-" + UUID.randomUUID();
        String eventCancelled = "event-cancelled-" + UUID.randomUUID();
        String eventOpen2 = "event-open-2-" + UUID.randomUUID();
        seedOdds(service, eventOpen, "home", new BigDecimal("1.50"));
        seedOdds(service, eventCancelled, "away", new BigDecimal("1.50"));
        seedOdds(service, eventOpen2, "draw", new BigDecimal("1.50"));

        PlaceBetResponse openBet = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventOpen, "home", new BigDecimal("12.00"))
        );
        placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventCancelled, "away", new BigDecimal("8.00"))
        );
        service.cancelBet(openBet.betId()); // make one non-open to verify filter

        placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventOpen2, "draw", new BigDecimal("5.00"))
        );

        UserExposureResponse exposure = service.getUserExposure(userId);
        Assertions.assertEquals(2, exposure.openBetCount());
        Assertions.assertEquals(new BigDecimal("19.50"), exposure.openExposure());
    }

    @Test
    void shouldPaginateUserBets() {
        String userId = "page-user-" + UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            String eventId = "event-" + i + "-" + UUID.randomUUID();
            seedOdds(service, eventId, "home", new BigDecimal("1.50"));
            placeAcceptedBet(service, new PlaceBetRequest(userId, eventId, "home", new BigDecimal("2.00")));
        }

        List<UserBetSummaryResponse> firstPage = service.getUserSummary(userId, 0, 2);
        List<UserBetSummaryResponse> secondPage = service.getUserSummary(userId, 1, 2);

        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals(1, secondPage.size());
    }

    @Test
    void shouldUpdateOddsFromFeed() {
        String userId = "odds-user-" + UUID.randomUUID();
        String eventId = "event-odds-" + UUID.randomUUID();
        seedOdds(service, eventId, "home", new BigDecimal("1.80"));
        PlaceBetResponse placed = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventId, "home", new BigDecimal("4.00"))
        );

        OddsUpdate update = new OddsUpdate();
        update.setEventId(eventId);
        update.setSelection("home");
        update.setOdds(new BigDecimal("2.20"));
        service.consumeOddsFeed(List.of(update));

        Assertions.assertEquals(new BigDecimal("1.80"), service.getBetById(placed.betId()).getOdds());
        PlaceBetResponse nextPlaced = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventId, "home", new BigDecimal("4.00"))
        );
        Assertions.assertEquals(new BigDecimal("2.20"), service.getBetById(nextPlaced.betId()).getOdds());
    }

    private static PlaceBetResponse placeAcceptedBet(BettingService service, PlaceBetRequest req) {
        return service.placeBet(req);
    }

    private static void seedOdds(BettingService service, String eventId, String selection, BigDecimal odds) {
        OddsUpdate update = new OddsUpdate();
        update.setEventId(eventId);
        update.setSelection(selection);
        update.setOdds(odds);
        service.consumeOddsFeed(List.of(update));
    }
}
