package com.sonal.sportsbetting.exception;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.error.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return new ApiErrorResponse(
                AppConstants.Messages.VALIDATION_ERROR,
                "Request validation failed",
                fieldErrors,
                Instant.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        return new ApiErrorResponse(
                AppConstants.Messages.VALIDATION_ERROR,
                ex.getMessage(),
                Map.of(),
                Instant.now()
        );
    }

    @ExceptionHandler(BetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(BetNotFoundException ex) {
        return new ApiErrorResponse(
                AppConstants.Messages.BET_NOT_FOUND,
                ex.getMessage(),
                Map.of(),
                Instant.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleBusiness(IllegalArgumentException ex) {
        return new ApiErrorResponse(
                AppConstants.Messages.BUSINESS_RULE_VIOLATION,
                ex.getMessage(),
                Map.of(),
                Instant.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex) {
        return new ApiErrorResponse(
                AppConstants.Messages.INTERNAL_ERROR,
                "Unexpected server error",
                Map.of(),
                Instant.now()
        );
    }
}
