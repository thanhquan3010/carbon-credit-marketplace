package com.carbonmarketplace.transactionservice.exception;

/**
 * Custom exceptions for Transaction Service
 */
public class TransactionExceptions {
    
    public static class SagaExecutionException extends RuntimeException {
        public SagaExecutionException(String message) {
            super(message);
        }
        
        public SagaExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class EscrowException extends RuntimeException {
        public EscrowException(String message) {
            super(message);
        }
        
        public EscrowException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class SettlementException extends RuntimeException {
        public SettlementException(String message) {
            super(message);
        }
        
        public SettlementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class RefundException extends RuntimeException {
        public RefundException(String message) {
            super(message);
        }
        
        public RefundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class InvalidTransactionStateException extends RuntimeException {
        public InvalidTransactionStateException(String message) {
            super(message);
        }
    }
    
    public static class TransactionLimitExceededException extends RuntimeException {
        public TransactionLimitExceededException(String message) {
            super(message);
        }
    }
}
