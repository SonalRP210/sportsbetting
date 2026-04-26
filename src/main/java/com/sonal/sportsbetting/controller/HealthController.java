package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("${api.base-path:/api/v1}")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name:sportsbetting}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping(AppConstants.Api.HEALTH)
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                AppConstants.Messages.HEALTH_UP,
                applicationName,
                Instant.now()
        ));
    }
}
