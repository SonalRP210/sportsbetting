package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;

public interface BetPlacementService {

    default PlaceBetResponse placeBet(PlaceBetRequest request) {
        return placeBet(request, null);
    }

    PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey);
}
