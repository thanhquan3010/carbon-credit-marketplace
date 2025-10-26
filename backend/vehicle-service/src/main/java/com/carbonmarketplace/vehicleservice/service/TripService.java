package com.carbonmarketplace.vehicleservice.service;

import com.carbonmarketplace.vehicleservice.dto.request.TripUploadRequest;
import com.carbonmarketplace.vehicleservice.dto.response.TripResponse;
import com.carbonmarketplace.vehicleservice.entity.Trip;
import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import com.carbonmarketplace.vehicleservice.exception.InvalidTripDataException;
import com.carbonmarketplace.vehicleservice.exception.VehicleNotFoundException;
import com.carbonmarketplace.vehicleservice.repository.TripRepository;
import com.carbonmarketplace.vehicleservice.repository.VehicleRepository;
import com.carbonmarketplace.vehicleservice.util.CsvParser;
import com.carbonmarketplace.vehicleservice.validation.TripValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing trip operations
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final TripValidator tripValidator;
    private final CsvParser csvParser;
    private final VehicleService vehicleService;

    /**
     * Get trips by vehicle
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> getTripsByVehicle(UUID vehicleId, UUID userId, Pageable pageable) {
        log.info("Fetching trips for vehicle: {}", vehicleId);

        // Verify vehicle exists and user has access
        Vehicle vehicle = verifyVehicleAccess(vehicleId, userId);

        Page<Trip> trips = tripRepository.findByVehicleId(vehicleId, pageable);
        return trips.map(TripResponse::fromEntity);
    }

    /**
     * Get trips by date range
     */
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByDateRange(UUID vehicleId, LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        log.info("Fetching trips for vehicle: {} from {} to {}", vehicleId, startDate, endDate);

        // Verify vehicle exists and user has access
        verifyVehicleAccess(vehicleId, userId);

        List<Trip> trips = tripRepository.findByVehicleIdAndDateRange(vehicleId, startDate, endDate);
        return trips.stream()
                .map(TripResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Add trip manually
     */
    public TripResponse addTripManually(UUID vehicleId, TripUploadRequest request, UUID userId) {
        log.info("Adding manual trip for vehicle: {}", vehicleId);

        // Verify vehicle exists and user has access
        Vehicle vehicle = verifyVehicleAccess(vehicleId, userId);

        // Validate trip duration
        if (!request.isValidDuration()) {
            throw new InvalidTripDataException("Invalid trip duration. Trip must be at least 2 minutes long");
        }

        // Check for duplicate external trip ID if provided
        if (request.getExternalTripId() != null && 
            tripRepository.existsByExternalTripId(request.getExternalTripId())) {
            throw new InvalidTripDataException("Trip with external ID already exists: " + request.getExternalTripId());
        }

        // Create trip entity
        Trip trip = Trip.builder()
                .vehicle(vehicle)
                .vehicleId(vehicleId)
                .externalTripId(request.getExternalTripId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .startLat(request.getStartLat())
                .startLng(request.getStartLng())
                .endLat(request.getEndLat())
                .endLng(request.getEndLng())
                .startAddress(request.getStartAddress())
                .endAddress(request.getEndAddress())
                .distanceKm(request.getDistanceKm())
                .avgSpeed(request.getAvgSpeed())
                .maxSpeed(request.getMaxSpeed())
                .energyConsumedKwh(request.getEnergyConsumedKwh())
                .dataSource(Trip.DataSource.MANUAL)
                .idleTimeSeconds(request.getIdleTimeSeconds())
                .drivingTimeSeconds(request.getDrivingTimeSeconds())
                .chargingTimeSeconds(request.getChargingTimeSeconds())
                .regenerationKwh(request.getRegenerationKwh())
                .ambientTemperature(request.getAmbientTemperature())
                .batteryLevelStart(request.getBatteryLevelStart())
                .batteryLevelEnd(request.getBatteryLevelEnd())
                .build();

        // Calculate derived fields
        trip.calculateDerivedFields();

        // Validate trip data
        TripValidator.ValidationResult validationResult = tripValidator.validate(trip);
        if (!validationResult.isValid()) {
            throw new InvalidTripDataException("Trip validation failed: " + validationResult.getMessage());
        }

        if (validationResult.hasWarnings()) {
            trip.setHasAnomaly(true);
            trip.setAnomalyDescription(validationResult.getMessage());
            trip.setDataQualityScore(new BigDecimal("0.8"));
        }

        // Check for overlapping trips
        List<Trip> overlappingTrips = tripRepository.findOverlappingTrips(
                vehicleId, 
                request.getStartTime(), 
                request.getEndTime(), 
                UUID.randomUUID() // Dummy ID for new trip
        );
        
        if (!overlappingTrips.isEmpty()) {
            log.warn("Overlapping trips detected for vehicle: {}", vehicleId);
            trip.setHasAnomaly(true);
            trip.setAnomalyDescription("Overlapping with existing trips");
        }

        trip = tripRepository.save(trip);
        log.info("Trip added successfully: {}", trip.getTripId());

        // Update vehicle statistics
        updateVehicleStatistics(vehicle);

        return TripResponse.fromEntity(trip);
    }

    /**
     * Add trips in batch
     */
    public Map<String, Object> addTripsBatch(UUID vehicleId, List<TripUploadRequest> requests, UUID userId) {
        log.info("Adding {} trips in batch for vehicle: {}", requests.size(), vehicleId);

        // Verify vehicle exists and user has access
        Vehicle vehicle = verifyVehicleAccess(vehicleId, userId);

        Map<String, Object> result = new HashMap<>();
        List<TripResponse> successfulTrips = new ArrayList<>();
        List<Map<String, String>> failedTrips = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Get existing external trip IDs to check for duplicates
        List<String> externalIds = requests.stream()
                .map(TripUploadRequest::getExternalTripId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        Set<String> existingIds = new HashSet<>(tripRepository.findExistingExternalTripIds(externalIds));

        for (int i = 0; i < requests.size(); i++) {
            TripUploadRequest request = requests.get(i);
            try {
                // Skip if duplicate external ID
                if (request.getExternalTripId() != null && existingIds.contains(request.getExternalTripId())) {
                    failedTrips.add(Map.of(
                            "index", String.valueOf(i),
                            "error", "Duplicate external ID: " + request.getExternalTripId()
                    ));
                    failureCount++;
                    continue;
                }

                TripResponse trip = addTripManually(vehicleId, request, userId);
                successfulTrips.add(trip);
                successCount++;
                
                if (request.getExternalTripId() != null) {
                    existingIds.add(request.getExternalTripId());
                }
            } catch (Exception e) {
                failedTrips.add(Map.of(
                        "index", String.valueOf(i),
                        "error", e.getMessage()
                ));
                failureCount++;
            }
        }

        result.put("totalProcessed", requests.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successfulTrips", successfulTrips);
        result.put("failedTrips", failedTrips);

        return result;
    }

    /**
     * Upload trips from CSV
     */
    public Map<String, Object> uploadTripsFromCsv(UUID vehicleId, MultipartFile file, UUID userId) {
        log.info("Processing CSV upload for vehicle: {}", vehicleId);

        try {
            List<TripUploadRequest> tripRequests = csvParser.parseTripsCsv(file.getInputStream());
            return addTripsBatch(vehicleId, tripRequests, userId);
        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    /**
     * Get vehicle statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVehicleStatistics(UUID vehicleId, LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        log.info("Calculating statistics for vehicle: {}", vehicleId);

        // Verify vehicle exists and user has access
        verifyVehicleAccess(vehicleId, userId);

        // Default date range if not provided (last 30 days)
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Object[] stats = tripRepository.getVehicleTripStatistics(vehicleId, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (stats != null && stats.length > 0 && stats[0] != null) {
            Object[] row = (Object[]) stats[0];
            statistics.put("tripCount", row[0]);
            statistics.put("totalDistanceKm", row[1]);
            statistics.put("totalCo2SavedKg", row[2]);
            statistics.put("totalEnergyKwh", row[3]);
            statistics.put("avgDistancePerTrip", row[4]);
            statistics.put("avgSpeed", row[5]);
        } else {
            statistics.put("tripCount", 0L);
            statistics.put("totalDistanceKm", BigDecimal.ZERO);
            statistics.put("totalCo2SavedKg", BigDecimal.ZERO);
            statistics.put("totalEnergyKwh", BigDecimal.ZERO);
            statistics.put("avgDistancePerTrip", BigDecimal.ZERO);
            statistics.put("avgSpeed", BigDecimal.ZERO);
        }

        // Get monthly summary
        List<Object[]> monthlySummary = tripRepository.getMonthlyTripSummary(vehicleId, startDate, endDate);
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        for (Object[] row : monthlySummary) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", row[0]);
            monthData.put("tripCount", row[1]);
            monthData.put("totalDistance", row[2]);
            monthData.put("totalCo2Saved", row[3]);
            monthData.put("totalEnergy", row[4]);
            monthlyData.add(monthData);
        }
        
        statistics.put("monthlyBreakdown", monthlyData);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);

        // Get anomaly count
        long anomalyCount = tripRepository.findAnomalousTripsForVehicle(vehicleId).size();
        statistics.put("anomalyCount", anomalyCount);

        return statistics;
    }

    /**
     * Save trips from external sync
     */
    public Map<String, Object> saveTripsFromSync(UUID vehicleId, List<Trip> trips, UUID syncBatchId) {
        log.info("Saving {} synced trips for vehicle: {}", trips.size(), vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        Map<String, Object> result = new HashMap<>();
        int newTrips = 0;
        int duplicates = 0;
        int errors = 0;
        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal totalCo2 = BigDecimal.ZERO;

        // Get existing external IDs
        List<String> externalIds = trips.stream()
                .map(Trip::getExternalTripId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        Set<String> existingIds = new HashSet<>(tripRepository.findExistingExternalTripIds(externalIds));

        for (Trip trip : trips) {
            try {
                // Skip duplicates
                if (trip.getExternalTripId() != null && existingIds.contains(trip.getExternalTripId())) {
                    duplicates++;
                    continue;
                }

                trip.setVehicle(vehicle);
                trip.setVehicleId(vehicleId);
                trip.setSyncBatchId(syncBatchId);
                trip.calculateDerivedFields();

                // Validate trip
                TripValidator.ValidationResult validationResult = tripValidator.validate(trip);
                if (!validationResult.isValid()) {
                    log.warn("Invalid trip data from sync: {}", validationResult.getMessage());
                    errors++;
                    continue;
                }

                if (validationResult.hasWarnings()) {
                    trip.setHasAnomaly(true);
                    trip.setAnomalyDescription(validationResult.getMessage());
                    trip.setDataQualityScore(new BigDecimal("0.8"));
                }

                tripRepository.save(trip);
                newTrips++;
                totalDistance = totalDistance.add(trip.getDistanceKm());
                totalCo2 = totalCo2.add(trip.getCo2SavedKg());

            } catch (Exception e) {
                log.error("Error saving trip from sync", e);
                errors++;
            }
        }

        // Update vehicle statistics
        if (newTrips > 0) {
            vehicleService.markVehicleAsSynced(vehicleId, newTrips, totalDistance.doubleValue(), totalCo2.doubleValue());
        }

        result.put("totalProcessed", trips.size());
        result.put("newTrips", newTrips);
        result.put("duplicates", duplicates);
        result.put("errors", errors);
        result.put("totalDistance", totalDistance);
        result.put("totalCo2Saved", totalCo2);

        return result;
    }

    /**
     * Get recent trips
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> getRecentTrips(UUID vehicleId, UUID userId, int limit) {
        log.info("Fetching {} recent trips for vehicle: {}", limit, vehicleId);

        verifyVehicleAccess(vehicleId, userId);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startTime"));
        Page<Trip> trips = tripRepository.findRecentTripsByVehicleId(vehicleId, pageable);
        
        return trips.map(TripResponse::fromEntity);
    }

    /**
     * Verify vehicle access
     */
    private Vehicle verifyVehicleAccess(UUID vehicleId, UUID userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        // Check ownership unless admin
        if (!vehicle.getOwnerId().equals(userId) && !isAdmin(userId)) {
            throw new SecurityException("Unauthorized access to vehicle");
        }

        return vehicle;
    }

    /**
     * Update vehicle statistics
     */
    private void updateVehicleStatistics(Vehicle vehicle) {
        BigDecimal totalDistance = tripRepository.getTotalDistanceByVehicleId(vehicle.getVehicleId());
        BigDecimal totalCo2 = tripRepository.getTotalCo2SavedByVehicleId(vehicle.getVehicleId());
        long tripCount = tripRepository.countByVehicleId(vehicle.getVehicleId());

        vehicle.setTotalDistanceKm(totalDistance.doubleValue());
        vehicle.setTotalCo2SavedKg(totalCo2.doubleValue());
        vehicle.setTotalTrips((int) tripCount);

        vehicleRepository.save(vehicle);
    }

    /**
     * Check if user is admin (placeholder - should integrate with auth service)
     */
    private boolean isAdmin(UUID userId) {
        // TODO: Integrate with user service to check admin role
        return false;
    }
}

