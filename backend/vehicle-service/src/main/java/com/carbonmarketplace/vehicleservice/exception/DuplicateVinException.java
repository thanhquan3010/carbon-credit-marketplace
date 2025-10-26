package com.carbonmarketplace.vehicleservice.exception;

/**
 * Exception thrown when attempting to register a vehicle with a duplicate VIN
 */
public class DuplicateVinException extends RuntimeException {
    
    public DuplicateVinException(String message) {
        super(message);
    }
    
    public DuplicateVinException(String message, Throwable cause) {
        super(message, cause);
    }
}

