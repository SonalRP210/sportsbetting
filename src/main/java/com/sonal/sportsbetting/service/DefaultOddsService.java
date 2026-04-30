package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.LatestOdds;
import com.sonal.sportsbetting.model.LatestOddsId;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.LatestOddsRepository;
import com.sonal.sportsbetting.service.outbox.DomainEventPublisher;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultOddsService implements OddsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultOddsService.class);

    private final MeterRegistry meterRegistry;
    private final MoneyFormatting moneyFormatting;
    private final LatestOddsRepository latestOddsRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OddsCacheUpdater oddsCacheUpdater;

    public DefaultOddsService(
            MeterRegistry meterRegistry,
            MoneyFormatting moneyFormatting,
            LatestOddsRepository latestOddsRepository,
            DomainEventPublisher domainEventPublisher,
            OddsCacheUpdater oddsCacheUpdater) {
        this.meterRegistry = meterRegistry;
        this.moneyFormatting = moneyFormatting;
        this.latestOddsRepository = latestOddsRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.oddsCacheUpdater = oddsCacheUpdater;
    }

    @Override
    @Transactional
    public void consumeOddsFeed(List<OddsUpdate> feedEvents) {
        for (OddsUpdate update : feedEvents) {
            BigDecimal odds = moneyFormatting.normalize(update.getOdds());
            LatestOddsId id = new LatestOddsId(update.getEventId(), update.getSelection());
            LatestOdds row = latestOddsRepository.findById(id).orElseGet(() -> new LatestOdds(id, odds));
            row.setId(id);
            row.setOdds(odds);
            latestOddsRepository.save(row);
            domainEventPublisher.publish(
                    DomainEventType.ODDS_UPDATED,
                    new OddsUpdatedPayload(update.getEventId(), update.getSelection(), odds));
            meterRegistry.counter("odds.feed.events.processed").increment();
        }
        log.info("Processed odds feed batch size={}", feedEvents.size());
    }

    @Override
    public Optional<BigDecimal> getOdds(String eventId, String selection) {
        Optional<BigDecimal> cached = oddsCacheUpdater.getCached(eventId, selection);
        if (cached.isPresent()) {
            return cached;
        }
        LatestOddsId id = new LatestOddsId(eventId, selection);
        Optional<BigDecimal> fromStore = latestOddsRepository
                .findById(id)
                .map(LatestOdds::getOdds)
                .map(moneyFormatting::normalize);
        fromStore.ifPresent(o -> oddsCacheUpdater.put(eventId, selection, o));
        return fromStore;
    }
}
