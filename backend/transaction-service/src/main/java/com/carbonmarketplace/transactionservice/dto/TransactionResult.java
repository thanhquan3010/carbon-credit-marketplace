package com.carbonmarketplace.transactionservice.dto;

import com.carbonmarketplace.transactionservice.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {
    private boolean success;
    private String message;
    private Transaction transaction;
    private String errorCode;
    
    public static TransactionResult success(Transaction transaction) {
        return TransactionResult.builder()
                .success(true)
                .transaction(transaction)
                .message("Transaction completed successfully")
                .build();
    }
    
    public static TransactionResult failure(String message) {
        return TransactionResult.builder()
                .success(false)
                .message(message)
                .errorCode("TRANSACTION_FAILED")
                .build();
    }
}
