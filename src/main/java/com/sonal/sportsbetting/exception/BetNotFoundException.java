package com.sonal.sportsbetting.exception;

public class BetNotFoundException extends RuntimeException {
    public BetNotFoundException(String message) {
        super(message);
    }
}
