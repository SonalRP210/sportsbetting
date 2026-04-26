package com.sonal.sportsbetting.dto;

public record CancelBetResponse(
        String betId,
        String status,
        String message
) {
}
