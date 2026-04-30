package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * When Redis is disabled, cross-node odds fan-out is unavailable; odds updates stay on this JVM only.
 * Logging is DEBUG to avoid noise on hot paths — enable DEBUG for this class to trace fan-out skips.
 */
@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOddsUpdateBroadcaster implements OddsUpdateBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(NoOpOddsUpdateBroadcaster.class);

    @Override
    public void broadcast(OddsUpdate update) {
        log.debug(
                "Odds fan-out skipped (app.redis.enabled=false): update applied on this instance only. "
                        + "eventId={} selection={} odds={}",
                update.getEventId(),
                update.getSelection(),
                update.getOdds());
    }
}
