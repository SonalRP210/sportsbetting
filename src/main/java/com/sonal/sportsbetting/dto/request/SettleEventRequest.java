package com.sonal.sportsbetting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SettleEventRequest(
        @NotBlank String eventId,
        @NotBlank String winningSelection
) {
}
