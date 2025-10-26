package com.carbonmarketplace.marketplaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a bid on an auction listing.
 */
@Entity
@Table(name = "bids", indexes = {
    @Index(name = "idx_bid_listing", columnList = "listing_id"),
    @Index(name = "idx_bid_bidder", columnList = "bidder_id"),
    @Index(name = "idx_bid_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Bid {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;
    
    @Column(name = "bidder_name")
    private String bidderName;
    
    @Column(name = "bid_amount_per_ton", nullable = false, precision = 15, scale = 2)
    private BigDecimal bidAmountPerTon;
    
    @Column(name = "max_auto_bid_amount", precision = 15, scale = 2)
    private BigDecimal maxAutoBidAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BidStatus status;
    
    @Column(name = "is_auto_bid")
    private Boolean isAutoBid = false;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum BidStatus {
        ACTIVE,
        OUTBID,
        WINNING,
        WON,
        CANCELLED,
        EXPIRED
    }
    
    public BigDecimal getTotalBidAmount() {
        if (listing != null && bidAmountPerTon != null) {
            return bidAmountPerTon.multiply(listing.getCreditAmountTons());
        }
        return BigDecimal.ZERO;
    }
}
