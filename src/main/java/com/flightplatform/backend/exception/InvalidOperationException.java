package com.flightplatform.backend.exception;

public class InvalidOperationException extends BusinessRuleException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
