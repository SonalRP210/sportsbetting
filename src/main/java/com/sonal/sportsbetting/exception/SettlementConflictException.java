package com.sonal.sportsbetting.exception;

/**
 * Raised when an event was already settled with a different winning selection (idempotency conflict).
 */
public class SettlementConflictException extends RuntimeException {

    public SettlementConflictException(String message) {
        super(message);
    }
}
