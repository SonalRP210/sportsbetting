package com.sonal.sportsbetting.integration;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.DomainEventOutboxRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.repository.LatestOddsRepository;
import com.sonal.sportsbetting.service.BettingService;
import com.sonal.sportsbetting.support.AbstractPostgresSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BetIdempotencyIntegrationTest extends AbstractPostgresSpringBootTest {

    @Autowired
    private BettingService bettingService;

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
    void duplicateIdempotencyKeyReturnsSameBetWithoutDuplicateRows() {
        String eventId = "evt-" + UUID.randomUUID();
        OddsUpdate odds = new OddsUpdate();
        odds.setEventId(eventId);
        odds.setSelection("home");
        odds.setOdds(new BigDecimal("1.60"));
        bettingService.consumeOddsFeed(List.of(odds));

        PlaceBetRequest req = new PlaceBetRequest("user-a", eventId, "home", new BigDecimal("25.00"));
        String key = "idem-" + UUID.randomUUID();

        PlaceBetResponse first = bettingService.placeBet(req, key);
        PlaceBetResponse second = bettingService.placeBet(req, key);

        assertEquals(first.betId(), second.betId());
        assertEquals(first.acceptedOdds(), second.acceptedOdds());
        assertEquals(1, betRepository.findByUserId("user-a").size());
    }
}
