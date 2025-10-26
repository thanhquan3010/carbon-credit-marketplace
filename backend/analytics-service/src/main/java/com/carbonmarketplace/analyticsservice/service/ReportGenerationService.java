package com.carbonmarketplace.analyticsservice.service;

import com.carbonmarketplace.analyticsservice.dto.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {
    
    private final MetricsCalculationService metricsCalculationService;
    private final Map<String, ReportGenerationResponse> reportCache = new ConcurrentHashMap<>();
    private final Map<String, byte[]> reportContentCache = new ConcurrentHashMap<>();
    
    @Async
    public ReportGenerationResponse generateReport(ReportGenerationRequest request) {
        log.info("Generating report: {}", request.getReportName());
        
        String reportId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        
        ReportGenerationResponse response = ReportGenerationResponse.builder()
                .reportId(reportId)
                .reportName(request.getReportName())
                .status("PROCESSING")
                .createdAt(startTime)
                .build();
        
        reportCache.put(reportId, response);
        
        try {
            byte[] reportContent = generateReportContent(request);
            reportContentCache.put(reportId, reportContent);
            
            LocalDateTime endTime = LocalDateTime.now();
            long processingTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            response.setStatus("COMPLETED");
            response.setCompletedAt(endTime);
            response.setProcessingTimeMs(processingTime);
            response.setDownloadUrl("/api/v1/analytics/reports/" + reportId + "/download");
            response.setMetadata(ReportGenerationResponse.ReportMetadata.builder()
                    .format(request.getOutputFormat().toString())
                    .sizeBytes((long) reportContent.length)
                    .pageCount(calculatePageCount(reportContent, request.getOutputFormat()))
                    .recordCount(calculateRecordCount(request))
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());
            
            log.info("Report generated successfully: {} in {} ms", reportId, processingTime);
        } catch (Exception e) {
            log.error("Failed to generate report: {}", reportId, e);
            response.setStatus("FAILED");
            response.setErrorMessage(e.getMessage());
        }
        
        return response;
    }
    
    public byte[] getReportContent(String reportId, String format) throws IOException {
        if (!reportContentCache.containsKey(reportId)) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        
        byte[] content = reportContentCache.get(reportId);
        
        // Convert format if needed
        if (!"PDF".equalsIgnoreCase(format)) {
            content = convertReportFormat(content, format);
        }
        
        return content;
    }
    
    public List<ReportTemplateDto> getAvailableTemplates() {
        List<ReportTemplateDto> templates = new ArrayList<>();
        
        templates.add(ReportTemplateDto.builder()
                .templateId("exec-summary")
                .templateName("Executive Summary")
                .description("High-level overview of platform performance")
                .category("EXECUTIVE")
                .availableMetrics(Arrays.asList("revenue", "users", "transactions", "co2"))
                .availableDimensions(Arrays.asList("time", "region", "user_type"))
                .defaultParameters(Map.of("period", "monthly", "includeCharts", "true"))
                .supportedFormats(Arrays.asList("PDF", "EXCEL"))
                .customizable(true)
                .estimatedGenerationTime(30)
                .build());
        
        templates.add(ReportTemplateDto.builder()
                .templateId("financial-report")
                .templateName("Financial Report")
                .description("Detailed financial metrics and analysis")
                .category("FINANCIAL")
                .availableMetrics(Arrays.asList("gmv", "revenue", "fees", "refunds"))
                .availableDimensions(Arrays.asList("time", "category", "payment_method"))
                .defaultParameters(Map.of("period", "quarterly", "includeProjections", "true"))
                .supportedFormats(Arrays.asList("PDF", "EXCEL", "CSV"))
                .customizable(true)
                .estimatedGenerationTime(45)
                .build());
        
        templates.add(ReportTemplateDto.builder()
                .templateId("user-analytics")
                .templateName("User Analytics Report")
                .description("User behavior and engagement analysis")
                .category("USER")
                .availableMetrics(Arrays.asList("active_users", "retention", "churn", "ltv"))
                .availableDimensions(Arrays.asList("cohort", "segment", "acquisition_channel"))
                .defaultParameters(Map.of("period", "weekly", "includeCohortAnalysis", "true"))
                .supportedFormats(Arrays.asList("PDF", "EXCEL"))
                .customizable(true)
                .estimatedGenerationTime(60)
                .build());
        
        templates.add(ReportTemplateDto.builder()
                .templateId("environmental-impact")
                .templateName("Environmental Impact Report")
                .description("CO2 offset and environmental metrics")
                .category("ENVIRONMENTAL")
                .availableMetrics(Arrays.asList("co2_offset", "credits_issued", "distance_tracked"))
                .availableDimensions(Arrays.asList("time", "vehicle_type", "region"))
                .defaultParameters(Map.of("period", "yearly", "includeEquivalents", "true"))
                .supportedFormats(Arrays.asList("PDF"))
                .customizable(false)
                .estimatedGenerationTime(20)
                .build());
        
        return templates;
    }
    
    private byte[] generateReportContent(ReportGenerationRequest request) throws IOException {
        switch (request.getOutputFormat()) {
            case PDF:
                return generatePDFReport(request);
            case EXCEL:
                return generateExcelReport(request);
            case CSV:
                return generateCSVReport(request);
            case JSON:
                return generateJSONReport(request);
            default:
                throw new UnsupportedOperationException("Format not supported: " + request.getOutputFormat());
        }
    }
    
    private byte[] generatePDFReport(ReportGenerationRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        try {
            // Add header
            document.add(new Paragraph(request.getReportName())
                    .setFontSize(20)
                    .setBold());
            
            document.add(new Paragraph("Generated: " + LocalDateTime.now())
                    .setFontSize(10));
            
            document.add(new Paragraph("Period: " + request.getStartDate() + " to " + request.getEndDate())
                    .setFontSize(10));
            
            // Add summary section
            if (request.getIncludeSummary()) {
                addSummarySection(document, request);
            }
            
            // Add metrics data
            addMetricsSection(document, request);
            
            // Add charts if requested
            if (request.getIncludeCharts()) {
                addChartsSection(document, request);
            }
            
            document.close();
        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new IOException("Failed to generate PDF report", e);
        }
        
        return baos.toByteArray();
    }
    
    private byte[] generateExcelReport(ReportGenerationRequest request) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            addSummaryToExcel(summarySheet, request);
            
            // Create metrics sheets
            for (String metric : request.getMetrics()) {
                Sheet metricSheet = workbook.createSheet(metric);
                addMetricDataToExcel(metricSheet, metric, request);
            }
            
            // Auto-size columns
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int col = 0; col < 10; col++) {
                    sheet.autoSizeColumn(col);
                }
            }
            
            workbook.write(baos);
        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new IOException("Failed to generate Excel report", e);
        } finally {
            workbook.close();
        }
        
        return baos.toByteArray();
    }
    
    private byte[] generateCSVReport(ReportGenerationRequest request) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        // Add headers
        csv.append("Date,Metric,Value,Category\n");
        
        // Add data rows
        Map<String, Object> reportData = metricsCalculationService.generateKPIReports();
        for (Map.Entry<String, Object> entry : reportData.entrySet()) {
            csv.append(LocalDateTime.now()).append(",")
               .append(entry.getKey()).append(",")
               .append(entry.getValue()).append(",")
               .append(request.getReportType()).append("\n");
        }
        
        return csv.toString().getBytes();
    }
    
    private byte[] generateJSONReport(ReportGenerationRequest request) throws IOException {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportName", request.getReportName());
        reportData.put("reportType", request.getReportType());
        reportData.put("startDate", request.getStartDate());
        reportData.put("endDate", request.getEndDate());
        reportData.put("generatedAt", LocalDateTime.now());
        reportData.put("data", metricsCalculationService.generateKPIReports());
        
        // Convert to JSON using simple string builder (in production, use Jackson)
        return reportData.toString().getBytes();
    }
    
    private void addSummarySection(Document document, ReportGenerationRequest request) {
        document.add(new Paragraph("\nExecutive Summary")
                .setFontSize(16)
                .setBold());
        
        Map<String, Object> kpiReports = metricsCalculationService.generateKPIReports();
        
        if (kpiReports.containsKey("executiveSummary")) {
            Map<String, Object> summary = (Map<String, Object>) kpiReports.get("executiveSummary");
            
            Table table = new Table(2);
            table.addCell("Metric");
            table.addCell("Value");
            
            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                table.addCell(entry.getKey());
                table.addCell(String.valueOf(entry.getValue()));
            }
            
            document.add(table);
        }
    }
    
    private void addMetricsSection(Document document, ReportGenerationRequest request) {
        document.add(new Paragraph("\nDetailed Metrics")
                .setFontSize(16)
                .setBold());
        
        for (String metric : request.getMetrics()) {
            document.add(new Paragraph("\n" + metric)
                    .setFontSize(14)
                    .setBold());
            
            // Add metric data table
            Table table = new Table(3);
            table.addCell("Period");
            table.addCell("Value");
            table.addCell("Change");
            
            // Add sample data rows
            for (int i = 0; i < 10; i++) {
                table.addCell("Period " + i);
                table.addCell(String.valueOf(Math.random() * 1000));
                table.addCell(String.format("%.2f%%", Math.random() * 20 - 10));
            }
            
            document.add(table);
        }
    }
    
    private void addChartsSection(Document document, ReportGenerationRequest request) {
        document.add(new Paragraph("\nCharts and Visualizations")
                .setFontSize(16)
                .setBold());
        
        document.add(new Paragraph("Charts would be rendered here using a charting library")
                .setItalic());
    }
    
    private void addSummaryToExcel(Sheet sheet, ReportGenerationRequest request) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(request.getReportName());
        
        // Generated date
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated:");
        dateRow.createCell(1).setCellValue(LocalDateTime.now().toString());
        
        // Period
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Period:");
        periodRow.createCell(1).setCellValue(request.getStartDate() + " to " + request.getEndDate());
        
        rowNum++; // Empty row
        
        // Summary data
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Metric");
        headerRow.createCell(1).setCellValue("Value");
        
        Map<String, Object> kpiReports = metricsCalculationService.generateKPIReports();
        if (kpiReports.containsKey("executiveSummary")) {
            Map<String, Object> summary = (Map<String, Object>) kpiReports.get("executiveSummary");
            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(entry.getKey());
                dataRow.createCell(1).setCellValue(String.valueOf(entry.getValue()));
            }
        }
    }
    
    private void addMetricDataToExcel(Sheet sheet, String metric, ReportGenerationRequest request) {
        int rowNum = 0;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Date");
        headerRow.createCell(1).setCellValue("Value");
        headerRow.createCell(2).setCellValue("Change %");
        
        // Data rows (sample data)
        for (int i = 0; i < 30; i++) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(LocalDateTime.now().minusDays(30 - i).toString());
            dataRow.createCell(1).setCellValue(Math.random() * 1000);
            dataRow.createCell(2).setCellValue(Math.random() * 20 - 10);
        }
    }
    
    private byte[] convertReportFormat(byte[] content, String targetFormat) {
        // In production, implement actual format conversion
        log.warn("Format conversion not implemented. Returning original content.");
        return content;
    }
    
    private Integer calculatePageCount(byte[] content, ReportGenerationRequest.OutputFormat format) {
        if (format == ReportGenerationRequest.OutputFormat.PDF) {
            // Estimate based on content size
            return Math.max(1, content.length / 3000);
        }
        return null;
    }
    
    private Integer calculateRecordCount(ReportGenerationRequest request) {
        // Calculate based on date range and metrics
        long days = java.time.Duration.between(request.getStartDate(), request.getEndDate()).toDays();
        return (int) (days * request.getMetrics().size());
    }
}
