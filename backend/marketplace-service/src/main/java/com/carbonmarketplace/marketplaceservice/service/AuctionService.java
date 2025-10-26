package com.carbonmarketplace.marketplaceservice.service;

import com.carbonmarketplace.marketplaceservice.dto.BidRequest;
import com.carbonmarketplace.marketplaceservice.dto.BidResponse;
import com.carbonmarketplace.marketplaceservice.dto.BidUpdate;
import com.carbonmarketplace.marketplaceservice.entity.Bid;
import com.carbonmarketplace.marketplaceservice.entity.Listing;
import com.carbonmarketplace.marketplaceservice.exception.InvalidBidException;
import com.carbonmarketplace.marketplaceservice.exception.ResourceNotFoundException;
import com.carbonmarketplace.marketplaceservice.repository.jpa.BidRepository;
import com.carbonmarketplace.marketplaceservice.repository.jpa.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing auctions and bidding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuctionService {
    
    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${marketplace.listing.auction.min-increment-vnd}")
    private BigDecimal minBidIncrement;
    
    @Value("${marketplace.listing.auction.auto-extend-minutes}")
    private int autoExtendMinutes;
    
    @Value("${marketplace.listing.auction.auto-extend-window-minutes}")
    private int autoExtendWindowMinutes;
    
    /**
     * Place a bid on an auction listing.
     */
    public BidResponse placeBid(UUID bidderId, BidRequest request) {
        log.info("Processing bid from user {} for listing {}", bidderId, request.getListingId());
        
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Validate auction status
        if (!listing.isAuctionActive()) {
            throw new InvalidBidException("Auction is not active");
        }
        
        // Validate bid amount
        validateBidAmount(listing, request.getBidAmountPerTon());
        
        // Create and save bid
        Bid bid = Bid.builder()
                .listing(listing)
                .bidderId(bidderId)
                .bidderName(request.getBidderName())
                .bidAmountPerTon(request.getBidAmountPerTon())
                .maxAutoBidAmount(request.getMaxAutoBidAmount())
                .status(Bid.BidStatus.ACTIVE)
                .isAutoBid(false)
                .build();
        
        bid = bidRepository.save(bid);
        
        // Update previous highest bidder status
        if (listing.getHighestBidderId() != null && !listing.getHighestBidderId().equals(bidderId)) {
            updatePreviousBidderStatus(listing.getId(), listing.getHighestBidderId());
        }
        
        // Update listing with new highest bid
        listing.setCurrentBidPriceVnd(request.getBidAmountPerTon());
        listing.setHighestBidderId(bidderId);
        listing.setBidCount(listing.getBidCount() + 1);
        
        // Check for auto-extension
        if (shouldExtendAuction(listing)) {
            extendAuction(listing);
        }
        
        listingRepository.save(listing);
        
        // Broadcast bid update to all watchers
        broadcastBidUpdate(listing, bid);
        
        // Process auto-bids from other users
        processAutoBids(listing, bidderId);
        
        log.info("Bid placed successfully: {}", bid.getId());
        return BidResponse.fromEntity(bid);
    }
    
    /**
     * Validate bid amount meets requirements.
     */
    private void validateBidAmount(Listing listing, BigDecimal bidAmount) {
        BigDecimal currentPrice = listing.getCurrentBidPriceVnd();
        BigDecimal minBid = currentPrice != null 
            ? currentPrice.add(minBidIncrement)
            : listing.getStartingPricePerTonVnd();
        
        if (bidAmount.compareTo(minBid) < 0) {
            throw new InvalidBidException(
                String.format("Bid must be at least %s VND (current: %s + increment: %s)",
                    minBid, currentPrice, minBidIncrement)
            );
        }
    }
    
    /**
     * Check if auction should be extended.
     */
    private boolean shouldExtendAuction(Listing listing) {
        if (listing.getAuctionExtendedCount() != null && listing.getAuctionExtendedCount() >= 3) {
            return false; // Maximum 3 extensions
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime auctionEnd = listing.getAuctionEndTime();
        long minutesUntilEnd = ChronoUnit.MINUTES.between(now, auctionEnd);
        
        return minutesUntilEnd <= autoExtendWindowMinutes;
    }
    
    /**
     * Extend auction end time.
     */
    private void extendAuction(Listing listing) {
        LocalDateTime newEndTime = listing.getAuctionEndTime().plusMinutes(autoExtendMinutes);
        listing.setAuctionEndTime(newEndTime);
        listing.setAuctionExtendedCount(listing.getAuctionExtendedCount() + 1);
        
        log.info("Auction {} extended by {} minutes. New end time: {}", 
            listing.getId(), autoExtendMinutes, newEndTime);
        
        // Notify all watchers about extension
        BidUpdate extensionUpdate = BidUpdate.builder()
                .listingId(listing.getId())
                .updateType("AUCTION_EXTENDED")
                .message(String.format("Auction extended by %d minutes", autoExtendMinutes))
                .timeRemainingSeconds(ChronoUnit.SECONDS.between(LocalDateTime.now(), newEndTime))
                .timestamp(LocalDateTime.now())
                .build();
        
        messagingTemplate.convertAndSend("/topic/auction/" + listing.getId(), extensionUpdate);
    }
    
    /**
     * Broadcast bid update to WebSocket subscribers.
     */
    private void broadcastBidUpdate(Listing listing, Bid bid) {
        BidUpdate update = BidUpdate.builder()
                .listingId(listing.getId())
                .bidderId(bid.getBidderId())
                .bidderName(bid.getBidderName())
                .bidAmount(bid.getBidAmountPerTon())
                .totalAmount(bid.getTotalBidAmount())
                .bidCount(listing.getBidCount())
                .timestamp(LocalDateTime.now())
                .updateType("NEW_BID")
                .timeRemainingSeconds(ChronoUnit.SECONDS.between(LocalDateTime.now(), listing.getAuctionEndTime()))
                .build();
        
        messagingTemplate.convertAndSend("/topic/auction/" + listing.getId(), update);
        
        // Send personal notification to previous highest bidder
        if (listing.getHighestBidderId() != null && !listing.getHighestBidderId().equals(bid.getBidderId())) {
            BidUpdate outbidNotification = BidUpdate.builder()
                    .listingId(listing.getId())
                    .updateType("OUTBID")
                    .message("You have been outbid")
                    .isWinning(false)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                listing.getHighestBidderId().toString(),
                "/queue/notifications",
                outbidNotification
            );
        }
    }
    
    /**
     * Update status of previous highest bidder.
     */
    private void updatePreviousBidderStatus(UUID listingId, UUID previousBidderId) {
        bidRepository.findByBidderAndListing(previousBidderId, listingId)
                .stream()
                .filter(b -> b.getStatus() == Bid.BidStatus.ACTIVE || b.getStatus() == Bid.BidStatus.WINNING)
                .forEach(b -> {
                    b.setStatus(Bid.BidStatus.OUTBID);
                    bidRepository.save(b);
                });
    }
    
    /**
     * Process auto-bids from other users.
     */
    private void processAutoBids(Listing listing, UUID currentBidderId) {
        List<Bid> activeBids = bidRepository.findActiveBidsByListing(listing.getId());
        
        for (Bid bid : activeBids) {
            if (bid.getBidderId().equals(currentBidderId)) {
                continue;
            }
            
            if (bid.getMaxAutoBidAmount() != null && 
                bid.getMaxAutoBidAmount().compareTo(listing.getCurrentBidPriceVnd()) > 0) {
                
                BigDecimal autoBidAmount = listing.getCurrentBidPriceVnd().add(minBidIncrement);
                if (autoBidAmount.compareTo(bid.getMaxAutoBidAmount()) <= 0) {
                    // Place auto-bid
                    BidRequest autoBidRequest = BidRequest.builder()
                            .listingId(listing.getId())
                            .bidAmountPerTon(autoBidAmount)
                            .bidderName(bid.getBidderName())
                            .build();
                    
                    try {
                        placeBid(bid.getBidderId(), autoBidRequest);
                        log.info("Auto-bid placed for user {}", bid.getBidderId());
                    } catch (Exception e) {
                        log.error("Failed to place auto-bid for user {}", bid.getBidderId(), e);
                    }
                    
                    break; // Only process one auto-bid at a time
                }
            }
        }
    }
    
    /**
     * Get bid history for a listing.
     */
    public List<BidResponse> getBidHistory(UUID listingId) {
        return bidRepository.findByListingIdOrderByBidAmountPerTonDesc(listingId)
                .stream()
                .map(BidResponse::fromEntity)
                .toList();
    }
    
    /**
     * Get user's bid history.
     */
    public List<BidResponse> getUserBids(UUID userId) {
        return bidRepository.findByBidderIdOrderByCreatedAtDesc(userId, null).stream()
                .map(BidResponse::fromEntity)
                .toList();
    }
    
    /**
     * Scheduled task to close expired auctions.
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Listing> expiredAuctions = listingRepository.findExpiredAuctions(now);
        
        for (Listing listing : expiredAuctions) {
            closeAuction(listing);
        }
    }
    
    /**
     * Close an auction and determine the winner.
     */
    private void closeAuction(Listing listing) {
        log.info("Closing auction {}", listing.getId());
        
        // Check if reserve price was met
        if (!listing.hasReservePriceMet()) {
            listing.setStatus(Listing.ListingStatus.EXPIRED);
            log.info("Auction {} closed without meeting reserve price", listing.getId());
        } else if (listing.getHighestBidderId() != null) {
            listing.setStatus(Listing.ListingStatus.PENDING_PAYMENT);
            
            // Mark winning bid
            bidRepository.findByBidderAndListing(listing.getHighestBidderId(), listing.getId())
                    .stream()
                    .filter(b -> b.getStatus() == Bid.BidStatus.ACTIVE)
                    .forEach(b -> {
                        b.setStatus(Bid.BidStatus.WON);
                        bidRepository.save(b);
                    });
            
            // Notify winner
            BidUpdate winnerNotification = BidUpdate.builder()
                    .listingId(listing.getId())
                    .updateType("AUCTION_WON")
                    .message("Congratulations! You won the auction")
                    .isWinning(true)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                listing.getHighestBidderId().toString(),
                "/queue/notifications",
                winnerNotification
            );
            
            log.info("Auction {} won by user {}", listing.getId(), listing.getHighestBidderId());
        } else {
            listing.setStatus(Listing.ListingStatus.EXPIRED);
            log.info("Auction {} expired with no bids", listing.getId());
        }
        
        listingRepository.save(listing);
    }
}
