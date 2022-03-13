package com.expediagroup.pact.exception;

public class PactGenerationException extends RuntimeException {

    private static final long serialVersionUID = -5269968231651993381L;

    public PactGenerationException(String message) {
        super(message);
    }

    public PactGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
