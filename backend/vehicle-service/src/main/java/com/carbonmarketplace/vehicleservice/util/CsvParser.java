package com.carbonmarketplace.vehicleservice.util;

import com.carbonmarketplace.vehicleservice.dto.request.TripUploadRequest;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for parsing CSV files containing trip data
 */
@Component
@Slf4j
public class CsvParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /**
     * Parse trips from CSV input stream
     */
    public List<TripUploadRequest> parseTripsCsv(InputStream inputStream) throws IOException, CsvValidationException {
        log.info("Parsing trips CSV file");
        
        List<TripUploadRequest> trips = new ArrayList<>();
        
        try (Reader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {
            
            String[] headers = csvReader.readNext();
            if (headers == null || headers.length == 0) {
                throw new IllegalArgumentException("CSV file is empty or has no headers");
            }
            
            Map<String, Integer> headerMap = createHeaderMap(headers);
            validateHeaders(headerMap);
            
            String[] line;
            int lineNumber = 2; // Starting from line 2 (after headers)
            
            while ((line = csvReader.readNext()) != null) {
                try {
                    TripUploadRequest trip = parseTripFromCsvLine(line, headerMap);
                    trips.add(trip);
                } catch (Exception e) {
                    log.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                    // Continue parsing other lines
                }
                lineNumber++;
            }
        }
        
        log.info("Successfully parsed {} trips from CSV", trips.size());
        return trips;
    }

    /**
     * Parse trips using bean mapping (alternative approach)
     */
    public List<TripUploadRequest> parseTripsCsvWithBean(InputStream inputStream) throws IOException {
        log.info("Parsing trips CSV using bean mapping");
        
        try (Reader reader = new InputStreamReader(inputStream)) {
            List<TripCsvBean> beans = new CsvToBeanBuilder<TripCsvBean>(reader)
                    .withType(TripCsvBean.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
            
            return beans.stream()
                    .map(this::convertBeanToRequest)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Create header map for easier access
     */
    private Map<String, Integer> createHeaderMap(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i].toLowerCase().trim(), i);
        }
        return headerMap;
    }

    /**
     * Validate required headers are present
     */
    private void validateHeaders(Map<String, Integer> headerMap) {
        List<String> requiredHeaders = Arrays.asList(
                "start_time", "end_time", "distance_km"
        );
        
        // Check various possible header names
        Map<String, List<String>> headerVariations = new HashMap<>();
        headerVariations.put("start_time", Arrays.asList("start_time", "starttime", "start", "departure"));
        headerVariations.put("end_time", Arrays.asList("end_time", "endtime", "end", "arrival"));
        headerVariations.put("distance_km", Arrays.asList("distance_km", "distance", "km", "kilometers"));
        
        for (String required : requiredHeaders) {
            boolean found = false;
            for (String variation : headerVariations.get(required)) {
                if (headerMap.containsKey(variation)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Required header missing: " + required);
            }
        }
    }

    /**
     * Parse a single trip from CSV line
     */
    private TripUploadRequest parseTripFromCsvLine(String[] line, Map<String, Integer> headerMap) {
        TripUploadRequest request = new TripUploadRequest();
        
        // Parse required fields
        request.setStartTime(parseDateTime(getValue(line, headerMap, "start_time", "starttime", "start", "departure")));
        request.setEndTime(parseDateTime(getValue(line, headerMap, "end_time", "endtime", "end", "arrival")));
        request.setDistanceKm(parseBigDecimal(getValue(line, headerMap, "distance_km", "distance", "km", "kilometers")));
        
        // Parse optional fields
        String externalId = getValue(line, headerMap, "trip_id", "external_id", "id");
        if (externalId != null && !externalId.isEmpty()) {
            request.setExternalTripId(externalId);
        }
        
        // Location fields
        String startLat = getValue(line, headerMap, "start_lat", "start_latitude", "departure_lat");
        if (startLat != null && !startLat.isEmpty()) {
            request.setStartLat(parseBigDecimal(startLat));
        }
        
        String startLng = getValue(line, headerMap, "start_lng", "start_longitude", "departure_lng");
        if (startLng != null && !startLng.isEmpty()) {
            request.setStartLng(parseBigDecimal(startLng));
        }
        
        String endLat = getValue(line, headerMap, "end_lat", "end_latitude", "arrival_lat");
        if (endLat != null && !endLat.isEmpty()) {
            request.setEndLat(parseBigDecimal(endLat));
        }
        
        String endLng = getValue(line, headerMap, "end_lng", "end_longitude", "arrival_lng");
        if (endLng != null && !endLng.isEmpty()) {
            request.setEndLng(parseBigDecimal(endLng));
        }
        
        // Address fields
        String startAddress = getValue(line, headerMap, "start_address", "departure_address", "from");
        if (startAddress != null && !startAddress.isEmpty()) {
            request.setStartAddress(startAddress);
        }
        
        String endAddress = getValue(line, headerMap, "end_address", "arrival_address", "to");
        if (endAddress != null && !endAddress.isEmpty()) {
            request.setEndAddress(endAddress);
        }
        
        // Speed and energy fields
        String avgSpeed = getValue(line, headerMap, "avg_speed", "average_speed", "speed");
        if (avgSpeed != null && !avgSpeed.isEmpty()) {
            request.setAvgSpeed(parseBigDecimal(avgSpeed));
        }
        
        String maxSpeed = getValue(line, headerMap, "max_speed", "maximum_speed");
        if (maxSpeed != null && !maxSpeed.isEmpty()) {
            request.setMaxSpeed(parseBigDecimal(maxSpeed));
        }
        
        String energy = getValue(line, headerMap, "energy_kwh", "energy_consumed", "energy", "kwh");
        if (energy != null && !energy.isEmpty()) {
            request.setEnergyConsumedKwh(parseBigDecimal(energy));
        }
        
        // Additional metrics
        String idleTime = getValue(line, headerMap, "idle_time", "idle_seconds");
        if (idleTime != null && !idleTime.isEmpty()) {
            request.setIdleTimeSeconds(parseInt(idleTime));
        }
        
        String drivingTime = getValue(line, headerMap, "driving_time", "driving_seconds");
        if (drivingTime != null && !drivingTime.isEmpty()) {
            request.setDrivingTimeSeconds(parseInt(drivingTime));
        }
        
        String batteryStart = getValue(line, headerMap, "battery_start", "soc_start");
        if (batteryStart != null && !batteryStart.isEmpty()) {
            request.setBatteryLevelStart(parseInt(batteryStart));
        }
        
        String batteryEnd = getValue(line, headerMap, "battery_end", "soc_end");
        if (batteryEnd != null && !batteryEnd.isEmpty()) {
            request.setBatteryLevelEnd(parseInt(batteryEnd));
        }
        
        return request;
    }

    /**
     * Get value from CSV line by trying multiple header variations
     */
    private String getValue(String[] line, Map<String, Integer> headerMap, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = headerMap.get(header.toLowerCase());
            if (index != null && index < line.length) {
                String value = line[index];
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse date time with multiple format support
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }

    /**
     * Parse BigDecimal from string
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // Remove any non-numeric characters except decimal point and minus
            String cleaned = value.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + value);
        }
    }

    /**
     * Parse integer from string
     */
    private Integer parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // Remove any non-numeric characters except minus
            String cleaned = value.replaceAll("[^0-9-]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + value);
        }
    }

    /**
     * Convert CSV bean to TripUploadRequest
     */
    private TripUploadRequest convertBeanToRequest(TripCsvBean bean) {
        return TripUploadRequest.builder()
                .externalTripId(bean.getTripId())
                .startTime(bean.getStartTime())
                .endTime(bean.getEndTime())
                .startLat(bean.getStartLat())
                .startLng(bean.getStartLng())
                .endLat(bean.getEndLat())
                .endLng(bean.getEndLng())
                .startAddress(bean.getStartAddress())
                .endAddress(bean.getEndAddress())
                .distanceKm(bean.getDistanceKm())
                .avgSpeed(bean.getAvgSpeed())
                .maxSpeed(bean.getMaxSpeed())
                .energyConsumedKwh(bean.getEnergyKwh())
                .build();
    }

    /**
     * CSV bean for mapping
     */
    @Data
    public static class TripCsvBean {
        @CsvBindByName(column = "trip_id")
        private String tripId;
        
        @CsvBindByName(column = "start_time", required = true)
        @CsvDate("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @CsvBindByName(column = "end_time", required = true)
        @CsvDate("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        @CsvBindByName(column = "start_lat")
        private BigDecimal startLat;
        
        @CsvBindByName(column = "start_lng")
        private BigDecimal startLng;
        
        @CsvBindByName(column = "end_lat")
        private BigDecimal endLat;
        
        @CsvBindByName(column = "end_lng")
        private BigDecimal endLng;
        
        @CsvBindByName(column = "start_address")
        private String startAddress;
        
        @CsvBindByName(column = "end_address")
        private String endAddress;
        
        @CsvBindByName(column = "distance_km", required = true)
        private BigDecimal distanceKm;
        
        @CsvBindByName(column = "avg_speed")
        private BigDecimal avgSpeed;
        
        @CsvBindByName(column = "max_speed")
        private BigDecimal maxSpeed;
        
        @CsvBindByName(column = "energy_kwh")
        private BigDecimal energyKwh;
    }
}

