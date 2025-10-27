package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerationRequest {

    @NotBlank(message = "Report name is required")
    private String reportName;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    private String templateId;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private List<String> metrics;
    private List<String> dimensions;
    private Map<String, String> filters;
    private String groupBy;
    private String sortBy;
    private OutputFormat outputFormat;
    private Boolean includeCharts;
    private Boolean includeSummary;
    private String emailRecipient;
    private Map<String, Object> customParameters;

    public enum ReportType {
        EXECUTIVE_SUMMARY,
        FINANCIAL,
        USER_ANALYTICS,
        MARKETPLACE,
        ENVIRONMENTAL,
        OPERATIONAL,
        CUSTOM
    }

    public enum OutputFormat {
        PDF,
        EXCEL,
        CSV,
        JSON
    }
}
