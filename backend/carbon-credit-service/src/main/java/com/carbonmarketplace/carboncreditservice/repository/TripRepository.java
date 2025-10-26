package com.carbonmarketplace.carboncreditservice.repository;

import com.carbonmarketplace.carboncreditservice.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Trip entity
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    
    List<Trip> findByVehicleIdAndStartTimeBetweenOrderByStartTimeDesc(
            UUID vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startTime AND t.endTime <= :endTime " +
           "AND (:includeAnomalous = true OR t.hasAnomaly = false) " +
           "ORDER BY t.startTime DESC")
    List<Trip> findTripsForCalculation(
            @Param("vehicleId") UUID vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("includeAnomalous") boolean includeAnomalous
    );
    
    @Query("SELECT SUM(t.distanceKm) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startTime AND t.endTime <= :endTime " +
           "AND t.hasAnomaly = false")
    BigDecimal calculateTotalDistance(
            @Param("vehicleId") UUID vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT SUM(t.co2SavedKg) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startTime AND t.endTime <= :endTime " +
           "AND t.hasAnomaly = false")
    BigDecimal calculateTotalCO2Saved(
            @Param("vehicleId") UUID vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startTime AND t.endTime <= :endTime " +
           "AND t.hasAnomaly = false")
    Integer countTrips(
            @Param("vehicleId") UUID vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    List<Trip> findByVerificationRequestVerificationId(UUID verificationId);
    
    List<Trip> findByTripIdIn(List<UUID> tripIds);
    
    @Query("SELECT AVG(t.dataQualityScore) FROM Trip t WHERE t.tripId IN :tripIds")
    BigDecimal calculateAverageDataQualityScore(@Param("tripIds") List<UUID> tripIds);
}
