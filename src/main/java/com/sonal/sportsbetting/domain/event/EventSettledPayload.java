package com.sonal.sportsbetting.domain.event;

import java.math.BigDecimal;
import java.util.List;

public record EventSettledPayload(String eventId, String winningSelection, List<RiskRelease> releases) {

    public record RiskRelease(String userId, BigDecimal openRisk) {
    }
}
