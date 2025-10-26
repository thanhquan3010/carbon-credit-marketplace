package com.carbonmarketplace.vehicleservice.service;

import com.carbonmarketplace.vehicleservice.client.VinFastApiClient;
import com.carbonmarketplace.vehicleservice.entity.Trip;
import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import com.carbonmarketplace.vehicleservice.exception.VehicleNotFoundException;
import com.carbonmarketplace.vehicleservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for synchronizing trip data from external sources
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TripSyncService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final TripService tripService;
    private final VinFastApiClient vinFastApiClient;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Sync trips for a specific vehicle
     */
    public Map<String, Object> syncVehicleTrips(UUID vehicleId, UUID userId) {
        log.info("Starting trip sync for vehicle: {}", vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        // Check ownership unless admin
        if (!vehicle.getOwnerId().equals(userId) && !isAdmin(userId)) {
            throw new SecurityException("Unauthorized access to vehicle");
        }

        // Check if sync is already in progress
        if (vehicle.getSyncStatus() == Vehicle.SyncStatus.IN_PROGRESS) {
            log.warn("Sync already in progress for vehicle: {}", vehicleId);
            return Map.of(
                    "status", "IN_PROGRESS",
                    "message", "Synchronization is already in progress for this vehicle"
            );
        }

        // Mark sync as in progress
        vehicleService.markSyncInProgress(vehicleId);

        Map<String, Object> result;
        try {
            result = performSync(vehicle);
            log.info("Sync completed for vehicle: {}", vehicleId);
        } catch (Exception e) {
            log.error("Sync failed for vehicle: {}", vehicleId, e);
            vehicleService.markSyncFailed(vehicleId);
            result = Map.of(
                    "status", "FAILED",
                    "error", e.getMessage()
            );
        }

        return result;
    }

    /**
     * Sync all vehicles that need synchronization
     */
    public Map<String, Object> syncAllVehicles() {
        log.info("Starting batch sync for all vehicles");

        List<Vehicle> vehiclesNeedingSync = vehicleService.getVehiclesNeedingSync();
        log.info("Found {} vehicles needing sync", vehiclesNeedingSync.size());

        Map<String, Object> result = new HashMap<>();
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (Vehicle vehicle : vehiclesNeedingSync) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            vehicleService.markSyncInProgress(vehicle.getVehicleId());
                            return performSync(vehicle);
                        } catch (Exception e) {
                            log.error("Sync failed for vehicle: {}", vehicle.getVehicleId(), e);
                            vehicleService.markSyncFailed(vehicle.getVehicleId());
                            return Map.of(
                                    "vehicleId", vehicle.getVehicleId(),
                                    "status", "FAILED",
                                    "error", e.getMessage()
                            );
                        }
                    }, executorService);
            
            futures.add(future);
        }

        // Wait for all syncs to complete
        List<Map<String, Object>> syncResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long successCount = syncResults.stream()
                .filter(r -> "SUCCESS".equals(r.get("status")))
                .count();

        result.put("totalVehicles", vehiclesNeedingSync.size());
        result.put("successCount", successCount);
        result.put("failureCount", vehiclesNeedingSync.size() - successCount);
        result.put("results", syncResults);

        return result;
    }

    /**
     * Retry failed syncs
     */
    public Map<String, Object> retryFailedSyncs() {
        log.info("Retrying failed syncs");

        List<Vehicle> failedVehicles = vehicleRepository.findVehiclesWithFailedSync();
        log.info("Found {} vehicles with failed sync", failedVehicles.size());

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> retryResults = new ArrayList<>();

        for (Vehicle vehicle : failedVehicles) {
            try {
                vehicleService.markSyncInProgress(vehicle.getVehicleId());
                Map<String, Object> syncResult = performSync(vehicle);
                retryResults.add(syncResult);
            } catch (Exception e) {
                log.error("Retry sync failed for vehicle: {}", vehicle.getVehicleId(), e);
                vehicleService.markSyncFailed(vehicle.getVehicleId());
                retryResults.add(Map.of(
                        "vehicleId", vehicle.getVehicleId(),
                        "status", "FAILED",
                        "error", e.getMessage()
                ));
            }
        }

        long successCount = retryResults.stream()
                .filter(r -> "SUCCESS".equals(r.get("status")))
                .count();

        result.put("totalRetried", failedVehicles.size());
        result.put("successCount", successCount);
        result.put("failureCount", failedVehicles.size() - successCount);
        result.put("results", retryResults);

        return result;
    }

    /**
     * Perform actual sync for a vehicle
     */
    private Map<String, Object> performSync(Vehicle vehicle) {
        log.info("Performing sync for vehicle: {} (VIN: {})", vehicle.getVehicleId(), vehicle.getVin());

        Map<String, Object> result = new HashMap<>();
        result.put("vehicleId", vehicle.getVehicleId());
        result.put("vin", vehicle.getVin());

        // Determine sync method based on data source
        List<Trip> trips;
        switch (vehicle.getDataSource()) {
            case API:
                trips = syncFromApi(vehicle);
                break;
            case OBD:
                trips = syncFromObd(vehicle);
                break;
            default:
                log.info("Manual data source, skipping sync for vehicle: {}", vehicle.getVehicleId());
                result.put("status", "SKIPPED");
                result.put("message", "Manual data source - no automatic sync");
                return result;
        }

        if (trips.isEmpty()) {
            log.info("No new trips found for vehicle: {}", vehicle.getVehicleId());
            vehicleService.markVehicleAsSynced(vehicle.getVehicleId(), 0, 0, 0);
            result.put("status", "SUCCESS");
            result.put("newTrips", 0);
            result.put("message", "No new trips found");
            return result;
        }

        // Save trips
        UUID syncBatchId = UUID.randomUUID();
        Map<String, Object> saveResult = tripService.saveTripsFromSync(vehicle.getVehicleId(), trips, syncBatchId);

        result.put("status", "SUCCESS");
        result.put("syncBatchId", syncBatchId);
        result.put("syncTime", LocalDateTime.now());
        result.putAll(saveResult);

        return result;
    }

    /**
     * Sync trips from API (VinFast or other APIs)
     */
    private List<Trip> syncFromApi(Vehicle vehicle) {
        log.info("Syncing from API for vehicle: {}", vehicle.getVin());

        // Get last sync time or default to 7 days ago
        LocalDateTime fromDate = vehicle.getLastSyncAt() != null ? 
                vehicle.getLastSyncAt() : 
                LocalDateTime.now().minusDays(7);
        
        LocalDateTime toDate = LocalDateTime.now();

        try {
            // Call VinFast API to get trips
            List<VinFastApiClient.VinFastTrip> vinFastTrips = vinFastApiClient.getVehicleTrips(
                    vehicle.getVin(), 
                    fromDate, 
                    toDate
            );

            // Convert VinFast trips to our Trip entities
            return vinFastTrips.stream()
                    .map(vt -> convertVinFastTrip(vt, vehicle))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to sync from VinFast API", e);
            throw new RuntimeException("API sync failed: " + e.getMessage());
        }
    }

    /**
     * Sync trips from OBD device
     */
    private List<Trip> syncFromObd(Vehicle vehicle) {
        log.info("Syncing from OBD for vehicle: {}", vehicle.getVin());
        
        // TODO: Implement OBD sync logic
        // This would involve connecting to OBD cloud service or device
        // For now, return empty list
        log.warn("OBD sync not yet implemented for vehicle: {}", vehicle.getVehicleId());
        return new ArrayList<>();
    }

    /**
     * Convert VinFast trip to our Trip entity
     */
    private Trip convertVinFastTrip(VinFastApiClient.VinFastTrip vinFastTrip, Vehicle vehicle) {
        return Trip.builder()
                .externalTripId(vinFastTrip.getTripId())
                .startTime(vinFastTrip.getStartTime())
                .endTime(vinFastTrip.getEndTime())
                .startLat(vinFastTrip.getStartLat())
                .startLng(vinFastTrip.getStartLng())
                .endLat(vinFastTrip.getEndLat())
                .endLng(vinFastTrip.getEndLng())
                .startAddress(vinFastTrip.getStartAddress())
                .endAddress(vinFastTrip.getEndAddress())
                .distanceKm(vinFastTrip.getDistanceKm())
                .avgSpeed(vinFastTrip.getAvgSpeed())
                .maxSpeed(vinFastTrip.getMaxSpeed())
                .energyConsumedKwh(vinFastTrip.getEnergyConsumed())
                .dataSource(Trip.DataSource.API)
                .dataQualityScore(vinFastTrip.getDataQuality())
                .rawData(vinFastTrip.getRawData())
                .build();
    }

    /**
     * Check if user is admin (placeholder - should integrate with auth service)
     */
    private boolean isAdmin(UUID userId) {
        // TODO: Integrate with user service to check admin role
        return false;
    }

    /**
     * Cleanup executor service on shutdown
     */
    public void shutdown() {
        executorService.shutdown();
    }
}

