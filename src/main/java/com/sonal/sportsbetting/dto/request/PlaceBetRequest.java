package com.sonal.sportsbetting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlaceBetRequest(
        @NotBlank @Size(max = 64) String userId,
        @NotBlank @Size(max = 64) String eventId,
        @NotBlank @Size(max = 64) String selection,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal stake
) {
}
