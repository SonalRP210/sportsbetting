package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.OddsCacheUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsCacheUpdaterTest {

    private OddsCacheUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new OddsCacheUpdater(PropertyFixtures.odds(), PropertyFixtures.moneyFormatting());
    }

    @Test
    void updateFromDispatchedStoresNormalizedValue() {
        updater.updateFromDispatched(new OddsUpdatedPayload("evt-1", "home", new BigDecimal("1.756")));

        assertEquals(new BigDecimal("1.76"), updater.getCached("evt-1", "home").orElseThrow());
    }

    @Test
    void updateFromBroadcastStoresNormalizedValue() {
        OddsUpdate update = new OddsUpdate();
        update.setEventId("evt-2");
        update.setSelection("away");
        update.setOdds(new BigDecimal("2.119"));
        updater.updateFromBroadcast(update);

        assertTrue(updater.getCached("evt-2", "away").isPresent());
        assertEquals(new BigDecimal("2.12"), updater.getCached("evt-2", "away").orElseThrow());
    }
}
