package com.carbonmarketplace.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotNull(message = "Buyer ID is required")
    private UUID buyerId;
    
    @NotNull(message = "Seller ID is required")
    private UUID sellerId;
    
    @NotNull(message = "Credit amount is required")
    @Positive(message = "Credit amount must be positive")
    private BigDecimal creditAmount;
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;
    
    @NotNull(message = "Buyer name is required")
    private String buyerName;
    
    @Email(message = "Invalid email format")
    @NotNull(message = "Buyer email is required")
    private String buyerEmail;
    
    private String buyerPhone;
    private String buyerCompany;
    private String buyerTaxId;
    private String buyerAddress;
    
    private String idempotencyKey;
    private Boolean isExpressSettlement = false;
    private String notes;
}
