package com.carbonmarketplace.analyticsservice.service;

import com.carbonmarketplace.analyticsservice.entity.PlatformKPI;
import com.carbonmarketplace.analyticsservice.entity.TransactionMetric;
import com.carbonmarketplace.analyticsservice.entity.UserAnalytics;
import com.carbonmarketplace.analyticsservice.repository.PlatformKPIRepository;
import com.carbonmarketplace.analyticsservice.repository.TransactionMetricRepository;
import com.carbonmarketplace.analyticsservice.repository.UserAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsCalculationService {
    
    private final TransactionMetricRepository transactionMetricRepository;
    private final PlatformKPIRepository platformKPIRepository;
    private final UserAnalyticsRepository userAnalyticsRepository;
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void calculateDailyMetrics() {
        log.info("Calculating daily metrics...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        calculateMetricsForPeriod(startOfDay, endOfDay, "DAILY");
    }
    
    @Transactional
    public void calculateWeeklyMetrics() {
        log.info("Calculating weekly metrics...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);
        
        calculateMetricsForPeriod(startOfWeek, endOfWeek, "WEEKLY");
    }
    
    @Transactional
    public void calculateMonthlyMetrics() {
        log.info("Calculating monthly metrics...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
        
        calculateMetricsForPeriod(startOfMonth, endOfMonth, "MONTHLY");
    }
    
    private void calculateMetricsForPeriod(LocalDateTime start, LocalDateTime end, String periodType) {
        // Calculate platform KPIs
        calculatePlatformKPIs(start, end, periodType);
        
        // Calculate user analytics
        calculateUserAnalytics(start, end, periodType);
        
        // Calculate marketplace metrics
        calculateMarketplaceMetrics(start, end, periodType);
        
        // Calculate environmental impact metrics
        calculateEnvironmentalMetrics(start, end, periodType);
    }
    
    private void calculatePlatformKPIs(LocalDateTime start, LocalDateTime end, String periodType) {
        log.debug("Calculating platform KPIs for period: {} to {}", start, end);
        
        // Total GMV (Gross Merchandise Value)
        BigDecimal totalGMV = transactionMetricRepository.calculateTotalGMV(start, end);
        savePlatformKPI("GMV", "Total Gross Merchandise Value", totalGMV, periodType, start, end, "FINANCIAL", "USD");
        
        // Total Active Users
        Long activeUsers = transactionMetricRepository.countDistinctUsers(start, end);
        savePlatformKPI("ACTIVE_USERS", "Total Active Users", BigDecimal.valueOf(activeUsers), 
                        periodType, start, end, "USER", "COUNT");
        
        // Total Transactions
        Long totalTransactions = transactionMetricRepository.countTransactions(start, end);
        savePlatformKPI("TOTAL_TRANSACTIONS", "Total Transactions", BigDecimal.valueOf(totalTransactions), 
                        periodType, start, end, "TRANSACTION", "COUNT");
        
        // Total CO2 Offset
        Double totalCO2 = transactionMetricRepository.calculateTotalCO2Offset(start, end);
        savePlatformKPI("CO2_OFFSET", "Total CO2 Offset", BigDecimal.valueOf(totalCO2), 
                        periodType, start, end, "ENVIRONMENTAL", "TONS");
        
        // Platform Revenue
        Double totalRevenue = transactionMetricRepository.calculateTotalRevenue(start, end);
        savePlatformKPI("PLATFORM_REVENUE", "Platform Revenue", BigDecimal.valueOf(totalRevenue), 
                        periodType, start, end, "FINANCIAL", "USD");
        
        // Average Transaction Value
        BigDecimal avgTransactionValue = totalGMV.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
        savePlatformKPI("AVG_TRANSACTION_VALUE", "Average Transaction Value", avgTransactionValue, 
                        periodType, start, end, "FINANCIAL", "USD");
        
        // Conversion Rate (assume we track visitors separately)
        BigDecimal conversionRate = calculateConversionRate(start, end);
        savePlatformKPI("CONVERSION_RATE", "Conversion Rate", conversionRate, 
                        periodType, start, end, "PERFORMANCE", "PERCENTAGE");
    }
    
    private void calculateUserAnalytics(LocalDateTime start, LocalDateTime end, String periodType) {
        log.debug("Calculating user analytics for period: {} to {}", start, end);
        
        List<Map<String, Object>> userMetrics = jdbcTemplate.queryForList(
            "SELECT user_id, " +
            "COUNT(*) as transaction_count, " +
            "SUM(amount) as total_amount, " +
            "SUM(credit_amount) as total_credits, " +
            "SUM(co2_offset) as total_co2, " +
            "AVG(amount) as avg_transaction " +
            "FROM transaction_metrics " +
            "WHERE timestamp >= ? AND timestamp < ? " +
            "GROUP BY user_id",
            start, end
        );
        
        for (Map<String, Object> metrics : userMetrics) {
            UserAnalytics analytics = new UserAnalytics();
            analytics.setUserId(((Number) metrics.get("user_id")).longValue());
            analytics.setTotalTransactions(((Number) metrics.get("transaction_count")).intValue());
            analytics.setTotalRevenue(new BigDecimal(metrics.get("total_amount").toString()));
            analytics.setTotalCreditsEarned(((Number) metrics.get("total_credits")).doubleValue());
            analytics.setTotalCo2Offset(((Number) metrics.get("total_co2")).doubleValue());
            analytics.setAvgTransactionValue(new BigDecimal(metrics.get("avg_transaction").toString()));
            analytics.setPeriodType(periodType);
            analytics.setPeriodStart(start);
            analytics.setPeriodEnd(end);
            analytics.setCalculatedAt(LocalDateTime.now());
            
            // Calculate engagement score and retention status
            analytics.setEngagementScore(calculateEngagementScore(analytics));
            analytics.setRetentionStatus(determineRetentionStatus(analytics));
            
            userAnalyticsRepository.save(analytics);
        }
    }
    
    private void calculateMarketplaceMetrics(LocalDateTime start, LocalDateTime end, String periodType) {
        log.debug("Calculating marketplace metrics for period: {} to {}", start, end);
        
        // Listing metrics
        Long totalListings = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM listings WHERE created_at >= ? AND created_at < ?",
            Long.class, start, end
        );
        savePlatformKPI("TOTAL_LISTINGS", "Total New Listings", BigDecimal.valueOf(totalListings), 
                        periodType, start, end, "MARKETPLACE", "COUNT");
        
        // Average listing price
        BigDecimal avgListingPrice = jdbcTemplate.queryForObject(
            "SELECT AVG(price_per_credit) FROM listings WHERE created_at >= ? AND created_at < ?",
            BigDecimal.class, start, end
        );
        if (avgListingPrice != null) {
            savePlatformKPI("AVG_LISTING_PRICE", "Average Listing Price", avgListingPrice, 
                           periodType, start, end, "MARKETPLACE", "USD");
        }
        
        // Auction metrics
        Long totalAuctions = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM listings WHERE listing_type = 'AUCTION' AND created_at >= ? AND created_at < ?",
            Long.class, start, end
        );
        savePlatformKPI("TOTAL_AUCTIONS", "Total Auctions", BigDecimal.valueOf(totalAuctions), 
                        periodType, start, end, "MARKETPLACE", "COUNT");
    }
    
    private void calculateEnvironmentalMetrics(LocalDateTime start, LocalDateTime end, String periodType) {
        log.debug("Calculating environmental metrics for period: {} to {}", start, end);
        
        // Total distance tracked
        Double totalDistance = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(distance), 0) FROM trip_records WHERE created_at >= ? AND created_at < ?",
            Double.class, start, end
        );
        savePlatformKPI("TOTAL_DISTANCE", "Total Distance Tracked", BigDecimal.valueOf(totalDistance), 
                        periodType, start, end, "ENVIRONMENTAL", "KM");
        
        // Average CO2 per km
        Double avgCO2PerKm = totalDistance > 0 ? 
            transactionMetricRepository.calculateTotalCO2Offset(start, end) / totalDistance : 0;
        savePlatformKPI("AVG_CO2_PER_KM", "Average CO2 per KM", BigDecimal.valueOf(avgCO2PerKm), 
                        periodType, start, end, "ENVIRONMENTAL", "KG/KM");
        
        // Total verified credits
        Long verifiedCredits = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM carbon_credits WHERE verification_status = 'VERIFIED' " +
            "AND created_at >= ? AND created_at < ?",
            Long.class, start, end
        );
        savePlatformKPI("VERIFIED_CREDITS", "Total Verified Credits", BigDecimal.valueOf(verifiedCredits), 
                        periodType, start, end, "ENVIRONMENTAL", "COUNT");
    }
    
    private void savePlatformKPI(String kpiType, String kpiName, BigDecimal value, 
                                  String periodType, LocalDateTime start, LocalDateTime end,
                                  String category, String unit) {
        PlatformKPI kpi = new PlatformKPI();
        kpi.setKpiType(kpiType);
        kpi.setKpiName(kpiName);
        kpi.setKpiValue(value);
        kpi.setCalculationDate(LocalDateTime.now());
        kpi.setPeriodType(periodType);
        kpi.setPeriodStart(start);
        kpi.setPeriodEnd(end);
        kpi.setCategory(category);
        kpi.setUnit(unit);
        
        // Calculate trend
        PlatformKPI previousKPI = platformKPIRepository.findPreviousKPI(kpiType, periodType, start);
        if (previousKPI != null) {
            kpi.setPreviousValue(previousKPI.getKpiValue());
            if (previousKPI.getKpiValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = value.subtract(previousKPI.getKpiValue())
                    .divide(previousKPI.getKpiValue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                kpi.setChangePercentage(change);
                kpi.setTrend(change.compareTo(BigDecimal.ZERO) > 0 ? "UP" : 
                           change.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE");
            }
        }
        
        platformKPIRepository.save(kpi);
    }
    
    @Transactional
    public void refreshMaterializedViews() {
        log.info("Refreshing materialized views...");
        
        // Refresh hourly aggregates
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hourly_metrics");
        
        // Refresh daily aggregates
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_metrics");
        
        // Refresh user summary
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_summary");
        
        // Refresh marketplace summary
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_marketplace_summary");
        
        log.info("Materialized views refreshed successfully");
    }
    
    @Cacheable(value = "kpiReports", key = "#periodType + '_' + #start + '_' + #end")
    public Map<String, Object> generateKPIReports() {
        log.info("Generating KPI reports...");
        
        Map<String, Object> report = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Executive Summary
        report.put("executiveSummary", generateExecutiveSummary(now));
        
        // Financial Metrics
        report.put("financialMetrics", generateFinancialMetrics(now));
        
        // User Metrics
        report.put("userMetrics", generateUserMetrics(now));
        
        // Environmental Impact
        report.put("environmentalImpact", generateEnvironmentalMetrics(now));
        
        // Marketplace Performance
        report.put("marketplacePerformance", generateMarketplacePerformance(now));
        
        return report;
    }
    
    private Map<String, Object> generateExecutiveSummary(LocalDateTime date) {
        Map<String, Object> summary = new HashMap<>();
        
        // Get latest KPIs
        List<PlatformKPI> latestKPIs = platformKPIRepository.findLatestKPIs(date);
        
        for (PlatformKPI kpi : latestKPIs) {
            Map<String, Object> kpiData = new HashMap<>();
            kpiData.put("value", kpi.getKpiValue());
            kpiData.put("trend", kpi.getTrend());
            kpiData.put("changePercentage", kpi.getChangePercentage());
            kpiData.put("unit", kpi.getUnit());
            summary.put(kpi.getKpiType(), kpiData);
        }
        
        return summary;
    }
    
    private Map<String, Object> generateFinancialMetrics(LocalDateTime date) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("gmv", platformKPIRepository.findByKpiTypeAndDate("GMV", date));
        metrics.put("revenue", platformKPIRepository.findByKpiTypeAndDate("PLATFORM_REVENUE", date));
        metrics.put("avgTransactionValue", platformKPIRepository.findByKpiTypeAndDate("AVG_TRANSACTION_VALUE", date));
        
        return metrics;
    }
    
    private Map<String, Object> generateUserMetrics(LocalDateTime date) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("activeUsers", platformKPIRepository.findByKpiTypeAndDate("ACTIVE_USERS", date));
        metrics.put("newUsers", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE created_at >= ?", 
            Long.class, date.minusDays(30)));
        metrics.put("retentionRate", calculateRetentionRate(date));
        
        return metrics;
    }
    
    private Map<String, Object> generateEnvironmentalMetrics(LocalDateTime date) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("totalCO2Offset", platformKPIRepository.findByKpiTypeAndDate("CO2_OFFSET", date));
        metrics.put("totalDistance", platformKPIRepository.findByKpiTypeAndDate("TOTAL_DISTANCE", date));
        metrics.put("verifiedCredits", platformKPIRepository.findByKpiTypeAndDate("VERIFIED_CREDITS", date));
        
        return metrics;
    }
    
    private Map<String, Object> generateMarketplacePerformance(LocalDateTime date) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("totalListings", platformKPIRepository.findByKpiTypeAndDate("TOTAL_LISTINGS", date));
        metrics.put("avgListingPrice", platformKPIRepository.findByKpiTypeAndDate("AVG_LISTING_PRICE", date));
        metrics.put("totalAuctions", platformKPIRepository.findByKpiTypeAndDate("TOTAL_AUCTIONS", date));
        
        return metrics;
    }
    
    private BigDecimal calculateConversionRate(LocalDateTime start, LocalDateTime end) {
        // This would normally integrate with analytics tracking
        // For now, return a simulated value
        return BigDecimal.valueOf(3.5);
    }
    
    private BigDecimal calculateEngagementScore(UserAnalytics analytics) {
        // Simple engagement score calculation
        BigDecimal score = BigDecimal.ZERO;
        
        if (analytics.getTotalTransactions() > 0) {
            score = score.add(BigDecimal.valueOf(Math.min(analytics.getTotalTransactions() * 10, 50)));
        }
        
        if (analytics.getDaysActive() != null) {
            score = score.add(BigDecimal.valueOf(Math.min(analytics.getDaysActive() * 2, 30)));
        }
        
        if (analytics.getTotalCo2Offset() > 0) {
            score = score.add(BigDecimal.valueOf(Math.min(analytics.getTotalCo2Offset() / 10, 20)));
        }
        
        return score.min(BigDecimal.valueOf(100));
    }
    
    private String determineRetentionStatus(UserAnalytics analytics) {
        if (analytics.getLastActiveDate() == null) {
            return "INACTIVE";
        }
        
        long daysSinceActive = ChronoUnit.DAYS.between(analytics.getLastActiveDate(), LocalDateTime.now());
        
        if (daysSinceActive <= 7) {
            return "ACTIVE";
        } else if (daysSinceActive <= 30) {
            return "AT_RISK";
        } else {
            return "CHURNED";
        }
    }
    
    private BigDecimal calculateRetentionRate(LocalDateTime date) {
        // Calculate 30-day retention rate
        Long totalUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE created_at <= ?",
            Long.class, date.minusDays(30)
        );
        
        Long activeUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT user_id) FROM transactions WHERE created_at >= ?",
            Long.class, date.minusDays(30)
        );
        
        if (totalUsers == 0) return BigDecimal.ZERO;
        
        return BigDecimal.valueOf(activeUsers)
            .divide(BigDecimal.valueOf(totalUsers), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
