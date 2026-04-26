package com.sonal.sportsbetting.dto.response;

import java.math.BigDecimal;

public record UserBetSummaryResponse(
        String betId,
        String eventId,
        BigDecimal stake,
        BigDecimal odds,
        String status
) {
}
