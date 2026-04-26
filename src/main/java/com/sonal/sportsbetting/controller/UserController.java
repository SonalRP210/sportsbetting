package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.service.BettingService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("${api.base-path:/api/v1}")
public class UserController {

    private final BettingService service;

    public UserController(BettingService service) {
        this.service = service;
    }

    @GetMapping(AppConstants.Api.USER_BETS)
    public ResponseEntity<List<UserBetSummaryResponse>> userBets(
            @PathVariable String userId,
            @RequestParam(defaultValue = "${app.pagination.default-page}") @Min(0) int page,
            @RequestParam(defaultValue = "${app.pagination.default-page-size}") @Min(1) int size) {
        return ResponseEntity.ok(service.getUserSummary(userId, page, size));
    }

    @GetMapping(AppConstants.Api.USER_EXPOSURE)
    public ResponseEntity<UserExposureResponse> userExposure(@PathVariable String userId) {
        return ResponseEntity.ok(service.getUserExposure(userId));
    }
}
