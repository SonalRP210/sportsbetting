package com.sonal.sportsbetting.integration;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.dto.response.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.DomainEventOutboxRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.repository.LatestOddsRepository;
import com.sonal.sportsbetting.service.BettingService;
import com.sonal.sportsbetting.support.AbstractPostgresSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

class BettingServiceTest extends AbstractPostgresSpringBootTest {
    @Autowired
    private BettingService service;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private EventSettlementRepository eventSettlementRepository;

    @Autowired
    private DomainEventOutboxRepository domainEventOutboxRepository;

    @Autowired
    private LatestOddsRepository latestOddsRepository;

    @Autowired
    private ExposureProjectionRepository exposureProjectionRepository;

    @BeforeEach
    void setUp() {
        domainEventOutboxRepository.deleteAll();
        eventSettlementRepository.deleteAll();
        betRepository.deleteAll();
        latestOddsRepository.deleteAll();
        exposureProjectionRepository.deleteAll();
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

        UserExposureResponse exposure = waitForValue(
                () -> service.getUserExposure(userId),
                value -> value.openBetCount() == 2 && new BigDecimal("19.50").compareTo(value.openExposure()) == 0);
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
        waitForOutboxPoll();

        Assertions.assertEquals(new BigDecimal("1.80"), service.getBetById(placed.betId()).getOdds());
        PlaceBetResponse nextPlaced = placeAcceptedBet(
                service,
                new PlaceBetRequest(userId, eventId, "home", new BigDecimal("4.00"))
        );
        BigDecimal latestOdds = waitForValue(
                () -> service.getBetById(nextPlaced.betId()).getOdds(),
                value -> new BigDecimal("2.20").compareTo(value) == 0);
        Assertions.assertEquals(new BigDecimal("2.20"), latestOdds);
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

    private static <T> T waitForValue(Supplier<T> supplier, java.util.function.Predicate<T> predicate) {
        long deadline = System.currentTimeMillis() + 5000;
        T last = supplier.get();
        while (System.currentTimeMillis() < deadline) {
            if (predicate.test(last)) {
                return last;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for asynchronous projection update", ex);
            }
            last = supplier.get();
        }
        return last;
    }

    private static void waitForOutboxPoll() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for outbox poller", ex);
        }
    }
}
