package com.carbonmarketplace.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for real-time bid updates via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdate {
    
    private UUID listingId;
    private UUID bidderId;
    private String bidderName;
    private BigDecimal bidAmount;
    private BigDecimal totalAmount;
    private Integer bidCount;
    private LocalDateTime timestamp;
    private String updateType; // NEW_BID, OUTBID, AUCTION_EXTENDED, AUCTION_ENDING
    private Long timeRemainingSeconds;
    private Boolean isWinning;
    private String message;
}
