package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.OddsProperties;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final Map<String, BigDecimal> latestOdds = new ConcurrentHashMap<>();

    public DefaultOddsService(
            MeterRegistry meterRegistry,
            MoneyFormatting moneyFormatting,
            OddsProperties oddsProperties,
            OddsUpdateBroadcaster oddsUpdateBroadcaster) {
        this.meterRegistry = meterRegistry;
        this.moneyFormatting = moneyFormatting;
        this.oddsProperties = oddsProperties;
        this.oddsUpdateBroadcaster = oddsUpdateBroadcaster;
    }

    @Override
    public void consumeOddsFeed(List<OddsUpdate> feedEvents) {
        for (OddsUpdate update : feedEvents) {
            applyBroadcastUpdate(update);
            oddsUpdateBroadcaster.broadcast(update);
            meterRegistry.counter("odds.feed.events.processed").increment();
        }
        log.info("Processed odds feed batch size={}", feedEvents.size());
    }

    @Override
    public Optional<BigDecimal> getOdds(String eventId, String selection) {
        return Optional.ofNullable(latestOdds.get(buildOddsKey(eventId, selection)));
    }

    public void applyBroadcastUpdate(OddsUpdate update) {
        String key = buildOddsKey(update.getEventId(), update.getSelection());
        latestOdds.put(key, moneyFormatting.normalize(update.getOdds()));
    }

    private String buildOddsKey(String eventId, String selection) {
        return eventId + oddsProperties.compositeKeySeparator() + selection;
    }
}
