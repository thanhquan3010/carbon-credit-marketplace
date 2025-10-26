package com.carbonmarketplace.marketplaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an item in a user's shopping cart.
 */
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_item_user", columnList = "user_id"),
    @Index(name = "idx_cart_item_listing", columnList = "listing_id"),
    @Index(name = "idx_cart_item_reserved", columnList = "reservation_expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(name = "quantity_tons", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityTons;
    
    @Column(name = "price_per_ton_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerTonVnd;
    
    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;
    
    @Column(name = "is_reserved")
    private Boolean isReserved = false;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Version
    private Long version;
    
    public BigDecimal getTotalPrice() {
        return quantityTons.multiply(pricePerTonVnd);
    }
    
    public boolean isReservationValid() {
        return isReserved && 
               reservationExpiresAt != null && 
               LocalDateTime.now().isBefore(reservationExpiresAt);
    }
    
    public void reserve(int reservationMinutes) {
        this.isReserved = true;
        this.reservationExpiresAt = LocalDateTime.now().plusMinutes(reservationMinutes);
    }
    
    public void releaseReservation() {
        this.isReserved = false;
        this.reservationExpiresAt = null;
    }
}
