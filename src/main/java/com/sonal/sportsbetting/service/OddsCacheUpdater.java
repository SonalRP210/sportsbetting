package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.OddsProperties;
import com.sonal.sportsbetting.support.MoneyFormatting;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OddsCacheUpdater {
    private final OddsProperties oddsProperties;
    private final MoneyFormatting moneyFormatting;
    private final Map<String, BigDecimal> cache = new ConcurrentHashMap<>();

    public OddsCacheUpdater(OddsProperties oddsProperties, MoneyFormatting moneyFormatting) {
        this.oddsProperties = oddsProperties;
        this.moneyFormatting = moneyFormatting;
    }

    public Optional<BigDecimal> getCached(String eventId, String selection) {
        return Optional.ofNullable(cache.get(buildOddsKey(eventId, selection)));
    }

    public void updateFromDispatched(OddsUpdatedPayload payload) {
        cache.put(
                buildOddsKey(payload.eventId(), payload.selection()),
                moneyFormatting.normalize(payload.odds()));
    }

    public void updateFromBroadcast(OddsUpdate update) {
        cache.put(
                buildOddsKey(update.getEventId(), update.getSelection()),
                moneyFormatting.normalize(update.getOdds()));
    }

    public void put(String eventId, String selection, BigDecimal odds) {
        cache.put(buildOddsKey(eventId, selection), moneyFormatting.normalize(odds));
    }

    private String buildOddsKey(String eventId, String selection) {
        return eventId + oddsProperties.compositeKeySeparator() + selection;
    }
}
