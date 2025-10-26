package com.carbonmarketplace.marketplaceservice.repository.jpa;

import com.carbonmarketplace.marketplaceservice.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Listing entity.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    
    Page<Listing> findBySellerIdAndStatusIn(UUID sellerId, List<Listing.ListingStatus> statuses, Pageable pageable);
    
    Page<Listing> findByStatus(Listing.ListingStatus status, Pageable pageable);
    
    @Query("SELECT l FROM Listing l WHERE l.listingType = 'AUCTION' AND l.status = 'ACTIVE' AND l.auctionEndTime <= :endTime")
    List<Listing> findExpiredAuctions(@Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.expiresAt <= :now")
    List<Listing> findExpiredListings(@Param("now") LocalDateTime now);
    
    @Query("SELECT l FROM Listing l WHERE l.listingType = :type AND l.status = 'ACTIVE' " +
           "AND (:region IS NULL OR l.region = :region) " +
           "AND (:minPrice IS NULL OR l.pricePerTonVnd >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.pricePerTonVnd <= :maxPrice)")
    Page<Listing> searchListings(@Param("type") Listing.ListingType type,
                                  @Param("region") String region,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice,
                                  Pageable pageable);
    
    @Query("SELECT COUNT(l) FROM Listing l WHERE l.sellerId = :sellerId AND l.status = 'COMPLETED'")
    Integer countCompletedListingsBySeller(@Param("sellerId") UUID sellerId);
    
    @Query("SELECT AVG(l.pricePerTonVnd) FROM Listing l WHERE l.region = :region AND l.status = 'COMPLETED' " +
           "AND l.updatedAt >= :since")
    BigDecimal findAveragePriceByRegion(@Param("region") String region, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("UPDATE Listing l SET l.viewsCount = l.viewsCount + 1 WHERE l.id = :listingId")
    void incrementViewCount(@Param("listingId") UUID listingId);
    
    @Modifying
    @Query("UPDATE Listing l SET l.watchersCount = l.watchersCount + :delta WHERE l.id = :listingId")
    void updateWatchersCount(@Param("listingId") UUID listingId, @Param("delta") int delta);
    
    @Query("SELECT l FROM Listing l WHERE l.creditId = :creditId AND l.status IN ('ACTIVE', 'PENDING_PAYMENT')")
    Optional<Listing> findActiveListingByCreditId(@Param("creditId") UUID creditId);
    
    @Query("SELECT l FROM Listing l WHERE l.listingType = 'AUCTION' " +
           "AND l.status = 'ACTIVE' " +
           "AND l.auctionEndTime > :now " +
           "AND l.auctionEndTime <= :extendWindow")
    List<Listing> findAuctionsNearingEnd(@Param("now") LocalDateTime now, 
                                          @Param("extendWindow") LocalDateTime extendWindow);
    
    boolean existsBySellerIdAndCreditIdAndStatusIn(UUID sellerId, UUID creditId, List<Listing.ListingStatus> statuses);
}
