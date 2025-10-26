package com.carbonmarketplace.marketplaceservice.repository.jpa;

import com.carbonmarketplace.marketplaceservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for CartItem entity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    List<CartItem> findByUserIdOrderByCreatedAt(UUID userId);
    
    Optional<CartItem> findByUserIdAndListingId(UUID userId, UUID listingId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.id IN :itemIds")
    void deleteItemsByUserAndIds(@Param("userId") UUID userId, @Param("itemIds") List<UUID> itemIds);
    
    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.userId = :userId")
    Integer countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT c FROM CartItem c WHERE c.isReserved = true AND c.reservationExpiresAt <= :now")
    List<CartItem> findExpiredReservations(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(c.quantityTons) FROM CartItem c WHERE c.listing.id = :listingId AND c.isReserved = true AND c.reservationExpiresAt > :now")
    Double findReservedQuantityForListing(@Param("listingId") UUID listingId, @Param("now") LocalDateTime now);
    
    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);
}
