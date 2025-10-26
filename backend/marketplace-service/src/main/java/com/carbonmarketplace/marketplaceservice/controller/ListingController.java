package com.carbonmarketplace.marketplaceservice.controller;

import com.carbonmarketplace.marketplaceservice.dto.*;
import com.carbonmarketplace.marketplaceservice.service.ListingService;
import com.carbonmarketplace.marketplaceservice.service.PriceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for marketplace listing operations.
 */
@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Marketplace", description = "Marketplace listing operations")
public class ListingController {
    
    private final ListingService listingService;
    private final PriceRecommendationService priceRecommendationService;
    
    @Operation(summary = "Search marketplace listings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listings retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @GetMapping("/listings")
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @Parameter(description = "Search text") @RequestParam(required = false) String searchText,
            @Parameter(description = "Region filter") @RequestParam(required = false) String region,
            @Parameter(description = "Minimum price per ton") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price per ton") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum amount in tons") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Listing type") @RequestParam(required = false) String listingType,
            @Parameter(description = "Verification status") @RequestParam(required = false) String verificationStatus,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort order") @RequestParam(defaultValue = "DESC") String order,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int perPage) {
        
        ListingSearchRequest searchRequest = ListingSearchRequest.builder()
                .searchText(searchText)
                .minPricePerTon(minPrice)
                .maxPricePerTon(maxPrice)
                .minAmount(minAmount)
                .verificationStatus(verificationStatus)
                .sortBy(sortBy)
                .sortDirection(order)
                .build();
        
        if (region != null) {
            searchRequest.setRegions(java.util.List.of(region));
        }
        
        if (listingType != null) {
            try {
                searchRequest.setListingType(
                    com.carbonmarketplace.marketplaceservice.entity.Listing.ListingType.valueOf(listingType)
                );
            } catch (IllegalArgumentException e) {
                // Invalid listing type, ignore
            }
        }
        
        Sort sort = Sort.by(
            order.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, perPage, sort);
        
        Page<ListingResponse> listings = listingService.searchListings(searchRequest, pageable);
        
        return ResponseEntity.ok(listings);
    }
    
    @Operation(summary = "Get listing details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listing details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @GetMapping("/listings/{listingId}")
    public ResponseEntity<ListingResponse> getListingDetails(
            @Parameter(description = "Listing ID", required = true) @PathVariable UUID listingId) {
        
        ListingResponse listing = listingService.getListingDetails(listingId);
        return ResponseEntity.ok(listing);
    }
    
    @Operation(summary = "Create a new listing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Listing created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid listing data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/listings")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody ListingCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        ListingResponse listing = listingService.createListing(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(listing);
    }
    
    @Operation(summary = "Update a listing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listing updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @PutMapping("/listings/{listingId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ListingResponse> updateListing(
            @Parameter(description = "Listing ID", required = true) @PathVariable UUID listingId,
            @Valid @RequestBody ListingUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        ListingResponse listing = listingService.updateListing(userId, listingId, request);
        
        return ResponseEntity.ok(listing);
    }
    
    @Operation(summary = "Cancel a listing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Listing cancelled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @DeleteMapping("/listings/{listingId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> cancelListing(
            @Parameter(description = "Listing ID", required = true) @PathVariable UUID listingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        listingService.cancelListing(userId, listingId);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Get price recommendation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Price recommendation retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/price-recommendation")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<PriceRecommendationResponse> getPriceRecommendation(
            @Parameter(description = "Region", required = true) @RequestParam String region,
            @Parameter(description = "Vintage year") @RequestParam(required = false) Integer vintageYear,
            @Parameter(description = "Amount in tons", required = true) @RequestParam BigDecimal amountTons) {
        
        PriceRecommendationResponse recommendation = 
            priceRecommendationService.getPriceRecommendation(region, vintageYear, amountTons);
        
        return ResponseEntity.ok(recommendation);
    }
    
    @Operation(summary = "Get user's listings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User listings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<ListingResponse>> getMyListings(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int perPage,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        Pageable pageable = PageRequest.of(page, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<ListingResponse> listings = listingService.getUserListings(userId, pageable);
        
        return ResponseEntity.ok(listings);
    }
    
    @Operation(summary = "Get trending listings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trending listings retrieved successfully")
    })
    @GetMapping("/trending")
    public ResponseEntity<java.util.List<ListingResponse>> getTrendingListings() {
        java.util.List<ListingResponse> listings = listingService.getTrendingListings();
        return ResponseEntity.ok(listings);
    }
    
    /**
     * Extract user ID from authentication principal.
     * This would need to be adapted based on your actual authentication implementation.
     */
    private UUID getUserIdFromPrincipal(UserDetails userDetails) {
        // This is a placeholder - actual implementation would depend on your UserDetails implementation
        // Typically you'd have a custom UserDetails class with a getUserId() method
        return UUID.fromString(userDetails.getUsername());
    }
}
