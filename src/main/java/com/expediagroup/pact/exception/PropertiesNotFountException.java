package com.expediagroup.pact.exception;

public class PropertiesNotFountException extends RuntimeException {

    private static final long serialVersionUID = -5269968231651993381L;

    public PropertiesNotFountException(String message) {
        super(message);
    }

    public PropertiesNotFountException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertiesNotFountException() {
    }
}
