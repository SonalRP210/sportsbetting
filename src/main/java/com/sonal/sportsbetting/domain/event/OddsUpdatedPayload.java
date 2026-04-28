package com.sonal.sportsbetting.domain.event;

import java.math.BigDecimal;

public record OddsUpdatedPayload(String eventId, String selection, BigDecimal odds) {
}
