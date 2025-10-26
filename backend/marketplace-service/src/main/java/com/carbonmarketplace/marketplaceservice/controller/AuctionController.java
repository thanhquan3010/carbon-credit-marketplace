package com.carbonmarketplace.marketplaceservice.controller;

import com.carbonmarketplace.marketplaceservice.dto.BidRequest;
import com.carbonmarketplace.marketplaceservice.dto.BidResponse;
import com.carbonmarketplace.marketplaceservice.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for auction operations.
 */
@RestController
@RequestMapping("/marketplace/auctions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auctions", description = "Auction and bidding operations")
public class AuctionController {
    
    private final AuctionService auctionService;
    
    @Operation(summary = "Place a bid on an auction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bid placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid bid"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PostMapping("/bid")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BidResponse> placeBid(
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        BidResponse bid = auctionService.placeBid(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(bid);
    }
    
    @Operation(summary = "Get bid history for an auction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid history retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @GetMapping("/{listingId}/bids")
    public ResponseEntity<List<BidResponse>> getBidHistory(
            @Parameter(description = "Listing ID", required = true) @PathVariable UUID listingId) {
        
        List<BidResponse> bids = auctionService.getBidHistory(listingId);
        return ResponseEntity.ok(bids);
    }
    
    @Operation(summary = "Get user's bid history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User bid history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-bids")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<BidResponse>> getMyBids(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        List<BidResponse> bids = auctionService.getUserBids(userId);
        
        return ResponseEntity.ok(bids);
    }
    
    /**
     * Extract user ID from authentication principal.
     */
    private UUID getUserIdFromPrincipal(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
