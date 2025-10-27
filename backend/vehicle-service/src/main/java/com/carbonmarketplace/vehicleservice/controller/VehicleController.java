package com.carbonmarketplace.vehicleservice.controller;

import com.carbonmarketplace.vehicleservice.dto.request.TripUploadRequest;
import com.carbonmarketplace.vehicleservice.dto.request.UpdateVehicleRequest;
import com.carbonmarketplace.vehicleservice.dto.request.VehicleRegistrationRequest;
import com.carbonmarketplace.vehicleservice.dto.response.ApiResponse;
import com.carbonmarketplace.vehicleservice.dto.response.TripResponse;
import com.carbonmarketplace.vehicleservice.dto.response.VehicleResponse;
import com.carbonmarketplace.vehicleservice.service.TripService;
import com.carbonmarketplace.vehicleservice.service.TripSyncService;
import com.carbonmarketplace.vehicleservice.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vehicle management
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vehicle Management", description = "APIs for managing vehicles and trip data")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;
    private final TripService tripService;
    private final TripSyncService tripSyncService;

    /**
     * Register a new vehicle
     */
    @PostMapping
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Register a new vehicle", description = "Register a new electric vehicle for the authenticated user")
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(
            @Valid @RequestBody VehicleRegistrationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Registering new vehicle for user: {}", userId);
        VehicleResponse response = vehicleService.registerVehicle(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle registered successfully", response));
    }

    /**
     * Get all vehicles for the authenticated user
     */
    @GetMapping("/my-vehicles")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Get my vehicles", description = "Get all vehicles registered by the authenticated user")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Fetching vehicles for user: {}", userId);
        List<VehicleResponse> vehicles = vehicleService.getVehiclesByOwner(userId);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    /**
     * Get vehicle by ID
     */
    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasRole('EV_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get vehicle by ID", description = "Get detailed information about a specific vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(
            @PathVariable UUID vehicleId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Fetching vehicle: {} for user: {}", vehicleId, userId);
        VehicleResponse vehicle = vehicleService.getVehicleById(vehicleId, userId);
        return ResponseEntity.ok(ApiResponse.success(vehicle));
    }

    /**
     * Update vehicle information
     */
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Update vehicle", description = "Update vehicle information")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Updating vehicle: {} for user: {}", vehicleId, userId);
        VehicleResponse response = vehicleService.updateVehicle(vehicleId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", response));
    }

    /**
     * Delete a vehicle (soft delete)
     */
    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Delete vehicle", description = "Soft delete a vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable UUID vehicleId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Deleting vehicle: {} for user: {}", vehicleId, userId);
        vehicleService.deleteVehicle(vehicleId, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully", null));
    }

    /**
     * Get trips for a vehicle
     */
    @GetMapping("/{vehicleId}/trips")
    @PreAuthorize("hasRole('EV_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get vehicle trips", description = "Get all trips for a specific vehicle")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getVehicleTrips(
            @PathVariable UUID vehicleId,
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        
        log.info("Fetching trips for vehicle: {}", vehicleId);
        Page<TripResponse> trips = tripService.getTripsByVehicle(vehicleId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    /**
     * Get trips within date range
     */
    @GetMapping("/{vehicleId}/trips/date-range")
    @PreAuthorize("hasRole('EV_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get trips by date range", description = "Get trips within a specific date range")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByDateRange(
            @PathVariable UUID vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Fetching trips for vehicle: {} from {} to {}", vehicleId, startDate, endDate);
        List<TripResponse> trips = tripService.getTripsByDateRange(vehicleId, startDate, endDate, userId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    /**
     * Upload trips via CSV file
     */
    @PostMapping(value = "/{vehicleId}/trips/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Upload trips CSV", description = "Upload trip data via CSV file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadTripsCSV(
            @PathVariable UUID vehicleId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Uploading CSV trips for vehicle: {}", vehicleId);
        
        if (file.isEmpty()) {
            @SuppressWarnings("unchecked")
            ApiResponse<Map<String, Object>> errorResponse = (ApiResponse<Map<String, Object>>) (ApiResponse<?>) ApiResponse.error("File is empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (!file.getOriginalFilename().endsWith(".csv")) {
            @SuppressWarnings("unchecked")
            ApiResponse<Map<String, Object>> errorResponse = (ApiResponse<Map<String, Object>>) (ApiResponse<?>) ApiResponse.error("File must be a CSV");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Map<String, Object> result = tripService.uploadTripsFromCsv(vehicleId, file, userId);
        return ResponseEntity.ok(ApiResponse.success("CSV uploaded successfully", result));
    }

    /**
     * Upload single trip manually
     */
    @PostMapping("/{vehicleId}/trips")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Add trip manually", description = "Manually add a single trip")
    public ResponseEntity<ApiResponse<TripResponse>> addTripManually(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody TripUploadRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Adding manual trip for vehicle: {}", vehicleId);
        TripResponse trip = tripService.addTripManually(vehicleId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip added successfully", trip));
    }

    /**
     * Upload multiple trips manually
     */
    @PostMapping("/{vehicleId}/trips/batch")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Add trips in batch", description = "Manually add multiple trips")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addTripsBatch(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody List<TripUploadRequest> requests,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Adding {} trips in batch for vehicle: {}", requests.size(), vehicleId);
        Map<String, Object> result = tripService.addTripsBatch(vehicleId, requests, userId);
        return ResponseEntity.ok(ApiResponse.success("Batch upload completed", result));
    }

    /**
     * Sync trips from external API
     */
    @PostMapping("/{vehicleId}/sync")
    @PreAuthorize("hasRole('EV_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Sync vehicle trips", description = "Manually trigger trip synchronization from external API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncVehicleTrips(
            @PathVariable UUID vehicleId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Triggering sync for vehicle: {}", vehicleId);
        Map<String, Object> result = tripSyncService.syncVehicleTrips(vehicleId, userId);
        return ResponseEntity.ok(ApiResponse.success("Sync initiated", result));
    }

    /**
     * Get vehicle statistics
     */
    @GetMapping("/{vehicleId}/statistics")
    @PreAuthorize("hasRole('EV_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get vehicle statistics", description = "Get aggregated statistics for a vehicle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleStatistics(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Fetching statistics for vehicle: {}", vehicleId);
        Map<String, Object> stats = tripService.getVehicleStatistics(vehicleId, startDate, endDate, userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Verify vehicle ownership (Admin only)
     */
    @PostMapping("/{vehicleId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify vehicle", description = "Admin endpoint to verify vehicle ownership")
    public ResponseEntity<ApiResponse<VehicleResponse>> verifyVehicle(
            @PathVariable UUID vehicleId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes,
            @RequestHeader("X-User-Id") UUID adminId) {
        
        log.info("Admin {} verifying vehicle: {}, approved: {}", adminId, vehicleId, approved);
        VehicleResponse vehicle = vehicleService.verifyVehicle(vehicleId, approved, notes, adminId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle verification updated", vehicle));
    }

    /**
     * Get vehicles pending verification (Admin only)
     */
    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending vehicles", description = "Get all vehicles pending verification")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getPendingVehicles() {
        
        log.info("Fetching vehicles pending verification");
        List<VehicleResponse> vehicles = vehicleService.getPendingVerificationVehicles();
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    /**
     * Search vehicles (Admin only)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search vehicles", description = "Search vehicles by VIN or registration number")
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> searchVehicles(
            @RequestParam String searchTerm,
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        
        log.info("Searching vehicles with term: {}", searchTerm);
        Page<VehicleResponse> vehicles = vehicleService.searchVehicles(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    /**
     * Get vehicles with failed sync (Admin only)
     */
    @GetMapping("/failed-sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get failed sync vehicles", description = "Get all vehicles with failed synchronization")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getFailedSyncVehicles() {
        
        log.info("Fetching vehicles with failed sync");
        List<VehicleResponse> vehicles = vehicleService.getVehiclesWithFailedSync();
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    /**
     * Retry sync for failed vehicles (Admin only)
     */
    @PostMapping("/retry-failed-sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retry failed syncs", description = "Retry synchronization for all failed vehicles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryFailedSyncs() {
        
        log.info("Retrying failed syncs");
        Map<String, Object> result = tripSyncService.retryFailedSyncs();
        return ResponseEntity.ok(ApiResponse.success("Retry initiated", result));
    }
}

