package com.carbonmarketplace.transactionservice.exception;

public class SettlementException extends RuntimeException {
    public SettlementException(String message) {
        super(message);
    }

    public SettlementException(String message, Throwable cause) {
        super(message, cause);
    }
}
