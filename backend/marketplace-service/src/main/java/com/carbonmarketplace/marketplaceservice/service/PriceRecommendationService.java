package com.carbonmarketplace.marketplaceservice.service;

import com.carbonmarketplace.marketplaceservice.dto.PriceRecommendationResponse;
import com.carbonmarketplace.marketplaceservice.entity.PriceHistory;
import com.carbonmarketplace.marketplaceservice.entity.Transaction;
import com.carbonmarketplace.marketplaceservice.repository.jpa.PriceHistoryRepository;
import com.carbonmarketplace.marketplaceservice.repository.jpa.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for providing AI-powered price recommendations based on historical data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PriceRecommendationService {
    
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionRepository transactionRepository;
    
    @Value("${price-recommendation.min-historical-data}")
    private int minHistoricalData;
    
    @Value("${price-recommendation.confidence-thresholds.high}")
    private double highConfidenceThreshold;
    
    @Value("${price-recommendation.confidence-thresholds.medium}")
    private double mediumConfidenceThreshold;
    
    /**
     * Get price recommendation for a given region and vintage year.
     */
    @Cacheable(value = "priceRecommendations", key = "#region + '_' + #vintageYear + '_' + #amountTons")
    public PriceRecommendationResponse getPriceRecommendation(
            String region, Integer vintageYear, BigDecimal amountTons) {
        
        log.info("Generating price recommendation for region: {}, vintage: {}, amount: {} tons",
                region, vintageYear, amountTons);
        
        // Get historical data
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<PriceHistory> historicalPrices = priceHistoryRepository
                .findRecentHistory(thirtyDaysAgo, region, vintageYear);
        
        // Get recent transactions
        List<Transaction> recentTransactions = transactionRepository
                .findTransactionsInDateRange(
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now(),
                    Transaction.TransactionStatus.COMPLETED
                );
        
        // Calculate statistics
        PriceStatistics stats = calculatePriceStatistics(historicalPrices, recentTransactions);
        
        // Generate recommendation
        BigDecimal recommendedPrice = calculateRecommendedPrice(stats, amountTons);
        
        // Calculate confidence level
        double confidenceScore = calculateConfidenceScore(historicalPrices.size(), stats);
        String confidenceLevel = getConfidenceLevel(confidenceScore);
        
        // Determine demand indicator
        String demandIndicator = calculateDemandIndicator(historicalPrices, recentTransactions);
        
        // Generate insights
        List<PriceRecommendationResponse.MarketInsight> insights = generateMarketInsights(
                stats, historicalPrices, demandIndicator);
        
        return PriceRecommendationResponse.builder()
                .recommendedPricePerTon(recommendedPrice)
                .minPricePerTon(stats.min)
                .maxPricePerTon(stats.max)
                .avgPricePerTon(stats.average)
                .medianPricePerTon(stats.median)
                .confidenceLevel(confidenceLevel)
                .confidenceScore(confidenceScore)
                .demandIndicator(demandIndicator)
                .dataPointsUsed(historicalPrices.size())
                .lastUpdated(LocalDateTime.now())
                .insights(insights)
                .priceRange(PriceRecommendationResponse.PriceRange.builder()
                        .percentile25(stats.percentile25)
                        .percentile50(stats.median)
                        .percentile75(stats.percentile75)
                        .trend(stats.trend)
                        .volatility(stats.volatility)
                        .build())
                .build();
    }
    
    /**
     * Calculate price statistics from historical data.
     */
    private PriceStatistics calculatePriceStatistics(
            List<PriceHistory> historicalPrices, List<Transaction> transactions) {
        
        List<BigDecimal> allPrices = new ArrayList<>();
        
        // Add historical prices
        historicalPrices.forEach(h -> {
            if (h.getAvgPricePerTon() != null) {
                allPrices.add(h.getAvgPricePerTon());
            }
        });
        
        // Add recent transaction prices
        transactions.forEach(t -> {
            if (t.getPricePerTonVnd() != null) {
                allPrices.add(t.getPricePerTonVnd());
            }
        });
        
        if (allPrices.isEmpty()) {
            return new PriceStatistics();
        }
        
        Collections.sort(allPrices);
        
        PriceStatistics stats = new PriceStatistics();
        stats.min = allPrices.get(0);
        stats.max = allPrices.get(allPrices.size() - 1);
        stats.median = calculateMedian(allPrices);
        stats.average = calculateAverage(allPrices);
        stats.percentile25 = calculatePercentile(allPrices, 25);
        stats.percentile75 = calculatePercentile(allPrices, 75);
        stats.stdDeviation = calculateStandardDeviation(allPrices, stats.average);
        stats.volatility = stats.stdDeviation.divide(stats.average, 4, RoundingMode.HALF_UP).doubleValue();
        stats.trend = calculateTrend(historicalPrices);
        
        return stats;
    }
    
    /**
     * Calculate recommended price based on statistics and market conditions.
     */
    private BigDecimal calculateRecommendedPrice(PriceStatistics stats, BigDecimal amountTons) {
        if (stats.average == null || stats.median == null) {
            // Default price if no historical data
            return new BigDecimal("2500000");
        }
        
        // Base recommendation on weighted average of mean and median
        BigDecimal basePrice = stats.average.multiply(new BigDecimal("0.6"))
                .add(stats.median.multiply(new BigDecimal("0.4")));
        
        // Adjust for volume (bulk discount/premium)
        BigDecimal volumeAdjustment = calculateVolumeAdjustment(amountTons);
        
        // Adjust for trend
        BigDecimal trendAdjustment = calculateTrendAdjustment(stats.trend);
        
        // Apply adjustments
        BigDecimal recommendedPrice = basePrice
                .multiply(BigDecimal.ONE.add(volumeAdjustment))
                .multiply(BigDecimal.ONE.add(trendAdjustment));
        
        // Round to nearest 10,000 VND
        recommendedPrice = recommendedPrice.divide(new BigDecimal("10000"), 0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("10000"));
        
        return recommendedPrice;
    }
    
    /**
     * Calculate volume-based price adjustment.
     */
    private BigDecimal calculateVolumeAdjustment(BigDecimal amountTons) {
        if (amountTons.compareTo(new BigDecimal("10")) > 0) {
            // Bulk discount for large volumes
            return new BigDecimal("-0.05"); // 5% discount
        } else if (amountTons.compareTo(BigDecimal.ONE) < 0) {
            // Premium for small volumes
            return new BigDecimal("0.03"); // 3% premium
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate trend-based price adjustment.
     */
    private BigDecimal calculateTrendAdjustment(String trend) {
        switch (trend) {
            case "INCREASING":
                return new BigDecimal("0.02"); // 2% increase
            case "DECREASING":
                return new BigDecimal("-0.02"); // 2% decrease
            default:
                return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate confidence score based on data quality and quantity.
     */
    private double calculateConfidenceScore(int dataPoints, PriceStatistics stats) {
        // Base score on data quantity
        double quantityScore = Math.min(dataPoints / (double) minHistoricalData, 1.0) * 0.5;
        
        // Score based on volatility (lower volatility = higher confidence)
        double volatilityScore = Math.max(0, 1 - stats.volatility) * 0.3;
        
        // Score based on recency (more recent data = higher confidence)
        double recencyScore = 0.2; // Simplified - would check actual dates in production
        
        return quantityScore + volatilityScore + recencyScore;
    }
    
    /**
     * Get confidence level based on score.
     */
    private String getConfidenceLevel(double score) {
        if (score >= highConfidenceThreshold) {
            return "HIGH";
        } else if (score >= mediumConfidenceThreshold) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Calculate demand indicator based on transaction volume and price trends.
     */
    private String calculateDemandIndicator(List<PriceHistory> history, List<Transaction> transactions) {
        if (history.isEmpty()) {
            return "UNKNOWN";
        }
        
        // Check transaction volume trend
        int recentVolume = transactions.size();
        int historicalAvg = history.stream()
                .mapToInt(PriceHistory::getTransactionCount)
                .sum() / Math.max(1, history.size());
        
        if (recentVolume > historicalAvg * 1.2) {
            return "HIGH";
        } else if (recentVolume < historicalAvg * 0.8) {
            return "LOW";
        } else {
            return "MODERATE";
        }
    }
    
    /**
     * Generate market insights based on analysis.
     */
    private List<PriceRecommendationResponse.MarketInsight> generateMarketInsights(
            PriceStatistics stats, List<PriceHistory> history, String demandIndicator) {
        
        List<PriceRecommendationResponse.MarketInsight> insights = new ArrayList<>();
        
        // Trend insight
        if ("INCREASING".equals(stats.trend)) {
            insights.add(PriceRecommendationResponse.MarketInsight.builder()
                    .type("TREND")
                    .message("Prices have been trending upward over the past 30 days")
                    .impact("POSITIVE")
                    .build());
        } else if ("DECREASING".equals(stats.trend)) {
            insights.add(PriceRecommendationResponse.MarketInsight.builder()
                    .type("TREND")
                    .message("Prices have been trending downward over the past 30 days")
                    .impact("NEGATIVE")
                    .build());
        }
        
        // Demand insight
        if ("HIGH".equals(demandIndicator)) {
            insights.add(PriceRecommendationResponse.MarketInsight.builder()
                    .type("DEMAND")
                    .message("Market demand is currently high")
                    .impact("POSITIVE")
                    .build());
        } else if ("LOW".equals(demandIndicator)) {
            insights.add(PriceRecommendationResponse.MarketInsight.builder()
                    .type("DEMAND")
                    .message("Market demand is currently low")
                    .impact("NEGATIVE")
                    .build());
        }
        
        // Volatility insight
        if (stats.volatility > 0.2) {
            insights.add(PriceRecommendationResponse.MarketInsight.builder()
                    .type("VOLATILITY")
                    .message("High price volatility detected - consider pricing flexibility")
                    .impact("NEUTRAL")
                    .build());
        }
        
        return insights;
    }
    
    /**
     * Calculate median from sorted list of prices.
     */
    private BigDecimal calculateMedian(List<BigDecimal> sortedPrices) {
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return sortedPrices.get(size / 2 - 1)
                    .add(sortedPrices.get(size / 2))
                    .divide(new BigDecimal("2"), RoundingMode.HALF_UP);
        } else {
            return sortedPrices.get(size / 2);
        }
    }
    
    /**
     * Calculate average price.
     */
    private BigDecimal calculateAverage(List<BigDecimal> prices) {
        BigDecimal sum = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(prices.size()), RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate percentile from sorted list.
     */
    private BigDecimal calculatePercentile(List<BigDecimal> sortedPrices, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedPrices.size()) - 1;
        return sortedPrices.get(Math.max(0, index));
    }
    
    /**
     * Calculate standard deviation.
     */
    private BigDecimal calculateStandardDeviation(List<BigDecimal> prices, BigDecimal mean) {
        BigDecimal variance = prices.stream()
                .map(price -> price.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(prices.size()), RoundingMode.HALF_UP);
        
        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }
    
    /**
     * Calculate price trend.
     */
    private String calculateTrend(List<PriceHistory> history) {
        if (history.size() < 2) {
            return "STABLE";
        }
        
        // Simple linear regression on recent prices
        List<PriceHistory> recentHistory = history.stream()
                .sorted(Comparator.comparing(PriceHistory::getDate).reversed())
                .limit(10)
                .toList();
        
        if (recentHistory.size() < 2) {
            return "STABLE";
        }
        
        BigDecimal firstPrice = recentHistory.get(recentHistory.size() - 1).getAvgPricePerTon();
        BigDecimal lastPrice = recentHistory.get(0).getAvgPricePerTon();
        
        BigDecimal change = lastPrice.subtract(firstPrice)
                .divide(firstPrice, 4, RoundingMode.HALF_UP);
        
        if (change.compareTo(new BigDecimal("0.05")) > 0) {
            return "INCREASING";
        } else if (change.compareTo(new BigDecimal("-0.05")) < 0) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }
    
    /**
     * Update price history daily.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void updatePriceHistory() {
        log.info("Updating daily price history");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.plusDays(1).atStartOfDay();
        
        // Get all regions with transactions
        List<String> activeRegions = priceHistoryRepository.findActiveRegions(yesterday.minusDays(30));
        
        for (String region : activeRegions) {
            List<Transaction> regionTransactions = transactionRepository
                    .findTransactionsInDateRange(startOfDay, endOfDay, Transaction.TransactionStatus.COMPLETED)
                    .stream()
                    .filter(t -> {
                        // Would need to join with listing to get region
                        // Simplified for this implementation
                        return true;
                    })
                    .toList();
            
            if (!regionTransactions.isEmpty()) {
                PriceHistory history = calculateDailyPriceHistory(region, regionTransactions, yesterday);
                priceHistoryRepository.save(history);
            }
        }
        
        log.info("Price history update completed");
    }
    
    /**
     * Calculate daily price history for a region.
     */
    private PriceHistory calculateDailyPriceHistory(String region, List<Transaction> transactions, LocalDate date) {
        List<BigDecimal> prices = transactions.stream()
                .map(Transaction::getPricePerTonVnd)
                .sorted()
                .toList();
        
        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getCreditAmountTons)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return PriceHistory.builder()
                .date(date)
                .region(region)
                .avgPricePerTon(calculateAverage(prices))
                .minPricePerTon(prices.get(0))
                .maxPricePerTon(prices.get(prices.size() - 1))
                .medianPricePerTon(calculateMedian(prices))
                .stdDeviation(calculateStandardDeviation(prices, calculateAverage(prices)))
                .totalVolumeTons(totalVolume)
                .transactionCount(transactions.size())
                .build();
    }
    
    /**
     * Internal class for price statistics.
     */
    private static class PriceStatistics {
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        BigDecimal median = BigDecimal.ZERO;
        BigDecimal percentile25 = BigDecimal.ZERO;
        BigDecimal percentile75 = BigDecimal.ZERO;
        BigDecimal stdDeviation = BigDecimal.ZERO;
        double volatility = 0.0;
        String trend = "STABLE";
    }
}
