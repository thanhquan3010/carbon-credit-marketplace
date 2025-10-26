package com.carbonmarketplace.analyticsservice.controller;

import com.carbonmarketplace.analyticsservice.dto.*;
import com.carbonmarketplace.analyticsservice.service.DashboardService;
import com.carbonmarketplace.analyticsservice.service.MetricsCalculationService;
import com.carbonmarketplace.analyticsservice.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
public class AnalyticsController {
    
    private final DashboardService dashboardService;
    private final MetricsCalculationService metricsCalculationService;
    private final ReportGenerationService reportGenerationService;
    
    @GetMapping("/dashboard/executive")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
    @Operation(summary = "Get executive dashboard data", 
              description = "Returns real-time executive dashboard metrics with 15-minute refresh")
    public ResponseEntity<DashboardResponse> getExecutiveDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Fetching executive dashboard data for period: {} to {}", startDate, endDate);
        DashboardResponse response = dashboardService.getExecutiveDashboard(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/dashboard/realtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
    @Operation(summary = "Get real-time metrics", 
              description = "Returns current real-time platform metrics")
    public ResponseEntity<RealtimeMetricsResponse> getRealtimeMetrics() {
        log.info("Fetching real-time metrics");
        RealtimeMetricsResponse response = dashboardService.getRealtimeMetrics();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Get KPI metrics", 
              description = "Returns KPI metrics for specified period and category")
    public ResponseEntity<KPIResponse> getKPIs(
            @RequestParam @NotNull String category,
            @RequestParam @NotNull String periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Fetching KPIs for category: {}, period: {}", category, periodType);
        KPIResponse response = dashboardService.getKPIs(category, periodType, startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trends/{metricType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Get metric trends", 
              description = "Returns trend analysis for specified metric")
    public ResponseEntity<TrendAnalysisResponse> getTrendAnalysis(
            @PathVariable String metricType,
            @RequestParam @NotNull String periodType,
            @RequestParam(defaultValue = "30") Integer days) {
        
        log.info("Fetching trend analysis for metric: {}, period: {}", metricType, periodType);
        TrendAnalysisResponse response = dashboardService.getTrendAnalysis(metricType, periodType, days);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/{userId}/analytics")
    @PreAuthorize("hasAnyRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get user analytics", 
              description = "Returns analytics for specific user")
    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(
            @PathVariable Long userId,
            @RequestParam(required = false) String periodType) {
        
        log.info("Fetching analytics for user: {}", userId);
        UserAnalyticsResponse response = dashboardService.getUserAnalytics(userId, periodType);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reports/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Generate custom report", 
              description = "Generates custom report based on provided criteria")
    public ResponseEntity<ReportGenerationResponse> generateReport(
            @Valid @RequestBody ReportGenerationRequest request) {
        
        log.info("Generating custom report: {}", request.getReportName());
        ReportGenerationResponse response = reportGenerationService.generateReport(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/reports/{reportId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Download report", 
              description = "Downloads generated report in specified format")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String reportId,
            @RequestParam(defaultValue = "PDF") String format,
            HttpServletResponse response) throws IOException {
        
        log.info("Downloading report: {} in format: {}", reportId, format);
        
        byte[] reportContent = reportGenerationService.getReportContent(reportId, format);
        String filename = String.format("report_%s.%s", reportId, format.toLowerCase());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(format.equalsIgnoreCase("PDF") ? 
            MediaType.APPLICATION_PDF : 
            MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(reportContent);
    }
    
    @GetMapping("/reports/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Get report templates", 
              description = "Returns available report templates")
    public ResponseEntity<List<ReportTemplateDto>> getReportTemplates() {
        log.info("Fetching available report templates");
        List<ReportTemplateDto> templates = reportGenerationService.getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/marketplace/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
    @Operation(summary = "Get marketplace metrics", 
              description = "Returns marketplace performance metrics")
    public ResponseEntity<MarketplaceMetricsResponse> getMarketplaceMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Fetching marketplace metrics for period: {} to {}", startDate, endDate);
        MarketplaceMetricsResponse response = dashboardService.getMarketplaceMetrics(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/environmental/impact")
    @Operation(summary = "Get environmental impact metrics", 
              description = "Returns environmental impact and CO2 offset metrics")
    public ResponseEntity<EnvironmentalImpactResponse> getEnvironmentalImpact(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Fetching environmental impact metrics");
        EnvironmentalImpactResponse response = dashboardService.getEnvironmentalImpact(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually refresh analytics", 
              description = "Triggers manual refresh of analytics and materialized views")
    public ResponseEntity<Map<String, String>> refreshAnalytics() {
        log.info("Manually triggering analytics refresh");
        
        metricsCalculationService.refreshMaterializedViews();
        metricsCalculationService.calculateDailyMetrics();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Analytics refresh initiated",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    @GetMapping("/performance/bottlenecks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get performance bottlenecks", 
              description = "Identifies system performance bottlenecks")
    public ResponseEntity<PerformanceAnalysisResponse> getPerformanceBottlenecks() {
        log.info("Analyzing system performance bottlenecks");
        PerformanceAnalysisResponse response = dashboardService.analyzePerformance();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/forecast/{metricType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
    @Operation(summary = "Get metric forecast", 
              description = "Returns forecasted values for specified metric")
    public ResponseEntity<ForecastResponse> getMetricForecast(
            @PathVariable String metricType,
            @RequestParam(defaultValue = "30") Integer daysAhead) {
        
        log.info("Generating forecast for metric: {} for {} days", metricType, daysAhead);
        ForecastResponse response = dashboardService.generateForecast(metricType, daysAhead);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/cohort/analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'ANALYST')")
    @Operation(summary = "Get cohort analysis", 
              description = "Returns cohort analysis for user retention and behavior")
    public ResponseEntity<CohortAnalysisResponse> getCohortAnalysis(
            @RequestParam String cohortType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "12") Integer periods) {
        
        log.info("Fetching cohort analysis for type: {}", cohortType);
        CohortAnalysisResponse response = dashboardService.getCohortAnalysis(cohortType, startDate, periods);
        return ResponseEntity.ok(response);
    }
}
