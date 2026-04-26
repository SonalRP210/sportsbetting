package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;

public interface BetPlacementService {

    default PlaceBetResponse placeBet(PlaceBetRequest request) {
        return placeBet(request, null);
    }

    PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey);
}
