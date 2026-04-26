package com.sonal.sportsbetting.dto.response;

public record CancelBetResponse(
        String betId,
        String status,
        String message
) {
}
