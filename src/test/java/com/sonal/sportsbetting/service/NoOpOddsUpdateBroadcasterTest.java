package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOpOddsUpdateBroadcasterTest {

    @Test
    void broadcastDoesNotThrowAndCompletes() {
        NoOpOddsUpdateBroadcaster broadcaster = new NoOpOddsUpdateBroadcaster();
        OddsUpdate update = new OddsUpdate();
        update.setEventId("evt-1");
        update.setSelection("home");
        update.setOdds(new BigDecimal("1.95"));

        assertDoesNotThrow(() -> broadcaster.broadcast(update));
    }
}
