package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerationResponse {
    
    private String reportId;
    private String reportName;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long processingTimeMs;
    private String downloadUrl;
    private String errorMessage;
    private ReportMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String format;
        private Long sizeBytes;
        private Integer pageCount;
        private Integer recordCount;
        private LocalDateTime expiresAt;
    }
}
