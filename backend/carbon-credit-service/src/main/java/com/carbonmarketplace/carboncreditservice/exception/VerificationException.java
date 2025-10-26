package com.carbonmarketplace.carboncreditservice.exception;

/**
 * Exception thrown when verification fails or encounters errors
 */
public class VerificationException extends RuntimeException {
    
    public VerificationException(String message) {
        super(message);
    }
    
    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
