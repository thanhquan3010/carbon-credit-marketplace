package com.carbonmarketplace.vehicleservice.repository;

import com.carbonmarketplace.vehicleservice.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Trip entity operations
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Find trips by vehicle ID
     */
    List<Trip> findByVehicleId(UUID vehicleId);

    /**
     * Find trips by vehicle ID with pagination
     */
    Page<Trip> findByVehicleId(UUID vehicleId, Pageable pageable);

    /**
     * Find trips by vehicle ID within date range
     */
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
            "AND t.startTime >= :startDate AND t.endTime <= :endDate " +
            "ORDER BY t.startTime DESC")
    List<Trip> findByVehicleIdAndDateRange(@Param("vehicleId") UUID vehicleId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find trip by external trip ID
     */
    Optional<Trip> findByExternalTripId(String externalTripId);

    /**
     * Check if external trip ID already exists
     */
    boolean existsByExternalTripId(String externalTripId);

    /**
     * Find trips by sync batch ID
     */
    List<Trip> findBySyncBatchId(UUID syncBatchId);

    /**
     * Find trips with anomalies
     */
    @Query("SELECT t FROM Trip t WHERE t.hasAnomaly = true ORDER BY t.startTime DESC")
    Page<Trip> findTripsWithAnomalies(Pageable pageable);

    /**
     * Find trips with anomalies for a specific vehicle
     */
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId AND t.hasAnomaly = true")
    List<Trip> findAnomalousTripsForVehicle(@Param("vehicleId") UUID vehicleId);

    /**
     * Find unverified trips
     */
    @Query("SELECT t FROM Trip t WHERE t.isVerified = false ORDER BY t.createdAt ASC")
    Page<Trip> findUnverifiedTrips(Pageable pageable);

    /**
     * Calculate total distance for a vehicle
     */
    @Query("SELECT COALESCE(SUM(t.distanceKm), 0) FROM Trip t WHERE t.vehicleId = :vehicleId AND t.deletedAt IS NULL")
    BigDecimal getTotalDistanceByVehicleId(@Param("vehicleId") UUID vehicleId);

    /**
     * Calculate total CO2 saved for a vehicle
     */
    @Query("SELECT COALESCE(SUM(t.co2SavedKg), 0) FROM Trip t WHERE t.vehicleId = :vehicleId AND t.deletedAt IS NULL")
    BigDecimal getTotalCo2SavedByVehicleId(@Param("vehicleId") UUID vehicleId);

    /**
     * Count trips for a vehicle
     */
    long countByVehicleId(UUID vehicleId);

    /**
     * Get trips statistics for a vehicle within date range
     */
    @Query("SELECT " +
            "COUNT(t) as tripCount, " +
            "COALESCE(SUM(t.distanceKm), 0) as totalDistance, " +
            "COALESCE(SUM(t.co2SavedKg), 0) as totalCo2Saved, " +
            "COALESCE(SUM(t.energyConsumedKwh), 0) as totalEnergy, " +
            "COALESCE(AVG(t.distanceKm), 0) as avgDistance, " +
            "COALESCE(AVG(t.avgSpeed), 0) as avgSpeed " +
            "FROM Trip t " +
            "WHERE t.vehicleId = :vehicleId " +
            "AND t.startTime >= :startDate " +
            "AND t.startTime <= :endDate " +
            "AND t.deletedAt IS NULL")
    Object[] getVehicleTripStatistics(@Param("vehicleId") UUID vehicleId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Find duplicate trips (same start and end time for a vehicle)
     */
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
            "AND t.startTime = :startTime AND t.endTime = :endTime " +
            "AND t.tripId != :excludeTripId")
    List<Trip> findDuplicateTrips(@Param("vehicleId") UUID vehicleId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("excludeTripId") UUID excludeTripId);

    /**
     * Find overlapping trips (trips that overlap in time for the same vehicle)
     */
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId " +
            "AND ((t.startTime <= :startTime AND t.endTime > :startTime) " +
            "OR (t.startTime < :endTime AND t.endTime >= :endTime) " +
            "OR (t.startTime >= :startTime AND t.endTime <= :endTime)) " +
            "AND t.tripId != :excludeTripId")
    List<Trip> findOverlappingTrips(@Param("vehicleId") UUID vehicleId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("excludeTripId") UUID excludeTripId);

    /**
     * Find trips with high speed
     */
    @Query("SELECT t FROM Trip t WHERE t.avgSpeed > :speedThreshold ORDER BY t.avgSpeed DESC")
    List<Trip> findHighSpeedTrips(@Param("speedThreshold") BigDecimal speedThreshold);

    /**
     * Find trips with unusual efficiency
     */
    @Query("SELECT t FROM Trip t WHERE t.efficiencyKwhPerKm < :minEfficiency OR t.efficiencyKwhPerKm > :maxEfficiency")
    List<Trip> findTripsWithUnusualEfficiency(@Param("minEfficiency") BigDecimal minEfficiency,
                                               @Param("maxEfficiency") BigDecimal maxEfficiency);

    /**
     * Delete trips older than a specific date (for data retention)
     */
    @Query("DELETE FROM Trip t WHERE t.createdAt < :cutoffDate")
    void deleteTripsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find recent trips for a vehicle
     */
    @Query("SELECT t FROM Trip t WHERE t.vehicleId = :vehicleId ORDER BY t.startTime DESC")
    Page<Trip> findRecentTripsByVehicleId(@Param("vehicleId") UUID vehicleId, Pageable pageable);

    /**
     * Get monthly trip summary
     */
    @Query(value = "SELECT " +
            "DATE_TRUNC('month', start_time) as month, " +
            "COUNT(*) as trip_count, " +
            "SUM(distance_km) as total_distance, " +
            "SUM(co2_saved_kg) as total_co2_saved, " +
            "SUM(energy_consumed_kwh) as total_energy " +
            "FROM trips " +
            "WHERE vehicle_id = :vehicleId " +
            "AND start_time >= :startDate " +
            "AND start_time <= :endDate " +
            "AND deleted_at IS NULL " +
            "GROUP BY DATE_TRUNC('month', start_time) " +
            "ORDER BY month DESC",
            nativeQuery = true)
    List<Object[]> getMonthlyTripSummary(@Param("vehicleId") UUID vehicleId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Batch check for existing external trip IDs
     */
    @Query("SELECT t.externalTripId FROM Trip t WHERE t.externalTripId IN :externalTripIds")
    List<String> findExistingExternalTripIds(@Param("externalTripIds") List<String> externalTripIds);
}
