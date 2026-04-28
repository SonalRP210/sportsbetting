package com.sonal.sportsbetting.domain.event;

import java.math.BigDecimal;

public record BetPlacedPayload(String betId, String userId, BigDecimal openRisk) {
}
