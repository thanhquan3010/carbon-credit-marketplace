package com.carbonmarketplace.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for cart response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    
    private List<CartItemResponse> items;
    private BigDecimal totalAmountVnd;
    private BigDecimal platformFeeVnd;
    private BigDecimal grandTotalVnd;
    private Integer itemCount;
    private LocalDateTime reservationExpiresAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private UUID cartItemId;
        private UUID listingId;
        private String sellerName;
        private BigDecimal quantityTons;
        private BigDecimal pricePerTonVnd;
        private BigDecimal subtotalVnd;
        private String region;
        private String verificationStatus;
        private Boolean isReserved;
        private LocalDateTime reservationExpiresAt;
        private Boolean isAvailable;
    }
}
