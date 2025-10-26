package com.carbonmarketplace.carboncreditservice.controller;

import com.carbonmarketplace.carboncreditservice.dto.CarbonCalculationResult;
import com.carbonmarketplace.carboncreditservice.dto.request.CalculateCarbonRequest;
import com.carbonmarketplace.carboncreditservice.service.CarbonCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for carbon credit calculations
 */
@RestController
@RequestMapping("/api/v1/carbon/calculate")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Carbon Calculation", description = "Carbon credit calculation endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CarbonCalculationController {
    
    private final CarbonCalculationService calculationService;
    
    @Operation(summary = "Calculate carbon credits",
            description = "Calculate carbon credits based on trip data")
    @PostMapping
    @PreAuthorize("hasAnyRole('EVOWNER', 'ADMIN')")
    public ResponseEntity<CarbonCalculationResult> calculateCarbon(
            @Valid @RequestBody CalculateCarbonRequest request) {
        
        log.info("Calculating carbon credits for vehicle: {}", request.getVehicleId());
        
        CarbonCalculationResult result = calculationService.calculateFromRequest(request);
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Quick carbon calculation",
            description = "Quick estimation based on distance only")
    @GetMapping("/estimate")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CarbonCalculationResult> estimateCarbon(
            @RequestParam BigDecimal distanceKm) {
        
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        CarbonCalculationResult result = calculationService.quickCalculate(distanceKm);
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Validate calculation accuracy",
            description = "Check if a calculation is within acceptable tolerance")
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('EVOWNER', 'VERIFIER', 'ADMIN')")
    public ResponseEntity<Boolean> validateCalculation(
            @RequestParam BigDecimal calculated,
            @RequestParam BigDecimal expected) {
        
        boolean isAccurate = calculationService.validateAccuracy(calculated, expected);
        
        return ResponseEntity.ok(isAccurate);
    }
}
