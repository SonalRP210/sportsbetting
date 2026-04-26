package com.sonal.sportsbetting.service;

public class BetNotFoundException extends RuntimeException {
    public BetNotFoundException(String message) {
        super(message);
    }
}
