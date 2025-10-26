package com.carbonmarketplace.vehicleservice.exception;

/**
 * Exception thrown when trip data validation fails
 */
public class InvalidTripDataException extends RuntimeException {
    
    public InvalidTripDataException(String message) {
        super(message);
    }
    
    public InvalidTripDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
