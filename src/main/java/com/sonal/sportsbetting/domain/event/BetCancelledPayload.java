package com.sonal.sportsbetting.domain.event;

import java.math.BigDecimal;

public record BetCancelledPayload(String betId, String userId, BigDecimal openRisk) {
}
