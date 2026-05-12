package com.finai.exception;

/**
 * Eccezione di dominio FINAI con codice HTTP associato.
 * Viene gestita centralmente da {@link GlobalExceptionHandler}.
 */
public class FinaiException extends RuntimeException {

    private final int statusCode;

    public FinaiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
