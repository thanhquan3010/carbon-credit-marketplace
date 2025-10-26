package com.carbonmarketplace.vehicleservice.service;

import com.carbonmarketplace.vehicleservice.dto.request.UpdateVehicleRequest;
import com.carbonmarketplace.vehicleservice.dto.request.VehicleRegistrationRequest;
import com.carbonmarketplace.vehicleservice.dto.response.VehicleResponse;
import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import com.carbonmarketplace.vehicleservice.exception.DuplicateVinException;
import com.carbonmarketplace.vehicleservice.exception.VehicleNotFoundException;
import com.carbonmarketplace.vehicleservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing vehicle operations
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * Register a new vehicle
     */
    public VehicleResponse registerVehicle(VehicleRegistrationRequest request, UUID userId) {
        log.info("Registering vehicle with VIN: {} for user: {}", request.getVin(), userId);

        // Check if VIN already exists
        if (vehicleRepository.existsByVin(request.getVin())) {
            throw new DuplicateVinException("Vehicle with VIN " + request.getVin() + " already exists");
        }

        // Validate VIN format
        if (!isValidVin(request.getVin())) {
            throw new IllegalArgumentException("Invalid VIN format");
        }

        // Create new vehicle entity
        Vehicle vehicle = Vehicle.builder()
                .ownerId(userId)
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .vin(request.getVin().toUpperCase())
                .registrationNumber(request.getRegistrationNumber())
                .color(request.getColor())
                .dataSource(request.getDataSource())
                .dataSourceConfig(request.getDataSourceConfig())
                .syncFrequency(request.getSyncFrequency() != null ? request.getSyncFrequency() : Vehicle.SyncFrequency.DAILY)
                .verificationStatus(Vehicle.VerificationStatus.PENDING)
                .syncStatus(Vehicle.SyncStatus.SUCCESS)
                .notes(request.getNotes())
                .isActive(true)
                .totalDistanceKm(0.0)
                .totalTrips(0)
                .totalCo2SavedKg(0.0)
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle registered successfully with ID: {}", vehicle.getVehicleId());

        return VehicleResponse.fromEntity(vehicle);
    }

    /**
     * Get vehicles by owner
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByOwner(UUID userId) {
        log.info("Fetching vehicles for user: {}", userId);
        List<Vehicle> vehicles = vehicleRepository.findActiveVehiclesByOwnerId(userId);
        
        return vehicles.stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get vehicle by ID
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID vehicleId, UUID userId) {
        log.info("Fetching vehicle: {} for user: {}", vehicleId, userId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        // Check ownership unless admin
        if (!vehicle.getOwnerId().equals(userId) && !isAdmin(userId)) {
            throw new SecurityException("Unauthorized access to vehicle");
        }

        return VehicleResponse.fromEntity(vehicle);
    }

    /**
     * Update vehicle information
     */
    public VehicleResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request, UUID userId) {
        log.info("Updating vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        // Check ownership
        if (!vehicle.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized access to vehicle");
        }

        // Check VIN uniqueness if changed
        if (request.getVin() != null && !request.getVin().equals(vehicle.getVin())) {
            if (vehicleRepository.existsByVinAndNotVehicleId(request.getVin(), vehicleId)) {
                throw new DuplicateVinException("VIN already exists: " + request.getVin());
            }
            vehicle.setVin(request.getVin().toUpperCase());
        }

        // Update fields if provided
        if (request.getMake() != null) {
            vehicle.setMake(request.getMake());
        }
        if (request.getModel() != null) {
            vehicle.setModel(request.getModel());
        }
        if (request.getYear() != null) {
            vehicle.setYear(request.getYear());
        }
        if (request.getRegistrationNumber() != null) {
            vehicle.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getColor() != null) {
            vehicle.setColor(request.getColor());
        }
        if (request.getDataSource() != null) {
            vehicle.setDataSource(request.getDataSource());
        }
        if (request.getDataSourceConfig() != null) {
            vehicle.setDataSourceConfig(request.getDataSourceConfig());
        }
        if (request.getSyncFrequency() != null) {
            vehicle.setSyncFrequency(request.getSyncFrequency());
            vehicle.calculateNextSyncTime();
        }
        if (request.getIsActive() != null) {
            vehicle.setIsActive(request.getIsActive());
        }
        if (request.getNotes() != null) {
            vehicle.setNotes(request.getNotes());
        }

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully: {}", vehicleId);

        return VehicleResponse.fromEntity(vehicle);
    }

    /**
     * Delete vehicle (soft delete)
     */
    public void deleteVehicle(UUID vehicleId, UUID userId) {
        log.info("Deleting vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        // Check ownership
        if (!vehicle.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized access to vehicle");
        }

        vehicle.setDeletedAt(LocalDateTime.now());
        vehicle.setIsActive(false);
        
        vehicleRepository.save(vehicle);
        log.info("Vehicle soft deleted: {}", vehicleId);
    }

    /**
     * Verify vehicle (Admin only)
     */
    public VehicleResponse verifyVehicle(UUID vehicleId, boolean approved, String notes, UUID adminId) {
        log.info("Admin {} verifying vehicle: {}, approved: {}", adminId, vehicleId, approved);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        vehicle.setVerificationStatus(approved ? 
                Vehicle.VerificationStatus.VERIFIED : 
                Vehicle.VerificationStatus.REJECTED);
        vehicle.setVerifiedAt(LocalDateTime.now());
        vehicle.setVerifiedBy(adminId);
        
        if (notes != null) {
            String existingNotes = vehicle.getNotes();
            vehicle.setNotes(existingNotes != null ? 
                    existingNotes + "\nVerification: " + notes : 
                    "Verification: " + notes);
        }

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle verification updated: {}", vehicleId);

        return VehicleResponse.fromEntity(vehicle);
    }

    /**
     * Get vehicles pending verification
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getPendingVerificationVehicles() {
        log.info("Fetching vehicles pending verification");
        
        List<Vehicle> vehicles = vehicleRepository.findPendingVerification();
        return vehicles.stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search vehicles
     */
    @Transactional(readOnly = true)
    public Page<VehicleResponse> searchVehicles(String searchTerm, Pageable pageable) {
        log.info("Searching vehicles with term: {}", searchTerm);
        
        Page<Vehicle> vehicles = vehicleRepository.searchVehicles(searchTerm, pageable);
        return vehicles.map(VehicleResponse::fromEntity);
    }

    /**
     * Get vehicles with failed sync
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesWithFailedSync() {
        log.info("Fetching vehicles with failed sync");
        
        List<Vehicle> vehicles = vehicleRepository.findVehiclesWithFailedSync();
        return vehicles.stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mark vehicle as synced
     */
    public void markVehicleAsSynced(UUID vehicleId, int tripCount, double totalDistance, double totalCo2) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        vehicle.markAsSynced();
        vehicle.setTotalTrips(vehicle.getTotalTrips() + tripCount);
        vehicle.setTotalDistanceKm(vehicle.getTotalDistanceKm() + totalDistance);
        vehicle.setTotalCo2SavedKg(vehicle.getTotalCo2SavedKg() + totalCo2);
        
        vehicleRepository.save(vehicle);
    }

    /**
     * Mark sync as failed
     */
    public void markSyncFailed(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        vehicle.markSyncFailed();
        vehicleRepository.save(vehicle);
    }

    /**
     * Mark sync in progress
     */
    public void markSyncInProgress(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found: " + vehicleId));

        vehicle.markSyncInProgress();
        vehicleRepository.save(vehicle);
    }

    /**
     * Get vehicles needing sync
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesNeedingSync() {
        return vehicleRepository.findVehiclesNeedingSync(LocalDateTime.now());
    }

    /**
     * Validate VIN format
     */
    private boolean isValidVin(String vin) {
        if (vin == null || vin.length() != 17) {
            return false;
        }
        // VIN should not contain I, O, or Q
        return vin.matches("^[A-HJ-NPR-Z0-9]{17}$");
    }

    /**
     * Check if user is admin (placeholder - should integrate with auth service)
     */
    private boolean isAdmin(UUID userId) {
        // TODO: Integrate with user service to check admin role
        return false;
    }
}
