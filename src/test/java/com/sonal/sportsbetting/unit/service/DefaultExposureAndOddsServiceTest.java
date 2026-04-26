package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.service.DefaultExposureService;
import com.sonal.sportsbetting.service.DefaultOddsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultExposureAndOddsServiceTest {

    @Mock
    private BetRepository betRepository;

    private DefaultExposureService exposureService;
    private DefaultOddsService oddsService;

    @BeforeEach
    void setUp() {
        exposureService = new DefaultExposureService(
                betRepository,
                PropertyFixtures.moneyFormatting(),
                new SimpleMeterRegistry());
        oddsService = new DefaultOddsService(
                new SimpleMeterRegistry(),
                PropertyFixtures.moneyFormatting(),
                PropertyFixtures.odds());
    }

    @Test
    void exposureIncreaseAndDecreaseNeverBelowZero() {
        exposureService.increaseExposure(new BigDecimal("12.25"));
        exposureService.decreaseExposure(new BigDecimal("2.25"));
        exposureService.decreaseExposure(new BigDecimal("50.00"));

        assertEquals(0, exposureService.getTotalExposure().compareTo(BigDecimal.ZERO));
    }

    @Test
    void userExposureCountsOnlyOpenBets() {
        when(betRepository.findByUserId("u1")).thenReturn(List.of(
                new Bet("BET-1", "u1", "evt", new BigDecimal("10.00"), new BigDecimal("2.00"), "home", BetStatus.OPEN),
                new Bet("BET-2", "u1", "evt", new BigDecimal("5.00"), new BigDecimal("2.00"), "away", BetStatus.CANCELLED)
        ));

        var result = exposureService.getUserExposure("u1");

        assertEquals(1, result.openBetCount());
        assertEquals(new BigDecimal("20.00"), result.openExposure());
    }

    @Test
    void oddsServiceStoresScaledOdds() {
        OddsUpdate update = new OddsUpdate();
        update.setEventId("evt");
        update.setSelection("home");
        update.setOdds(new BigDecimal("1.756"));
        oddsService.consumeOddsFeed(List.of(update));

        assertEquals(new BigDecimal("1.76"), oddsService.getOdds("evt", "home").orElseThrow());
    }
}
