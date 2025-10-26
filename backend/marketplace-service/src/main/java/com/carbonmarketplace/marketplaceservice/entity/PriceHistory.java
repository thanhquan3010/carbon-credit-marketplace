package com.carbonmarketplace.marketplaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking price history and analytics.
 * Used for price recommendation and market analysis.
 */
@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_price_history_date", columnList = "date"),
    @Index(name = "idx_price_history_region", columnList = "region"),
    @Index(name = "idx_price_history_vintage", columnList = "vintage_year")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PriceHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "region")
    private String region;
    
    @Column(name = "vintage_year")
    private Integer vintageYear;
    
    @Column(name = "avg_price_per_ton", precision = 15, scale = 2)
    private BigDecimal avgPricePerTon;
    
    @Column(name = "min_price_per_ton", precision = 15, scale = 2)
    private BigDecimal minPricePerTon;
    
    @Column(name = "max_price_per_ton", precision = 15, scale = 2)
    private BigDecimal maxPricePerTon;
    
    @Column(name = "median_price_per_ton", precision = 15, scale = 2)
    private BigDecimal medianPricePerTon;
    
    @Column(name = "std_deviation", precision = 15, scale = 2)
    private BigDecimal stdDeviation;
    
    @Column(name = "total_volume_tons", precision = 15, scale = 3)
    private BigDecimal totalVolumeTons;
    
    @Column(name = "transaction_count")
    private Integer transactionCount;
    
    @Column(name = "listing_count")
    private Integer listingCount;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
