package com.sonal.sportsbetting;

import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class BettingServiceTest {

    @Test
    void shouldPlaceBet() {
        BettingService service = new BettingService(new BetRepository());
        PlaceBetRequest req = new PlaceBetRequest("u1-" + UUID.randomUUID(), "e1", "home", new BigDecimal("20.00"));
        PlaceBetResponse response = placeAcceptedBet(service, req);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.betId());
        Assertions.assertNotNull(response.status());
    }

    @Test
    void shouldThrowWhenBetNotFound() {
        BettingService service = new BettingService(new BetRepository());
        Assertions.assertThrows(BetNotFoundException.class, () -> service.getBetById("missing-bet-id"));
    }

    @Test
    void shouldCancelOpenBet() {
        BettingService service = new BettingService(new BetRepository());
        String userId = "cancel-user-" + UUID.randomUUID();
        PlaceBetResponse placed = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, "event-cancel", "home", new BigDecimal("10.00"))
        );

        CancelBetResponse cancelled = service.cancelBet(placed.betId());
        Assertions.assertEquals("CANCELLED", cancelled.status());
        Assertions.assertEquals(placed.betId(), cancelled.betId());
    }

    @Test
    void shouldComputeUserExposureOnlyForOpenBets() {
        BettingService service = new BettingService(new BetRepository());
        String userId = "exposure-user-" + UUID.randomUUID();

        PlaceBetResponse openBet = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, "event-open", "home", new BigDecimal("12.00"))
        );
        placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, "event-cancelled", "away", new BigDecimal("8.00"))
        );
        service.cancelBet(openBet.betId()); // make one non-open to verify filter

        placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, "event-open-2", "draw", new BigDecimal("5.00"))
        );

        UserExposureResponse exposure = service.getUserExposure(userId);
        Assertions.assertEquals(1, exposure.openBetCount());
        Assertions.assertEquals(new BigDecimal("7.50"), exposure.openExposure());
    }

    @Test
    void shouldPaginateUserBets() {
        BettingService service = new BettingService(new BetRepository());
        String userId = "page-user-" + UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            placeAcceptedBet(service, new PlaceBetRequest(userId, "event-" + i, "home", new BigDecimal("2.00")));
            sleep(2);
        }

        List<UserBetSummaryResponse> firstPage = service.getUserSummary(userId, 0, 2);
        List<UserBetSummaryResponse> secondPage = service.getUserSummary(userId, 1, 2);

        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals(1, secondPage.size());
    }

    @Test
    void shouldUpdateOddsFromFeed() {
        BettingService service = new BettingService(new BetRepository());
        String userId = "odds-user-" + UUID.randomUUID();
        PlaceBetResponse placed = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, "event-odds", "home", new BigDecimal("4.00"))
        );

        OddsUpdate update = new OddsUpdate();
        update.setEventId("event-odds");
        update.setSelection("home");
        update.setOdds(new BigDecimal("2.20"));
        service.consumeOddsFeed(List.of(update));

        Assertions.assertEquals(new BigDecimal("2.20"), service.getBetById(placed.betId()).getOdds());
    }

    private static PlaceBetResponse placeAcceptedBet(BettingService service, PlaceBetRequest req) {
        PlaceBetResponse response = null;
        for (int i = 0; i < 30; i++) {
            try {
                response = service.placeBet(req);
                break;
            } catch (IllegalArgumentException ignored) {
                // Retry because this service intentionally simulates random rejection.
            }
        }
        Assertions.assertNotNull(response, "Unable to place accepted bet after retries");
        return response;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating test data", e);
        }
    }
}
