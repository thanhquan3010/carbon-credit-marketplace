package com.carbonmarketplace.marketplaceservice.exception;

/**
 * Exception thrown when a bid is invalid.
 */
public class InvalidBidException extends RuntimeException {
    
    public InvalidBidException(String message) {
        super(message);
    }
    
    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
