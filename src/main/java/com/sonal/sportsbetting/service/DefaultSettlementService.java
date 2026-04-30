package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.domain.event.EventSettledPayload;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.EventSettlement;
import com.sonal.sportsbetting.properties.SettlementProperties;
import com.sonal.sportsbetting.properties.SettlementRetryProperties;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.outbox.DomainEventPublisher;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DefaultSettlementService implements SettlementService {
    private static final Logger log = LoggerFactory.getLogger(DefaultSettlementService.class);

    private final BetRepository betRepository;
    private final EventSettlementRepository eventSettlementRepository;
    private final ExposureService exposureService;
    private final DomainEventPublisher domainEventPublisher;
    private final SettlementRetryProperties settlementRetryProperties;
    private final SettlementProperties settlementProperties;
    private final MeterRegistry meterRegistry;
    private final MoneyFormatting moneyFormatting;

    private final Counter settlementSuccessCounter;
    private final Counter settlementConflictCounter;
    private final Counter settlementIdempotentCounter;
    private final Counter settlementLockFailureCounter;

    public DefaultSettlementService(
            BetRepository betRepository,
            EventSettlementRepository eventSettlementRepository,
            ExposureService exposureService,
            DomainEventPublisher domainEventPublisher,
            SettlementRetryProperties settlementRetryProperties,
            SettlementProperties settlementProperties,
            MeterRegistry meterRegistry,
            MoneyFormatting moneyFormatting) {
        this.betRepository = betRepository;
        this.eventSettlementRepository = eventSettlementRepository;
        this.exposureService = exposureService;
        this.domainEventPublisher = domainEventPublisher;
        this.settlementRetryProperties = settlementRetryProperties;
        this.settlementProperties = settlementProperties;
        this.meterRegistry = meterRegistry;
        this.moneyFormatting = moneyFormatting;

        // Pre-register counters for better performance and consistent naming
        this.settlementSuccessCounter = meterRegistry.counter("settlement.success");
        this.settlementConflictCounter = meterRegistry.counter("settlement.conflicts");
        this.settlementIdempotentCounter = meterRegistry.counter("settlement.idempotent_replays");
        this.settlementLockFailureCounter = meterRegistry.counter("settlement.lock.failures");
    }

    @Override
    @Transactional
    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        Timer.Sample durationSample = Timer.start(meterRegistry);
        try {
            Optional<EventSettlement> prior = eventSettlementRepository.findById(eventId);
            if (prior.isPresent()) {
                EventSettlement ledger = prior.get();
                if (!Objects.equals(ledger.getWinningSelection(), winningSelection)) {
                    settlementConflictCounter.increment();
                    throw new SettlementConflictException(
                            "Event " + eventId + " was already settled with selection " + ledger.getWinningSelection());
                }
                settlementIdempotentCounter.increment();
                log.info("Idempotent settlement replay eventId={}", eventId);
                return buildResponseFromLedger(ledger);
            }

            List<Bet> openBets;
            try {
                long lockStartNanos = System.nanoTime();
                openBets = findOpenBetsWithRetry(eventId);
                long lockWaitMillis = Duration.ofNanos(System.nanoTime() - lockStartNanos).toMillis();
                meterRegistry.timer("events.settlement.lock.wait").record(Duration.ofMillis(lockWaitMillis));
            } catch (PessimisticLockingFailureException | QueryTimeoutException ex) {
                settlementLockFailureCounter.increment();
                throw new IllegalStateException(
                        "Unable to acquire settlement lock within " + settlementProperties.lockTimeoutMs() + "ms", ex);
            }

            if (openBets.isEmpty()) {
                return new SettleEventResponse(
                        eventId,
                        winningSelection,
                        0,
                        0,
                        BigDecimal.ZERO,
                        exposureService.getTotalExposure());
            }

            List<EventSettledPayload.RiskRelease> releases = new ArrayList<>();
            for (Bet bet : openBets) {
                releases.add(new EventSettledPayload.RiskRelease(
                        bet.getUserId(),
                        moneyFormatting.normalize(bet.getStake().multiply(bet.getOdds()))));
            }

            int winners = 0;
            int losers = 0;
            BigDecimal payout = BigDecimal.ZERO;

            for (Bet bet : openBets) {
                BigDecimal riskAmount = moneyFormatting.normalize(bet.getStake().multiply(bet.getOdds()));
                if (Objects.equals(bet.getSelection(), winningSelection)) {
                    bet.setStatus(BetStatus.WON);
                    payout = moneyFormatting.normalize(payout.add(riskAmount));
                    winners++;
                } else {
                    bet.setStatus(BetStatus.LOST);
                    losers++;
                }
                betRepository.save(bet);
            }

            EventSettlement ledger = new EventSettlement(eventId, winningSelection, winners, losers, payout);
            eventSettlementRepository.save(ledger);

            domainEventPublisher.publish(
                    DomainEventType.EVENT_SETTLED,
                    new EventSettledPayload(eventId, winningSelection, releases));

            settlementSuccessCounter.increment();
            log.info("Settled event eventId={} winner={} winners={} losers={}", eventId, winningSelection, winners, losers);
            return new SettleEventResponse(
                    eventId,
                    winningSelection,
                    winners,
                    losers,
                    payout,
                    exposureService.getTotalExposure());
        } finally {
            durationSample.stop(Timer.builder("settlement.duration")
                    .tag("eventId", eventId)
                    .register(meterRegistry));
        }
    }

    private SettleEventResponse buildResponseFromLedger(EventSettlement ledger) {
        return new SettleEventResponse(
                ledger.getEventId(),
                ledger.getWinningSelection(),
                ledger.getWinners(),
                ledger.getLosers(),
                ledger.getTotalPayout(),
                exposureService.getTotalExposure());
    }

    private List<Bet> findOpenBetsWithRetry(String eventId) {
        int maxAttempts = Math.max(settlementRetryProperties.maxAttempts(), 1);
        long backoffMillis = Math.max(settlementRetryProperties.backoffMillis(), 0L);
        PessimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return betRepository.findByEventIdAndStatusForUpdate(eventId, BetStatus.OPEN);
            } catch (PessimisticLockingFailureException | QueryTimeoutException ex) {
                if (ex instanceof PessimisticLockingFailureException p) {
                    last = p;
                } else {
                    last = new PessimisticLockingFailureException(ex.getMessage(), ex);
                }
                if (attempt == maxAttempts) {
                    throw last;
                }
                if (backoffMillis > 0) {
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while waiting to retry settlement lock acquisition", interrupted);
                    }
                }
            }
        }
        throw new IllegalStateException("Unexpected settlement retry loop termination");
    }
}
