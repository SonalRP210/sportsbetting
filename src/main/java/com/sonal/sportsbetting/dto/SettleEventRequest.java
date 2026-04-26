package com.sonal.sportsbetting.dto;

import jakarta.validation.constraints.NotBlank;

public record SettleEventRequest(
        @NotBlank String eventId,
        @NotBlank String winningSelection
) {
}
