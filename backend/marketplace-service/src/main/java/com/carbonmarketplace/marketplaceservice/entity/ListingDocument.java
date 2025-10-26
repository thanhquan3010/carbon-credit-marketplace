package com.carbonmarketplace.marketplaceservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Elasticsearch document for listing search and filtering.
 * Optimized for fast full-text search and complex filtering.
 */
@Document(indexName = "marketplace_listings")
@Setting(replicas = 1, shards = 2)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String listingId;
    
    @Field(type = FieldType.Keyword)
    private String sellerId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String sellerName;
    
    @Field(type = FieldType.Keyword)
    private String creditId;
    
    @Field(type = FieldType.Keyword)
    private String listingType;
    
    @Field(type = FieldType.Double)
    private BigDecimal creditAmountTons;
    
    @Field(type = FieldType.Double)
    private BigDecimal availableAmountTons;
    
    @Field(type = FieldType.Double)
    private BigDecimal pricePerTonVnd;
    
    @Field(type = FieldType.Double)
    private BigDecimal currentBidPriceVnd;
    
    @Field(type = FieldType.Keyword)
    private String region;
    
    @Field(type = FieldType.Integer)
    private Integer vintageYear;
    
    @Field(type = FieldType.Keyword)
    private String verificationStatus;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String cvaOrganization;
    
    @Field(type = FieldType.Text, analyzer = "standard", fielddata = true)
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Double)
    private Double sellerRating;
    
    @Field(type = FieldType.Integer)
    private Integer sellerTransactions;
    
    @Field(type = FieldType.Integer)
    private Integer viewsCount;
    
    @Field(type = FieldType.Integer)
    private Integer watchersCount;
    
    @Field(type = FieldType.Integer)
    private Integer bidCount;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime auctionEndTime;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime expiresAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String searchableText;
    
    @GeoPointField
    private GeoPoint location;
    
    public static ListingDocument fromListing(Listing listing, Double sellerRating, Integer sellerTransactions) {
        return ListingDocument.builder()
                .id(listing.getId().toString())
                .listingId(listing.getId().toString())
                .sellerId(listing.getSellerId().toString())
                .sellerName(listing.getSellerName())
                .creditId(listing.getCreditId().toString())
                .listingType(listing.getListingType().toString())
                .creditAmountTons(listing.getCreditAmountTons())
                .availableAmountTons(listing.getAvailableAmountTons())
                .pricePerTonVnd(listing.getPricePerTonVnd())
                .currentBidPriceVnd(listing.getCurrentBidPriceVnd())
                .region(listing.getRegion())
                .vintageYear(listing.getVintageYear())
                .verificationStatus(listing.getVerificationStatus())
                .cvaOrganization(listing.getCvaOrganization())
                .description(listing.getDescription())
                .status(listing.getStatus().toString())
                .sellerRating(sellerRating)
                .sellerTransactions(sellerTransactions)
                .viewsCount(listing.getViewsCount())
                .watchersCount(listing.getWatchersCount())
                .bidCount(listing.getBidCount())
                .auctionEndTime(listing.getAuctionEndTime())
                .expiresAt(listing.getExpiresAt())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .searchableText(buildSearchableText(listing))
                .build();
    }
    
    private static String buildSearchableText(Listing listing) {
        return String.join(" ",
                listing.getSellerName() != null ? listing.getSellerName() : "",
                listing.getRegion() != null ? listing.getRegion() : "",
                listing.getCvaOrganization() != null ? listing.getCvaOrganization() : "",
                listing.getDescription() != null ? listing.getDescription() : ""
        );
    }
}
