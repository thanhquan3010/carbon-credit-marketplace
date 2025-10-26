package com.carbonmarketplace.marketplaceservice.dto;

import com.carbonmarketplace.marketplaceservice.entity.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for listing response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    
    private UUID listingId;
    private UUID sellerId;
    private String sellerName;
    private Double sellerRating;
    private Integer sellerTransactions;
    private UUID creditId;
    private BigDecimal creditAmountTons;
    private BigDecimal availableAmountTons;
    private BigDecimal pricePerTonVnd;
    private BigDecimal totalPriceVnd;
    private Listing.ListingType listingType;
    private String region;
    private Integer vintageYear;
    private String verificationStatus;
    private String cvaOrganization;
    private Listing.ListingStatus status;
    private LocalDateTime createdAt;
    private Integer viewsCount;
    private Integer watchersCount;
    
    // Auction-specific fields
    private BigDecimal currentBidPriceVnd;
    private BigDecimal reservePriceVnd;
    private LocalDateTime auctionEndTime;
    private Integer bidCount;
    private UUID highestBidderId;
    private String highestBidderName;
    private Boolean reservePriceMet;
    private Long timeRemainingSeconds;
    
    // Calculated fields
    private BigDecimal platformFeeVnd;
    private BigDecimal buyerTotalVnd;
    
    private String description;
    private LocalDateTime expiresAt;
    
    public static ListingResponse fromEntity(Listing listing, Double sellerRating, Integer sellerTransactions) {
        ListingResponse response = ListingResponse.builder()
                .listingId(listing.getId())
                .sellerId(listing.getSellerId())
                .sellerName(listing.getSellerName())
                .sellerRating(sellerRating)
                .sellerTransactions(sellerTransactions)
                .creditId(listing.getCreditId())
                .creditAmountTons(listing.getCreditAmountTons())
                .availableAmountTons(listing.getAvailableAmountTons())
                .pricePerTonVnd(listing.getPricePerTonVnd())
                .listingType(listing.getListingType())
                .region(listing.getRegion())
                .vintageYear(listing.getVintageYear())
                .verificationStatus(listing.getVerificationStatus())
                .cvaOrganization(listing.getCvaOrganization())
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .viewsCount(listing.getViewsCount())
                .watchersCount(listing.getWatchersCount())
                .description(listing.getDescription())
                .expiresAt(listing.getExpiresAt())
                .build();
        
        // Calculate total price
        if (listing.getListingType() == Listing.ListingType.FIXED) {
            response.setTotalPriceVnd(listing.getPricePerTonVnd().multiply(listing.getCreditAmountTons()));
        } else if (listing.getListingType() == Listing.ListingType.AUCTION) {
            response.setCurrentBidPriceVnd(listing.getCurrentBidPriceVnd());
            response.setReservePriceVnd(listing.getReservePricePerTonVnd());
            response.setAuctionEndTime(listing.getAuctionEndTime());
            response.setBidCount(listing.getBidCount());
            response.setHighestBidderId(listing.getHighestBidderId());
            response.setReservePriceMet(listing.hasReservePriceMet());
            
            if (listing.getCurrentBidPriceVnd() != null) {
                response.setTotalPriceVnd(listing.getCurrentBidPriceVnd().multiply(listing.getCreditAmountTons()));
            }
            
            if (listing.getAuctionEndTime() != null) {
                long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), listing.getAuctionEndTime()).getSeconds();
                response.setTimeRemainingSeconds(secondsRemaining > 0 ? secondsRemaining : 0L);
            }
        }
        
        // Calculate fees
        if (response.getTotalPriceVnd() != null) {
            BigDecimal platformFee = response.getTotalPriceVnd().multiply(new BigDecimal("0.05"));
            response.setPlatformFeeVnd(platformFee);
            response.setBuyerTotalVnd(response.getTotalPriceVnd().add(platformFee));
        }
        
        return response;
    }
}
