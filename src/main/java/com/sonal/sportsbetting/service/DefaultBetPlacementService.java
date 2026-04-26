package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.config.PersistenceRetryConfiguration;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.properties.BettingProperties;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class DefaultBetPlacementService implements BetPlacementService {
    private static final Logger log = LoggerFactory.getLogger(DefaultBetPlacementService.class);

    private final BetRepository betRepository;
    private final OddsService oddsService;
    private final ExposureService exposureService;
    private final MeterRegistry meterRegistry;
    private final RetryTemplate persistenceRetryTemplate;
    private final MoneyFormatting moneyFormatting;
    private final BettingProperties bettingProperties;

    public DefaultBetPlacementService(
            BetRepository betRepository,
            OddsService oddsService,
            ExposureService exposureService,
            MeterRegistry meterRegistry,
            @Qualifier(PersistenceRetryConfiguration.PERSISTENCE_RETRY_TEMPLATE) RetryTemplate persistenceRetryTemplate,
            MoneyFormatting moneyFormatting,
            BettingProperties bettingProperties) {
        this.betRepository = betRepository;
        this.oddsService = oddsService;
        this.exposureService = exposureService;
        this.meterRegistry = meterRegistry;
        this.persistenceRetryTemplate = persistenceRetryTemplate;
        this.moneyFormatting = moneyFormatting;
        this.bettingProperties = bettingProperties;
    }

    @Override
    @Transactional
    public PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey != null) {
            Optional<Bet> existing = betRepository.findByUserIdAndIdempotencyKey(request.userId(), normalizedKey);
            if (existing.isPresent()) {
                meterRegistry.counter("bets.placed.idempotent_replay").increment();
                log.info("Idempotent bet replay userId={} idempotencyKey={} betId={}",
                        request.userId(), normalizedKey, existing.get().getBetId());
                return toResponse(existing.get());
            }
        }

        BigDecimal odds = oddsService.getOdds(request.eventId(), request.selection())
                .orElseThrow(() -> new IllegalArgumentException("No active odds found for event/selection"));
        validateBetPlacement(request, odds);

        String betId = bettingProperties.betIdPrefix() + UUID.randomUUID();
        Bet bet = new Bet(
                betId,
                request.userId(),
                request.eventId(),
                moneyFormatting.normalize(request.stake()),
                moneyFormatting.normalize(odds),
                request.selection(),
                BetStatus.OPEN
        );
        bet.setIdempotencyKey(normalizedKey);

        try {
            persistenceRetryTemplate.execute(context -> betRepository.save(bet));
        } catch (DataIntegrityViolationException ex) {
            if (normalizedKey == null) {
                throw ex;
            }
            Bet recovered = betRepository.findByUserIdAndIdempotencyKey(request.userId(), normalizedKey)
                    .orElseThrow(() -> ex);
            meterRegistry.counter("bets.placed.idempotent_replay_after_race").increment();
            log.warn("Resolved duplicate idempotency key after constraint violation userId={} betId={}",
                    request.userId(), recovered.getBetId());
            return toResponse(recovered);
        }

        BigDecimal exposure = moneyFormatting.normalize(request.stake().multiply(odds));
        exposureService.increaseExposure(exposure);
        meterRegistry.counter("bets.placed.total").increment();
        log.info("Placed bet betId={} userId={} eventId={} selection={}", betId, request.userId(), request.eventId(), request.selection());

        return toResponse(bet);
    }

    private PlaceBetResponse toResponse(Bet bet) {
        return new PlaceBetResponse(
                bet.getBetId(),
                moneyFormatting.normalize(bet.getOdds()),
                bet.getStatus().name());
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int maxLen = bettingProperties.idempotencyKeyMaxLength();
        if (trimmed.length() > maxLen) {
            throw new IllegalArgumentException("Idempotency-Key must be at most " + maxLen + " characters");
        }
        return trimmed;
    }

    private void validateBetPlacement(PlaceBetRequest request, BigDecimal odds) {
        if (request.stake().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be greater than zero");
        }
        if (odds.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Odds must be greater than zero");
        }
    }
}
