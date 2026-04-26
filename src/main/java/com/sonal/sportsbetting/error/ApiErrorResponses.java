package com.sonal.sportsbetting.error;

import com.sonal.sportsbetting.properties.CorrelationProperties;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

public final class ApiErrorResponses {

    private ApiErrorResponses() {
    }

    public static ApiErrorResponse of(
            CorrelationProperties correlation,
            String code,
            String message,
            Map<String, String> fieldErrors) {
        String traceId = MDC.get(correlation.mdcKey());
        return new ApiErrorResponse(code, message, fieldErrors, Instant.now(), traceId);
    }
}
