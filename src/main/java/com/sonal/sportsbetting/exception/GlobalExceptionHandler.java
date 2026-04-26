package com.sonal.sportsbetting.exception;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.error.ApiErrorResponse;
import com.sonal.sportsbetting.error.ApiErrorResponses;
import com.sonal.sportsbetting.properties.CorrelationProperties;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final CorrelationProperties correlationProperties;

    public GlobalExceptionHandler(CorrelationProperties correlationProperties) {
        this.correlationProperties = correlationProperties;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.VALIDATION_ERROR,
                "Request validation failed",
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.VALIDATION_ERROR,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(BetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(BetNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.BET_NOT_FOUND,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleBusiness(IllegalArgumentException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.BUSINESS_RULE_VIOLATION,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiErrorResponse handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.RATE_LIMITED,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(SettlementConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleSettlementConflict(SettlementConflictException ex) {
        log.warn("Settlement conflict: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.SETTLEMENT_CONFLICT,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(TransientServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleTransient(TransientServiceException ex) {
        log.warn("Transient dependency failure: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.TRANSIENT_FAILURE,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.CONCURRENT_MODIFICATION,
                "Resource was modified concurrently; retry the request",
                Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.DUPLICATE_RESOURCE,
                "Conflicting or duplicate data",
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex) {
        log.error("Unexpected server error", ex);
        return ApiErrorResponses.of(
                correlationProperties,
                AppConstants.Messages.INTERNAL_ERROR,
                "Unexpected server error",
                Map.of()
        );
    }
}
