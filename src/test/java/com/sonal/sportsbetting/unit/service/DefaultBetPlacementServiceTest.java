package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.config.PersistenceRetryConfiguration;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.properties.BettingProperties;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.service.DefaultBetPlacementService;
import com.sonal.sportsbetting.service.ExposureService;
import com.sonal.sportsbetting.service.OddsService;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBetPlacementServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private OddsService oddsService;

    @Mock
    private ExposureService exposureService;

    private DefaultBetPlacementService service;
    private RetryTemplate persistenceRetryTemplate;
    private MoneyFormatting moneyFormatting;
    private BettingProperties bettingProperties;

    @BeforeEach
    void setUp() {
        bettingProperties = PropertyFixtures.betting();
        moneyFormatting = PropertyFixtures.moneyFormatting();
        persistenceRetryTemplate = PersistenceRetryConfiguration.createPersistenceRetryTemplate(
                PropertyFixtures.persistenceRetry());
        service = new DefaultBetPlacementService(
                betRepository,
                oddsService,
                exposureService,
                new SimpleMeterRegistry(),
                persistenceRetryTemplate,
                moneyFormatting,
                bettingProperties);
    }

    @Test
    void placeBetPersistsBetAndReturnsResponse() {
        PlaceBetRequest request = new PlaceBetRequest("u1", "e1", "home", new BigDecimal("10.00"));
        when(oddsService.getOdds("e1", "home")).thenReturn(Optional.of(new BigDecimal("1.75")));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceBetResponse response = service.placeBet(request);

        assertNotNull(response.betId());
        assertEquals("OPEN", response.status());
        assertEquals(new BigDecimal("1.75"), response.acceptedOdds());

        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
        verify(exposureService).increaseExposure(new BigDecimal("17.50"));
    }

    @Test
    void placeBetWithSameIdempotencyKeyReturnsExistingWithoutSecondSave() {
        PlaceBetRequest request = new PlaceBetRequest("u1", "e1", "home", new BigDecimal("10.00"));
        Bet existing = new Bet("BET-existing", "u1", "e1", new BigDecimal("10.00"), new BigDecimal("1.75"), "home", BetStatus.OPEN);
        existing.setIdempotencyKey("key-1");
        when(betRepository.findByUserIdAndIdempotencyKey("u1", "key-1")).thenReturn(Optional.of(existing));

        PlaceBetResponse response = service.placeBet(request, "key-1");

        assertEquals("BET-existing", response.betId());
        verify(betRepository, never()).save(any());
        verify(oddsService, never()).getOdds(any(), any());
        verify(exposureService, never()).increaseExposure(any());
    }

    @Test
    void placeBetThrowsWhenOddsMissing() {
        PlaceBetRequest request = new PlaceBetRequest("u1", "e1", "home", new BigDecimal("10.00"));
        when(oddsService.getOdds("e1", "home")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.placeBet(request));
    }

    @Test
    void placeBetWithWhitespaceOnlyIdempotencyKeyBehavesLikeAbsentKey() {
        PlaceBetRequest request = new PlaceBetRequest("u1", "e1", "home", new BigDecimal("10.00"));
        when(oddsService.getOdds("e1", "home")).thenReturn(Optional.of(new BigDecimal("2.00")));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.placeBet(request, "   \t  ");

        verify(betRepository, never()).findByUserIdAndIdempotencyKey(any(), any());
        verify(oddsService).getOdds("e1", "home");
        verify(betRepository).save(any(Bet.class));
    }

    @Test
    void placeBetRejectsIdempotencyKeyLongerThanConfiguredMax() {
        PlaceBetRequest request = new PlaceBetRequest("u1", "e1", "home", new BigDecimal("10.00"));
        String tooLong = "x".repeat(129);
        assertThrows(IllegalArgumentException.class, () -> service.placeBet(request, tooLong));
        verify(betRepository, never()).save(any());
    }
}
