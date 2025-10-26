package com.carbonmarketplace.carboncreditservice.exception;

/**
 * Exception thrown when a carbon wallet is not found
 */
public class WalletNotFoundException extends RuntimeException {
    
    public WalletNotFoundException(String message) {
        super(message);
    }
    
    public WalletNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
