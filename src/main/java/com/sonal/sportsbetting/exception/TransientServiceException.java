package com.sonal.sportsbetting.exception;

/**
 * Used for downstream / infrastructure failures that are safe to retry (timeouts, brief outages).
 */
public class TransientServiceException extends RuntimeException {

    public TransientServiceException(String message) {
        super(message);
    }

    public TransientServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
