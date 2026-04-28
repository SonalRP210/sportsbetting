package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.LatestOdds;
import com.sonal.sportsbetting.model.LatestOddsId;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.OddsProperties;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultOddsService implements OddsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultOddsService.class);

    private final MeterRegistry meterRegistry;
    private final MoneyFormatting moneyFormatting;
    private final OddsProperties oddsProperties;
    private final OddsUpdateBroadcaster oddsUpdateBroadcaster;
    private final LatestOddsRepository latestOddsRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Map<String, BigDecimal> readThroughCache = new ConcurrentHashMap<>();

    public DefaultOddsService(
            MeterRegistry meterRegistry,
            MoneyFormatting moneyFormatting,
            OddsProperties oddsProperties,
            OddsUpdateBroadcaster oddsUpdateBroadcaster,
            LatestOddsRepository latestOddsRepository,
            DomainEventPublisher domainEventPublisher) {
        this.meterRegistry = meterRegistry;
        this.moneyFormatting = moneyFormatting;
        this.oddsProperties = oddsProperties;
        this.oddsUpdateBroadcaster = oddsUpdateBroadcaster;
        this.latestOddsRepository = latestOddsRepository;
        this.domainEventPublisher = domainEventPublisher;
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
        String key = buildOddsKey(eventId, selection);
        BigDecimal cached = readThroughCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        LatestOddsId id = new LatestOddsId(eventId, selection);
        Optional<BigDecimal> fromStore = latestOddsRepository
                .findById(id)
                .map(LatestOdds::getOdds)
                .map(moneyFormatting::normalize);
        fromStore.ifPresent(o -> readThroughCache.put(key, o));
        return fromStore;
    }

    /**
     * Invoked after commit by the outbox dispatcher: fan-out broadcast and warm local read-through cache.
     */
    public void applyDispatchedOddsUpdate(OddsUpdatedPayload payload) {
        String key = buildOddsKey(payload.eventId(), payload.selection());
        BigDecimal odds = moneyFormatting.normalize(payload.odds());
        readThroughCache.put(key, odds);
        OddsUpdate update = new OddsUpdate();
        update.setEventId(payload.eventId());
        update.setSelection(payload.selection());
        update.setOdds(odds);
        oddsUpdateBroadcaster.broadcast(update);
    }

    /**
     * Cross-instance odds fan-out (e.g. Redis pub/sub): treat payload as hint and refresh local read model.
     */
    public void applyBroadcastUpdate(OddsUpdate update) {
        String key = buildOddsKey(update.getEventId(), update.getSelection());
        BigDecimal odds = moneyFormatting.normalize(update.getOdds());
        readThroughCache.put(key, odds);
    }

    private String buildOddsKey(String eventId, String selection) {
        return eventId + oddsProperties.compositeKeySeparator() + selection;
    }
}
