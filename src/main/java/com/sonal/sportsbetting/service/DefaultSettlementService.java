package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.domain.event.EventSettledPayload;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.exception.SettlementConflictException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.EventSettlement;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.EventSettlementRepository;
import com.sonal.sportsbetting.service.outbox.DomainEventPublisher;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final MeterRegistry meterRegistry;
    private final MoneyFormatting moneyFormatting;

    public DefaultSettlementService(
            BetRepository betRepository,
            EventSettlementRepository eventSettlementRepository,
            ExposureService exposureService,
            DomainEventPublisher domainEventPublisher,
            MeterRegistry meterRegistry,
            MoneyFormatting moneyFormatting) {
        this.betRepository = betRepository;
        this.eventSettlementRepository = eventSettlementRepository;
        this.exposureService = exposureService;
        this.domainEventPublisher = domainEventPublisher;
        this.meterRegistry = meterRegistry;
        this.moneyFormatting = moneyFormatting;
    }

    @Override
    @Transactional
    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        Optional<EventSettlement> prior = eventSettlementRepository.findById(eventId);
        if (prior.isPresent()) {
            EventSettlement ledger = prior.get();
            if (!Objects.equals(ledger.getWinningSelection(), winningSelection)) {
                throw new SettlementConflictException(
                        "Event " + eventId + " was already settled with selection " + ledger.getWinningSelection());
            }
            meterRegistry.counter("events.settled.idempotent_replay").increment();
            log.info("Idempotent settlement replay eventId={}", eventId);
            return buildResponseFromLedger(ledger);
        }

        List<Bet> openBets = betRepository.findByEventIdAndStatusForUpdate(eventId, BetStatus.OPEN);
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

        meterRegistry.counter("events.settled.total").increment();
        log.info("Settled event eventId={} winner={} winners={} losers={}", eventId, winningSelection, winners, losers);
        return new SettleEventResponse(
                eventId,
                winningSelection,
                winners,
                losers,
                payout,
                exposureService.getTotalExposure());
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
}
