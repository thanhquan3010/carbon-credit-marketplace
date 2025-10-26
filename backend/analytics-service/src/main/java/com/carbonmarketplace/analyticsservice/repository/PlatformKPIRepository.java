package com.carbonmarketplace.analyticsservice.repository;

import com.carbonmarketplace.analyticsservice.entity.PlatformKPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformKPIRepository extends JpaRepository<PlatformKPI, Long> {
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.kpiType = :kpiType " +
           "AND kpi.periodType = :periodType " +
           "AND kpi.periodStart < :currentStart " +
           "ORDER BY kpi.periodStart DESC")
    PlatformKPI findPreviousKPI(@Param("kpiType") String kpiType,
                                @Param("periodType") String periodType,
                                @Param("currentStart") LocalDateTime currentStart);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.calculationDate >= :date " +
           "AND kpi.periodType = 'DAILY' " +
           "ORDER BY kpi.calculationDate DESC")
    List<PlatformKPI> findLatestKPIs(@Param("date") LocalDateTime date);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.kpiType = :kpiType " +
           "AND kpi.calculationDate >= :date " +
           "ORDER BY kpi.calculationDate DESC")
    Optional<PlatformKPI> findByKpiTypeAndDate(@Param("kpiType") String kpiType,
                                               @Param("date") LocalDateTime date);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.category = :category " +
           "AND kpi.periodType = :periodType " +
           "AND kpi.periodStart >= :start " +
           "AND kpi.periodEnd <= :end " +
           "ORDER BY kpi.calculationDate DESC")
    List<PlatformKPI> findByCategoryAndPeriod(@Param("category") String category,
                                               @Param("periodType") String periodType,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.kpiType = :kpiType " +
           "AND kpi.periodType = :periodType " +
           "AND kpi.periodStart >= :start " +
           "ORDER BY kpi.periodStart")
    List<PlatformKPI> findKPITimeSeries(@Param("kpiType") String kpiType,
                                        @Param("periodType") String periodType,
                                        @Param("start") LocalDateTime start);
    
    @Query("SELECT DISTINCT kpi.kpiType FROM PlatformKPI kpi " +
           "WHERE kpi.category = :category")
    List<String> findKPITypesByCategory(@Param("category") String category);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.trend = :trend " +
           "AND kpi.periodType = :periodType " +
           "AND kpi.calculationDate >= :since " +
           "ORDER BY kpi.changePercentage DESC")
    List<PlatformKPI> findByTrendAndPeriod(@Param("trend") String trend,
                                           @Param("periodType") String periodType,
                                           @Param("since") LocalDateTime since);
    
    List<PlatformKPI> findByPeriodTypeAndPeriodStartBetween(String periodType, 
                                                            LocalDateTime start, 
                                                            LocalDateTime end);
    
    @Query("SELECT kpi FROM PlatformKPI kpi " +
           "WHERE kpi.achievementPercentage < :threshold " +
           "AND kpi.calculationDate >= :since " +
           "ORDER BY kpi.achievementPercentage")
    List<PlatformKPI> findUnderperformingKPIs(@Param("threshold") Double threshold,
                                              @Param("since") LocalDateTime since);
}
