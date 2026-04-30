package com.sonal.sportsbetting;

import com.sonal.sportsbetting.properties.BettingProperties;
import com.sonal.sportsbetting.properties.CorrelationProperties;
import com.sonal.sportsbetting.properties.OddsProperties;
import com.sonal.sportsbetting.properties.PersistenceRetryProperties;
import com.sonal.sportsbetting.properties.SettlementProperties;
import com.sonal.sportsbetting.properties.SettlementRetryProperties;
import com.sonal.sportsbetting.support.MoneyFormatting;

/**
 * Mirrors {@code application.yaml} defaults for unit tests that do not start the full Spring context.
 */
public final class PropertyFixtures {

    private PropertyFixtures() {
    }

    public static BettingProperties betting() {
        return new BettingProperties("BET-", 2, "HALF_UP", 128);
    }

    public static MoneyFormatting moneyFormatting() {
        return new MoneyFormatting(betting());
    }

    public static OddsProperties odds() {
        return new OddsProperties(":");
    }

    public static PersistenceRetryProperties persistenceRetry() {
        return new PersistenceRetryProperties(3, 50L);
    }

    public static SettlementRetryProperties settlementRetry() {
        return new SettlementRetryProperties(3, 100L);
    }

    public static SettlementProperties settlement() {
        return new SettlementProperties(2000);
    }

    public static CorrelationProperties correlation() {
        return new CorrelationProperties("X-Request-Id", "X-Request-Id", "traceId");
    }
}
