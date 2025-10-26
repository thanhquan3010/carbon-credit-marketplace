package com.carbonmarketplace.vehicleservice.repository;

import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Vehicle entity operations
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    /**
     * Find vehicle by VIN
     */
    Optional<Vehicle> findByVin(String vin);

    /**
     * Find all vehicles by owner ID
     */
    List<Vehicle> findByOwnerId(UUID ownerId);

    /**
     * Find all vehicles by owner ID with pagination
     */
    Page<Vehicle> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Find active vehicles by owner ID
     */
    @Query("SELECT v FROM Vehicle v WHERE v.ownerId = :ownerId AND v.isActive = true AND v.deletedAt IS NULL")
    List<Vehicle> findActiveVehiclesByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Check if VIN already exists
     */
    boolean existsByVin(String vin);

    /**
     * Check if VIN exists for another vehicle (excluding current vehicle)
     */
    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE v.vin = :vin AND v.vehicleId != :vehicleId")
    boolean existsByVinAndNotVehicleId(@Param("vin") String vin, @Param("vehicleId") UUID vehicleId);

    /**
     * Find vehicles by verification status
     */
    List<Vehicle> findByVerificationStatus(Vehicle.VerificationStatus status);

    /**
     * Find vehicles by verification status with pagination
     */
    Page<Vehicle> findByVerificationStatus(Vehicle.VerificationStatus status, Pageable pageable);

    /**
     * Find vehicles that need synchronization
     */
    @Query("SELECT v FROM Vehicle v WHERE v.nextSyncAt <= :currentTime AND v.syncStatus != 'IN_PROGRESS' AND v.isActive = true")
    List<Vehicle> findVehiclesNeedingSync(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find vehicles by data source
     */
    List<Vehicle> findByDataSource(Vehicle.DataSource dataSource);

    /**
     * Find vehicles by sync status
     */
    List<Vehicle> findBySyncStatus(Vehicle.SyncStatus syncStatus);

    /**
     * Count vehicles by owner
     */
    long countByOwnerId(UUID ownerId);

    /**
     * Count active vehicles by owner
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.ownerId = :ownerId AND v.isActive = true AND v.deletedAt IS NULL")
    long countActiveVehiclesByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Get total distance for owner's vehicles
     */
    @Query("SELECT COALESCE(SUM(v.totalDistanceKm), 0) FROM Vehicle v WHERE v.ownerId = :ownerId AND v.deletedAt IS NULL")
    Double getTotalDistanceByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Get total CO2 saved for owner's vehicles
     */
    @Query("SELECT COALESCE(SUM(v.totalCo2SavedKg), 0) FROM Vehicle v WHERE v.ownerId = :ownerId AND v.deletedAt IS NULL")
    Double getTotalCo2SavedByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find vehicles with failed sync attempts
     */
    @Query("SELECT v FROM Vehicle v WHERE v.syncStatus = 'FAILED' AND v.isActive = true")
    List<Vehicle> findVehiclesWithFailedSync();

    /**
     * Find vehicles by make and model
     */
    List<Vehicle> findByMakeAndModel(String make, String model);

    /**
     * Search vehicles by partial VIN or registration number
     */
    @Query("SELECT v FROM Vehicle v WHERE (UPPER(v.vin) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
            "OR UPPER(v.registrationNumber) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
            "AND v.deletedAt IS NULL")
    Page<Vehicle> searchVehicles(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find recently synced vehicles
     */
    @Query("SELECT v FROM Vehicle v WHERE v.lastSyncAt >= :since ORDER BY v.lastSyncAt DESC")
    List<Vehicle> findRecentlySyncedVehicles(@Param("since") LocalDateTime since);

    /**
     * Update sync status for a vehicle
     */
    @Query("UPDATE Vehicle v SET v.syncStatus = :status WHERE v.vehicleId = :vehicleId")
    void updateSyncStatus(@Param("vehicleId") UUID vehicleId, @Param("status") Vehicle.SyncStatus status);

    /**
     * Soft delete a vehicle
     */
    @Query("UPDATE Vehicle v SET v.deletedAt = :deletedAt, v.isActive = false WHERE v.vehicleId = :vehicleId")
    void softDelete(@Param("vehicleId") UUID vehicleId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Find vehicles pending verification
     */
    @Query("SELECT v FROM Vehicle v WHERE v.verificationStatus = 'PENDING' ORDER BY v.createdAt ASC")
    List<Vehicle> findPendingVerification();
}
