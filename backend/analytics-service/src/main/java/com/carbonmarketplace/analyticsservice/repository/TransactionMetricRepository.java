package com.carbonmarketplace.analyticsservice.repository;

import com.carbonmarketplace.analyticsservice.entity.TransactionMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TransactionMetricRepository extends JpaRepository<TransactionMetric, Long> {
    
    @Query("SELECT COALESCE(SUM(tm.amount), 0) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    BigDecimal calculateTotalGMV(@Param("start") LocalDateTime start, 
                                  @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(DISTINCT tm.userId) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    Long countDistinctUsers(@Param("start") LocalDateTime start, 
                            @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(tm) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    Long countTransactions(@Param("start") LocalDateTime start, 
                          @Param("end") LocalDateTime end);
    
    @Query("SELECT COALESCE(SUM(tm.co2Offset), 0) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    Double calculateTotalCO2Offset(@Param("start") LocalDateTime start, 
                                   @Param("end") LocalDateTime end);
    
    @Query("SELECT COALESCE(SUM(tm.revenueGenerated), 0) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    Double calculateTotalRevenue(@Param("start") LocalDateTime start, 
                                 @Param("end") LocalDateTime end);
    
    @Query("SELECT tm FROM TransactionMetric tm " +
           "WHERE tm.userId = :userId AND tm.timestamp >= :start AND tm.timestamp < :end")
    List<TransactionMetric> findByUserIdAndPeriod(@Param("userId") Long userId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
    
    @Query("SELECT tm.transactionType as type, COUNT(tm) as count, SUM(tm.amount) as total " +
           "FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end " +
           "GROUP BY tm.transactionType")
    List<Map<String, Object>> getTransactionTypeBreakdown(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);
    
    @Query("SELECT tm.status as status, COUNT(tm) as count " +
           "FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end " +
           "GROUP BY tm.status")
    List<Map<String, Object>> getTransactionStatusBreakdown(@Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);
    
    @Query(value = "SELECT DATE_TRUNC('hour', timestamp) as hour, " +
                   "COUNT(*) as transaction_count, " +
                   "SUM(amount) as total_amount, " +
                   "SUM(co2_offset) as total_co2 " +
                   "FROM transaction_metrics " +
                   "WHERE timestamp >= :start AND timestamp < :end " +
                   "GROUP BY DATE_TRUNC('hour', timestamp) " +
                   "ORDER BY hour", nativeQuery = true)
    List<Map<String, Object>> getHourlyMetrics(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
    
    @Query(value = "SELECT DATE_TRUNC('day', timestamp) as day, " +
                   "COUNT(*) as transaction_count, " +
                   "SUM(amount) as total_amount, " +
                   "SUM(co2_offset) as total_co2, " +
                   "COUNT(DISTINCT user_id) as unique_users " +
                   "FROM transaction_metrics " +
                   "WHERE timestamp >= :start AND timestamp < :end " +
                   "GROUP BY DATE_TRUNC('day', timestamp) " +
                   "ORDER BY day", nativeQuery = true)
    List<Map<String, Object>> getDailyMetrics(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
    
    @Query("SELECT AVG(tm.amount) FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end")
    BigDecimal calculateAverageTransactionValue(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
    
    List<TransactionMetric> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT tm.userId, COUNT(tm) as count FROM TransactionMetric tm " +
           "WHERE tm.timestamp >= :start AND tm.timestamp < :end " +
           "GROUP BY tm.userId " +
           "ORDER BY COUNT(tm) DESC")
    List<Map<Long, Long>> getTopUsers(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
