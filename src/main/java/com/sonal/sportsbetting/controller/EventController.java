package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.dto.SettleEventRequest;
import com.sonal.sportsbetting.dto.SettleEventResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.service.BettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("${api.base-path:/api/v1}")
public class EventController {

    private final BettingService service;

    public EventController(BettingService service) {
        this.service = service;
    }

    @PostMapping(AppConstants.Api.EVENT_SETTLEMENTS)
    public ResponseEntity<SettleEventResponse> settle(@Valid @RequestBody SettleEventRequest request) {
        return ResponseEntity.ok(service.settleEvent(request.eventId(), request.winningSelection()));
    }

    @GetMapping(AppConstants.Api.EVENT_BETS)
    public ResponseEntity<List<UserBetSummaryResponse>> eventBets(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(service.getEventBets(eventId, page, size));
    }
}
