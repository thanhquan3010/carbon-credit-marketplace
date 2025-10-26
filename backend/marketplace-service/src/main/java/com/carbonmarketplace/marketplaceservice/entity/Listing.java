package com.carbonmarketplace.marketplaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a carbon credit listing in the marketplace.
 * Supports both fixed-price and auction listing types.
 */
@Entity
@Table(name = "listings", indexes = {
    @Index(name = "idx_listing_status", columnList = "status"),
    @Index(name = "idx_listing_seller", columnList = "seller_id"),
    @Index(name = "idx_listing_type", columnList = "listing_type"),
    @Index(name = "idx_listing_region", columnList = "region"),
    @Index(name = "idx_listing_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Listing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    
    @Column(name = "seller_name")
    private String sellerName;
    
    @Column(name = "credit_id", nullable = false)
    private UUID creditId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;
    
    @Column(name = "credit_amount_tons", nullable = false, precision = 10, scale = 3)
    private BigDecimal creditAmountTons;
    
    @Column(name = "available_amount_tons", nullable = false, precision = 10, scale = 3)
    private BigDecimal availableAmountTons;
    
    @Column(name = "price_per_ton_vnd", precision = 15, scale = 2)
    private BigDecimal pricePerTonVnd;
    
    // Auction-specific fields
    @Column(name = "starting_price_per_ton_vnd", precision = 15, scale = 2)
    private BigDecimal startingPricePerTonVnd;
    
    @Column(name = "current_bid_price_vnd", precision = 15, scale = 2)
    private BigDecimal currentBidPriceVnd;
    
    @Column(name = "reserve_price_per_ton_vnd", precision = 15, scale = 2)
    private BigDecimal reservePricePerTonVnd;
    
    @Column(name = "auction_end_time")
    private LocalDateTime auctionEndTime;
    
    @Column(name = "auction_extended_count")
    private Integer auctionExtendedCount = 0;
    
    @Column(name = "highest_bidder_id")
    private UUID highestBidderId;
    
    @Column(name = "bid_count")
    private Integer bidCount = 0;
    
    // Common fields
    @Column(name = "region")
    private String region;
    
    @Column(name = "vintage_year")
    private Integer vintageYear;
    
    @Column(name = "verification_status")
    private String verificationStatus;
    
    @Column(name = "cva_organization")
    private String cvaOrganization;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ListingStatus status;
    
    @Column(name = "views_count")
    private Integer viewsCount = 0;
    
    @Column(name = "watchers_count")
    private Integer watchersCount = 0;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();
    
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();
    
    @Version
    private Long version;
    
    public enum ListingType {
        FIXED,
        AUCTION
    }
    
    public enum ListingStatus {
        DRAFT,
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED,
        COMPLETED,
        PENDING_PAYMENT
    }
    
    public BigDecimal getTotalPrice() {
        if (listingType == ListingType.FIXED) {
            return pricePerTonVnd.multiply(creditAmountTons);
        } else if (listingType == ListingType.AUCTION && currentBidPriceVnd != null) {
            return currentBidPriceVnd.multiply(creditAmountTons);
        }
        return BigDecimal.ZERO;
    }
    
    public boolean isAuctionActive() {
        return listingType == ListingType.AUCTION &&
               status == ListingStatus.ACTIVE &&
               LocalDateTime.now().isBefore(auctionEndTime);
    }
    
    public boolean hasReservePriceMet() {
        if (listingType != ListingType.AUCTION || reservePricePerTonVnd == null) {
            return true;
        }
        return currentBidPriceVnd != null && 
               currentBidPriceVnd.compareTo(reservePricePerTonVnd) >= 0;
    }
}
