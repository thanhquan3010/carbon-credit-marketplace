package com.carbonmarketplace.marketplaceservice.service;

import com.carbonmarketplace.marketplaceservice.dto.*;
import com.carbonmarketplace.marketplaceservice.entity.Listing;
import com.carbonmarketplace.marketplaceservice.entity.ListingDocument;
import com.carbonmarketplace.marketplaceservice.exception.InvalidRequestException;
import com.carbonmarketplace.marketplaceservice.exception.ResourceNotFoundException;
import com.carbonmarketplace.marketplaceservice.repository.elasticsearch.ListingSearchRepository;
import com.carbonmarketplace.marketplaceservice.repository.jpa.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing marketplace listings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListingService {
    
    private final ListingRepository listingRepository;
    private final ListingSearchRepository listingSearchRepository;
    
    /**
     * Create a new listing.
     */
    public ListingResponse createListing(UUID sellerId, ListingCreateRequest request) {
        log.info("Creating listing for seller {}: {} tons at {} VND/ton",
                sellerId, request.getCreditAmountTons(), 
                request.getListingType() == Listing.ListingType.FIXED 
                    ? request.getPricePerTonVnd() 
                    : request.getStartingPricePerTonVnd());
        
        // Validate request
        request.validate();
        
        // Check if credit is already listed
        if (listingRepository.existsBySellerIdAndCreditIdAndStatusIn(
                sellerId, request.getCreditId(), 
                Arrays.asList(Listing.ListingStatus.ACTIVE, Listing.ListingStatus.PENDING_PAYMENT))) {
            throw new InvalidRequestException("Credit is already listed");
        }
        
        // Create listing entity
        Listing listing = Listing.builder()
                .sellerId(sellerId)
                .creditId(request.getCreditId())
                .listingType(request.getListingType())
                .creditAmountTons(request.getCreditAmountTons())
                .availableAmountTons(request.getCreditAmountTons())
                .region(request.getRegion())
                .vintageYear(request.getVintageYear())
                .description(request.getDescription())
                .status(Listing.ListingStatus.ACTIVE)
                .viewsCount(0)
                .watchersCount(0)
                .bidCount(0)
                .build();
        
        // Set type-specific fields
        if (request.getListingType() == Listing.ListingType.FIXED) {
            listing.setPricePerTonVnd(request.getPricePerTonVnd());
            listing.setExpiresAt(LocalDateTime.now().plusDays(90));
        } else {
            listing.setStartingPricePerTonVnd(request.getStartingPricePerTonVnd());
            listing.setCurrentBidPriceVnd(null); // No bids yet
            listing.setReservePricePerTonVnd(request.getReservePricePerTonVnd());
            listing.setAuctionEndTime(LocalDateTime.now().plusDays(request.getAuctionDurationDays()));
            listing.setAuctionExtendedCount(0);
        }
        
        // TODO: Get verification status and CVA organization from carbon-credit-service
        listing.setVerificationStatus("VERIFIED");
        listing.setCvaOrganization("TÜV SÜD");
        
        // Save listing
        listing = listingRepository.save(listing);
        
        // Index in Elasticsearch
        indexListing(listing);
        
        log.info("Listing created successfully: {}", listing.getId());
        
        // TODO: Get seller rating and transaction count from user-service
        return ListingResponse.fromEntity(listing, 4.5, 10);
    }
    
    /**
     * Update an existing listing.
     */
    public ListingResponse updateListing(UUID sellerId, UUID listingId, ListingUpdateRequest request) {
        log.info("Updating listing {} for seller {}", listingId, sellerId);
        
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Verify ownership
        if (!listing.getSellerId().equals(sellerId)) {
            throw new InvalidRequestException("You can only update your own listings");
        }
        
        // Check if listing can be updated
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE &&
            listing.getStatus() != Listing.ListingStatus.DRAFT) {
            throw new InvalidRequestException("Cannot update listing in status: " + listing.getStatus());
        }
        
        // Update allowed fields
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        
        if (listing.getListingType() == Listing.ListingType.FIXED && request.getPricePerTonVnd() != null) {
            // Can update price if no purchases yet
            if (listing.getCreditAmountTons().equals(listing.getAvailableAmountTons())) {
                listing.setPricePerTonVnd(request.getPricePerTonVnd());
            } else {
                throw new InvalidRequestException("Cannot update price after partial sales");
            }
        }
        
        listing = listingRepository.save(listing);
        
        // Update Elasticsearch index
        indexListing(listing);
        
        return ListingResponse.fromEntity(listing, 4.5, 10);
    }
    
    /**
     * Cancel a listing.
     */
    public void cancelListing(UUID sellerId, UUID listingId) {
        log.info("Cancelling listing {} for seller {}", listingId, sellerId);
        
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Verify ownership
        if (!listing.getSellerId().equals(sellerId)) {
            throw new InvalidRequestException("You can only cancel your own listings");
        }
        
        // Check if listing can be cancelled
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot cancel listing in status: " + listing.getStatus());
        }
        
        // For auctions with bids, special handling may be needed
        if (listing.getListingType() == Listing.ListingType.AUCTION && listing.getBidCount() > 0) {
            // TODO: Notify bidders about cancellation
        }
        
        listing.setStatus(Listing.ListingStatus.CANCELLED);
        listingRepository.save(listing);
        
        // Remove from Elasticsearch
        listingSearchRepository.deleteById(listing.getId().toString());
        
        log.info("Listing {} cancelled successfully", listingId);
    }
    
    /**
     * Get listing details.
     */
    @Transactional(readOnly = true)
    public ListingResponse getListingDetails(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Increment view count
        listingRepository.incrementViewCount(listingId);
        
        // TODO: Get seller rating and transaction count from user-service
        return ListingResponse.fromEntity(listing, 4.5, 10);
    }
    
    /**
     * Search listings.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchListings(ListingSearchRequest request, Pageable pageable) {
        request.setDefaults();
        
        Page<ListingDocument> searchResults;
        
        if (request.getSearchText() != null && !request.getSearchText().isEmpty()) {
            // Text search using Elasticsearch
            searchResults = listingSearchRepository.searchByText(request.getSearchText(), pageable);
        } else if (request.getLatitude() != null && request.getLongitude() != null) {
            // Geolocation search
            searchResults = listingSearchRepository.findNearbyListings(
                "ACTIVE",
                request.getRadiusKm() != null ? request.getRadiusKm() : 50,
                request.getLatitude(),
                request.getLongitude(),
                pageable
            );
        } else {
            // Filter-based search
            String status = request.getActiveOnly() ? "ACTIVE" : null;
            
            if (request.getMinPricePerTon() != null && request.getMaxPricePerTon() != null) {
                searchResults = listingSearchRepository.findByPriceRange(
                    status, request.getMinPricePerTon(), request.getMaxPricePerTon(), pageable
                );
            } else if (request.getRegions() != null && !request.getRegions().isEmpty()) {
                searchResults = listingSearchRepository.findByRegionInAndStatus(
                    request.getRegions(), status, pageable
                );
            } else {
                searchResults = listingSearchRepository.findByStatus(status, pageable);
            }
        }
        
        // Convert to response DTOs
        List<ListingResponse> responses = searchResults.getContent().stream()
                .map(doc -> {
                    // Get full listing from database
                    return listingRepository.findById(UUID.fromString(doc.getListingId()))
                            .map(listing -> ListingResponse.fromEntity(
                                listing,
                                doc.getSellerRating(),
                                doc.getSellerTransactions()
                            ))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, searchResults.getTotalElements());
    }
    
    /**
     * Get user's listings.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> getUserListings(UUID userId, Pageable pageable) {
        Page<Listing> listings = listingRepository.findBySellerIdAndStatusIn(
            userId,
            Arrays.asList(Listing.ListingStatus.ACTIVE, Listing.ListingStatus.COMPLETED, 
                         Listing.ListingStatus.PENDING_PAYMENT),
            pageable
        );
        
        return listings.map(listing -> ListingResponse.fromEntity(listing, 4.5, 10));
    }
    
    /**
     * Get trending listings.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getTrendingListings() {
        List<ListingDocument> trending = listingSearchRepository
                .findTop10ByStatusOrderByViewsCountDesc("ACTIVE");
        
        return trending.stream()
                .map(doc -> {
                    return listingRepository.findById(UUID.fromString(doc.getListingId()))
                            .map(listing -> ListingResponse.fromEntity(
                                listing,
                                doc.getSellerRating(),
                                doc.getSellerTransactions()
                            ))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Index listing in Elasticsearch.
     */
    private void indexListing(Listing listing) {
        try {
            // TODO: Get seller rating and transaction count from user-service
            ListingDocument document = ListingDocument.fromListing(listing, 4.5, 10);
            listingSearchRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to index listing {} in Elasticsearch", listing.getId(), e);
            // Don't fail the transaction, search index can be rebuilt
        }
    }
    
    /**
     * Scheduled task to expire old listings.
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void expireOldListings() {
        LocalDateTime now = LocalDateTime.now();
        List<Listing> expiredListings = listingRepository.findExpiredListings(now);
        
        for (Listing listing : expiredListings) {
            listing.setStatus(Listing.ListingStatus.EXPIRED);
            listingRepository.save(listing);
            
            // Update Elasticsearch
            try {
                ListingDocument doc = listingSearchRepository.findById(listing.getId().toString())
                        .orElse(null);
                if (doc != null) {
                    doc.setStatus("EXPIRED");
                    listingSearchRepository.save(doc);
                }
            } catch (Exception e) {
                log.error("Failed to update expired status in Elasticsearch for listing {}", 
                         listing.getId(), e);
            }
        }
        
        if (!expiredListings.isEmpty()) {
            log.info("Expired {} listings", expiredListings.size());
        }
    }
}
