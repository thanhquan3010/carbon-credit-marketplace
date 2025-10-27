package com.carbonmarketplace.analyticsservice.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_kpis", indexes = {
        @Index(name = "idx_kpi_date", columnList = "calculation_date"),
        @Index(name = "idx_kpi_type", columnList = "kpi_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformKPI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_type", nullable = false, length = 100)
    private String kpiType;

    @Column(name = "kpi_name", nullable = false, length = 200)
    private String kpiName;

    @Column(name = "kpi_value", precision = 19, scale = 4)
    private BigDecimal kpiValue;

    @Column(name = "calculation_date", nullable = false)
    private LocalDateTime calculationDate;

    @Column(name = "period_type", length = 50) // DAILY, WEEKLY, MONTHLY, YEARLY
    private String periodType;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    // Comparison metrics
    @Column(name = "previous_value", precision = 19, scale = 4)
    private BigDecimal previousValue;

    @Column(name = "change_percentage", precision = 10, scale = 2)
    private BigDecimal changePercentage;

    @Column(name = "trend") // UP, DOWN, STABLE
    private String trend;

    // Additional metadata
    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "achievement_percentage", precision = 10, scale = 2)
    private BigDecimal achievementPercentage;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
