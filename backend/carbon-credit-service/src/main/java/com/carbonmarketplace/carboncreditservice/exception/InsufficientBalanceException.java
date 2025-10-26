package com.carbonmarketplace.carboncreditservice.exception;

/**
 * Exception thrown when there's insufficient balance for a transaction
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
