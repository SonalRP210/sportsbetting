package com.sonal.sportsbetting.dto.response;

import java.math.BigDecimal;

public record SettleEventResponse(
        String eventId,
        String winningSelection,
        int winners,
        int losers,
        BigDecimal totalPayout,
        BigDecimal globalExposure
) {
}
