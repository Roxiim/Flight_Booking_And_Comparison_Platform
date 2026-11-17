package com.flightplatform.backend.exception;

public class DuplicateResourceException extends BusinessRuleException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
