package com.sonal.sportsbetting.dto.response;

import java.math.BigDecimal;

public record PlaceBetResponse(
        String betId,
        BigDecimal acceptedOdds,
        String status
) {
}
