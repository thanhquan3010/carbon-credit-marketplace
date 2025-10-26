package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTemplateDto {
    
    private String templateId;
    private String templateName;
    private String description;
    private String category;
    private List<String> availableMetrics;
    private List<String> availableDimensions;
    private Map<String, String> defaultParameters;
    private List<String> supportedFormats;
    private Boolean customizable;
    private Integer estimatedGenerationTime;
}
