package com.sonal.sportsbetting.dto;

import java.math.BigDecimal;

public record UserExposureResponse(
        String userId,
        BigDecimal openExposure,
        int openBetCount
) {
}
