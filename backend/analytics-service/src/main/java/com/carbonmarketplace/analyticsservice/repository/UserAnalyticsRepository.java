package com.carbonmarketplace.analyticsservice.repository;

import com.carbonmarketplace.analyticsservice.entity.UserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnalyticsRepository extends JpaRepository<UserAnalytics, Long> {
    
    Optional<UserAnalytics> findByUserIdAndPeriodTypeAndPeriodStart(Long userId, 
                                                                    String periodType, 
                                                                    LocalDateTime periodStart);
    
    List<UserAnalytics> findByUserIdAndPeriodType(Long userId, String periodType);
    
    @Query("SELECT ua FROM UserAnalytics ua " +
           "WHERE ua.userId = :userId " +
           "AND ua.periodStart >= :start " +
           "AND ua.periodEnd <= :end " +
           "ORDER BY ua.periodStart")
    List<UserAnalytics> findUserAnalyticsInPeriod(@Param("userId") Long userId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
    
    @Query("SELECT ua FROM UserAnalytics ua " +
           "WHERE ua.retentionStatus = :status " +
           "AND ua.periodType = :periodType " +
           "AND ua.calculatedAt >= :since")
    List<UserAnalytics> findByRetentionStatus(@Param("status") String status,
                                              @Param("periodType") String periodType,
                                              @Param("since") LocalDateTime since);
    
    @Query("SELECT ua FROM UserAnalytics ua " +
           "WHERE ua.churnProbability > :threshold " +
           "AND ua.periodType = 'MONTHLY' " +
           "ORDER BY ua.churnProbability DESC")
    List<UserAnalytics> findHighChurnRiskUsers(@Param("threshold") Double threshold);
    
    @Query("SELECT ua FROM UserAnalytics ua " +
           "WHERE ua.lifetimeValue > :minValue " +
           "ORDER BY ua.lifetimeValue DESC")
    List<UserAnalytics> findHighValueUsers(@Param("minValue") Double minValue);
    
    @Query("SELECT AVG(ua.engagementScore) FROM UserAnalytics ua " +
           "WHERE ua.periodType = :periodType " +
           "AND ua.periodStart >= :start")
    Double calculateAverageEngagementScore(@Param("periodType") String periodType,
                                          @Param("start") LocalDateTime start);
    
    @Query("SELECT COUNT(ua) FROM UserAnalytics ua " +
           "WHERE ua.retentionStatus = 'ACTIVE' " +
           "AND ua.periodType = :periodType " +
           "AND ua.calculatedAt >= :since")
    Long countActiveUsers(@Param("periodType") String periodType,
                         @Param("since") LocalDateTime since);
    
    @Query("SELECT ua.userType, COUNT(ua) as count FROM UserAnalytics ua " +
           "WHERE ua.periodType = :periodType " +
           "AND ua.periodStart = :periodStart " +
           "GROUP BY ua.userType")
    List<Object[]> getUserTypeDistribution(@Param("periodType") String periodType,
                                          @Param("periodStart") LocalDateTime periodStart);
}
