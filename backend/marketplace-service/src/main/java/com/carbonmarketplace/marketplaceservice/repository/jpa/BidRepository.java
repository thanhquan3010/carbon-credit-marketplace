package com.carbonmarketplace.marketplaceservice.repository.jpa;

import com.carbonmarketplace.marketplaceservice.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Bid entity.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    
    List<Bid> findByListingIdOrderByBidAmountPerTonDesc(UUID listingId);
    
    Page<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId, Pageable pageable);
    
    Optional<Bid> findTopByListingIdOrderByBidAmountPerTonDesc(UUID listingId);
    
    @Query("SELECT b FROM Bid b WHERE b.listing.id = :listingId AND b.status = 'ACTIVE' ORDER BY b.bidAmountPerTon DESC")
    List<Bid> findActiveBidsByListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.listing.id = :listingId")
    Integer countBidsByListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT b FROM Bid b WHERE b.bidderId = :bidderId AND b.listing.id = :listingId ORDER BY b.createdAt DESC")
    List<Bid> findByBidderAndListing(@Param("bidderId") UUID bidderId, @Param("listingId") UUID listingId);
    
    boolean existsByBidderIdAndListingId(UUID bidderId, UUID listingId);
    
    @Query("SELECT DISTINCT b.bidderId FROM Bid b WHERE b.listing.id = :listingId")
    List<UUID> findDistinctBiddersByListing(@Param("listingId") UUID listingId);
}
