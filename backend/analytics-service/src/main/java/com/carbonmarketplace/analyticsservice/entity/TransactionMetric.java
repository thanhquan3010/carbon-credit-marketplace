package com.carbonmarketplace.analyticsservice.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_metrics", indexes = {
    @Index(name = "idx_transaction_metrics_timestamp", columnList = "timestamp"),
    @Index(name = "idx_transaction_metrics_user_id", columnList = "user_id"),
    @Index(name = "idx_transaction_metrics_transaction_type", columnList = "transaction_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "credit_amount")
    private Double creditAmount;
    
    @Column(name = "transaction_type", length = 50)
    private String transactionType;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "co2_offset")
    private Double co2Offset;
    
    @Column(name = "revenue_generated", precision = 19, scale = 4)
    private Double revenueGenerated;
    
    @CreationTimestamp
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    // Aggregation fields for time-series analysis
    @Column(name = "hour_bucket")
    private Integer hourBucket;
    
    @Column(name = "day_bucket")
    private Integer dayBucket;
    
    @Column(name = "week_bucket")
    private Integer weekBucket;
    
    @Column(name = "month_bucket")
    private Integer monthBucket;
    
    @Column(name = "year_bucket")
    private Integer yearBucket;
}
