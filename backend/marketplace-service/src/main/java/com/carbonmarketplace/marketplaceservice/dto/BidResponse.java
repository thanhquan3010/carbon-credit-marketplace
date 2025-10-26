package com.carbonmarketplace.marketplaceservice.dto;

import com.carbonmarketplace.marketplaceservice.entity.Bid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for bid response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    
    private UUID bidId;
    private UUID listingId;
    private UUID bidderId;
    private String bidderName;
    private BigDecimal bidAmountPerTon;
    private BigDecimal totalBidAmount;
    private Bid.BidStatus status;
    private Boolean isAutoBid;
    private LocalDateTime createdAt;
    
    public static BidResponse fromEntity(Bid bid) {
        return BidResponse.builder()
                .bidId(bid.getId())
                .listingId(bid.getListing().getId())
                .bidderId(bid.getBidderId())
                .bidderName(bid.getBidderName())
                .bidAmountPerTon(bid.getBidAmountPerTon())
                .totalBidAmount(bid.getTotalBidAmount())
                .status(bid.getStatus())
                .isAutoBid(bid.getIsAutoBid())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
