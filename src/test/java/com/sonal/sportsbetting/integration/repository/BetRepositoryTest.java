package com.sonal.sportsbetting.integration.repository;

import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private BetRepository betRepository;

    @Test
    void saveAndFindByUserIdAndEventId() {
        String userId = "u-" + UUID.randomUUID();
        String eventId = "e-" + UUID.randomUUID();
        Bet bet = new Bet(
                "BET-" + UUID.randomUUID(),
                userId,
                eventId,
                new BigDecimal("10.00"),
                new BigDecimal("2.00"),
                "home",
                BetStatus.OPEN
        );
        betRepository.save(bet);

        List<Bet> byUser = betRepository.findByUserId(userId);
        assertEquals(1, byUser.size());
        assertEquals(BetStatus.OPEN, byUser.getFirst().getStatus());

        List<Bet> byEvent = betRepository.findByEventId(eventId);
        assertEquals(1, byEvent.size());
        assertTrue(betRepository.findById(bet.getBetId()).isPresent());
    }
}
