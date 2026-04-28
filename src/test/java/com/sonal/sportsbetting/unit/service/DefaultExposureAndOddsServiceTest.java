package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.model.ExposureProjection;
import com.sonal.sportsbetting.model.ExposureProjectionKey;
import com.sonal.sportsbetting.model.LatestOdds;
import com.sonal.sportsbetting.model.LatestOddsId;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.repository.LatestOddsRepository;
import com.sonal.sportsbetting.service.DefaultExposureService;
import com.sonal.sportsbetting.service.DefaultOddsService;
import com.sonal.sportsbetting.service.OddsCacheUpdater;
import com.sonal.sportsbetting.service.outbox.DomainEventPublisher;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultExposureAndOddsServiceTest {

    @Mock
    private ExposureProjectionRepository exposureProjectionRepository;

    @Mock
    private LatestOddsRepository latestOddsRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private OddsCacheUpdater oddsCacheUpdater;
    private DefaultExposureService exposureService;
    private DefaultOddsService oddsService;

    @BeforeEach
    void setUp() {
        exposureService = new DefaultExposureService(
                exposureProjectionRepository,
                PropertyFixtures.moneyFormatting(),
                new SimpleMeterRegistry());
        oddsCacheUpdater = new OddsCacheUpdater(PropertyFixtures.odds(), PropertyFixtures.moneyFormatting());
        oddsService = new DefaultOddsService(
                new SimpleMeterRegistry(),
                PropertyFixtures.moneyFormatting(),
                latestOddsRepository,
                domainEventPublisher,
                oddsCacheUpdater);
    }

    @Test
    void totalExposureReadsGlobalProjection() {
        when(exposureProjectionRepository.findById(ExposureProjectionKey.global()))
                .thenReturn(Optional.of(new ExposureProjection(ExposureProjectionKey.global(), new BigDecimal("12.25"), 0)));

        assertEquals(0, exposureService.getTotalExposure().compareTo(new BigDecimal("12.25")));
    }

    @Test
    void totalExposureReturnsZeroWhenProjectionMissing() {
        when(exposureProjectionRepository.findById(ExposureProjectionKey.global())).thenReturn(Optional.empty());

        assertEquals(0, exposureService.getTotalExposure().compareTo(BigDecimal.ZERO));
    }

    @Test
    void userExposureReadsUserProjection() {
        when(exposureProjectionRepository.findById(ExposureProjectionKey.forUser("u1")))
                .thenReturn(Optional.of(new ExposureProjection(ExposureProjectionKey.forUser("u1"), new BigDecimal("20.00"), 1)));

        var result = exposureService.getUserExposure("u1");

        assertEquals(1, result.openBetCount());
        assertEquals(new BigDecimal("20.00"), result.openExposure());
    }

    @Test
    void oddsServiceStoresScaledOddsViaOutboxAndRepository() {
        OddsUpdate update = new OddsUpdate();
        update.setEventId("evt");
        update.setSelection("home");
        update.setOdds(new BigDecimal("1.756"));
        when(latestOddsRepository.findById(any(LatestOddsId.class))).thenReturn(Optional.empty());
        when(latestOddsRepository.save(any(LatestOdds.class))).thenAnswer(invocation -> invocation.getArgument(0));

        oddsService.consumeOddsFeed(List.of(update));

        verify(domainEventPublisher).publish(any(), any());
    }

    @Test
    void getOddsReadThroughFromRepository() {
        LatestOddsId id = new LatestOddsId("evt", "home");
        LatestOdds row = new LatestOdds(id, new BigDecimal("1.756"));
        when(latestOddsRepository.findById(id)).thenReturn(Optional.of(row));

        assertEquals(new BigDecimal("1.76"), oddsService.getOdds("evt", "home").orElseThrow());
    }
}
