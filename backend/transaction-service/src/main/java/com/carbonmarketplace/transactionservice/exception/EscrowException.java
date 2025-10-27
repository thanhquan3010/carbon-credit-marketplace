package com.carbonmarketplace.transactionservice.exception;

public class EscrowException extends RuntimeException {
    public EscrowException(String message) {
        super(message);
    }

    public EscrowException(String message, Throwable cause) {
        super(message, cause);
    }
}
