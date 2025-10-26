package com.carbonmarketplace.verificationservice.repository;

import com.carbonmarketplace.verificationservice.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for trip operations (read-only for verification).
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByVehicleId(UUID vehicleId);

    List<Trip> findByVerificationId(UUID verificationId);

    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate " +
           "ORDER BY t.startTime ASC")
    List<Trip> findByVehicleAndDateRange(@Param("vehicleId") UUID vehicleId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.hasAnomaly = true")
    List<Trip> findAnomalousTrips(@Param("vehicleId") UUID vehicleId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    Long countTripsByVehicleAndDateRange(@Param("vehicleId") UUID vehicleId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.distanceKm) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    BigDecimal getTotalDistanceByVehicleAndDateRange(@Param("vehicleId") UUID vehicleId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.co2SavedKg) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    BigDecimal getTotalCO2SavedByVehicleAndDateRange(@Param("vehicleId") UUID vehicleId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(t.avgSpeed) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    BigDecimal getAverageSpeedByVehicleAndDateRange(@Param("vehicleId") UUID vehicleId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(t.dataQualityScore) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate " +
           "AND t.dataQualityScore IS NOT NULL")
    BigDecimal getAverageDataQualityScore(@Param("vehicleId") UUID vehicleId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.dataSource, COUNT(t) FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate " +
           "GROUP BY t.dataSource")
    List<Object[]> getDataSourceDistribution(@Param("vehicleId") UUID vehicleId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
           "AND ABS(EXTRACT(EPOCH FROM (t.endTime - t.startTime))/3600 - " +
           "(t.distanceKm / t.avgSpeed)) > 0.5 " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    List<Trip> findTripsWithTimeDistanceDiscrepancy(@Param("vehicleId") UUID vehicleId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
}
