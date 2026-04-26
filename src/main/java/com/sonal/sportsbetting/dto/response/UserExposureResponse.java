package com.sonal.sportsbetting.dto.response;

import java.math.BigDecimal;

public record UserExposureResponse(
        String userId,
        BigDecimal openExposure,
        int openBetCount
) {
}
