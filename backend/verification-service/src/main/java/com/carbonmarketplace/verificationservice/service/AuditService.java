package com.carbonmarketplace.verificationservice.service;

import com.carbonmarketplace.verificationservice.entity.AuditTrail;
import com.carbonmarketplace.verificationservice.repository.AuditTrailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing audit trail with hash chain for tamper detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditTrailRepository auditTrailRepository;
    private final ObjectMapper objectMapper;
    
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Log an audit action with hash chain.
     */
    @Transactional
    public AuditTrail logAction(UUID verificationId, UUID auditorId, 
                                AuditTrail.AuditAction action, String entityType,
                                String entityId, String oldValue, String description) {
        try {
            // Get request information
            HttpServletRequest request = getHttpRequest();
            String ipAddress = request != null ? getClientIP(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            // Get previous hash
            String previousHash = getPreviousHash();
            
            // Create audit entry
            AuditTrail audit = AuditTrail.builder()
                    .verificationId(verificationId)
                    .auditorId(auditorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(oldValue)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestId(UUID.randomUUID().toString())
                    .previousHash(previousHash)
                    .build();
            
            // Calculate hash for this entry
            String logHash = calculateHash(audit, previousHash);
            audit.setLogHash(logHash);
            
            audit = auditTrailRepository.save(audit);
            
            log.debug("Created audit trail entry {} with hash {}", audit.getAuditId(), logHash);
            
            return audit;
        } catch (Exception e) {
            log.error("Failed to create audit trail entry: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create audit trail", e);
        }
    }
    
    /**
     * Log an audit action with old and new values.
     */
    @Transactional
    public AuditTrail logActionWithValues(UUID verificationId, UUID auditorId,
                                          AuditTrail.AuditAction action, String entityType,
                                          String entityId, Object oldValues, Object newValues,
                                          String description) {
        try {
            String oldJson = oldValues != null ? objectMapper.writeValueAsString(oldValues) : null;
            String newJson = newValues != null ? objectMapper.writeValueAsString(newValues) : null;
            String changesJson = calculateChanges(oldValues, newValues);
            
            HttpServletRequest request = getHttpRequest();
            String ipAddress = request != null ? getClientIP(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            String previousHash = getPreviousHash();
            
            AuditTrail audit = AuditTrail.builder()
                    .verificationId(verificationId)
                    .auditorId(auditorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(oldJson)
                    .newValues(newJson)
                    .changes(changesJson)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestId(UUID.randomUUID().toString())
                    .previousHash(previousHash)
                    .build();
            
            String logHash = calculateHash(audit, previousHash);
            audit.setLogHash(logHash);
            
            return auditTrailRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to create audit trail entry with values: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create audit trail", e);
        }
    }
    
    /**
     * Get the hash of the previous audit entry.
     */
    private String getPreviousHash() {
        Optional<AuditTrail> lastEntry = auditTrailRepository.findLastAuditEntry();
        return lastEntry.map(AuditTrail::getLogHash).orElse(GENESIS_HASH);
    }
    
    /**
     * Calculate SHA-256 hash for audit entry.
     */
    private String calculateHash(AuditTrail audit, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            
            // Combine all relevant fields for hash calculation
            String content = audit.getHashContent() + "|" + previousHash;
            
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }
    
    /**
     * Verify the integrity of the hash chain.
     */
    public boolean verifyHashChain(UUID verificationId) {
        log.info("Verifying hash chain for verification {}", verificationId);
        
        var auditEntries = auditTrailRepository.findByVerificationIdOrderByCreatedAtDesc(verificationId);
        
        if (auditEntries.isEmpty()) {
            return true;
        }
        
        String expectedPreviousHash = GENESIS_HASH;
        
        // Verify from oldest to newest
        for (int i = auditEntries.size() - 1; i >= 0; i--) {
            AuditTrail audit = auditEntries.get(i);
            
            // Check if previous hash matches
            if (!audit.getPreviousHash().equals(expectedPreviousHash)) {
                log.error("Hash chain broken at entry {}: expected previous hash {} but got {}",
                        audit.getAuditId(), expectedPreviousHash, audit.getPreviousHash());
                return false;
            }
            
            // Recalculate hash
            String recalculatedHash = calculateHash(audit, expectedPreviousHash);
            
            // Verify hash matches stored hash
            if (!recalculatedHash.equals(audit.getLogHash())) {
                log.error("Hash mismatch for entry {}: expected {} but got {}",
                        audit.getAuditId(), audit.getLogHash(), recalculatedHash);
                return false;
            }
            
            expectedPreviousHash = audit.getLogHash();
        }
        
        log.info("Hash chain verified successfully for verification {}", verificationId);
        return true;
    }
    
    /**
     * Check for hash chain breaks in the entire database.
     */
    public long detectHashChainBreaks() {
        return auditTrailRepository.countHashChainBreaks();
    }
    
    /**
     * Calculate the difference between old and new values.
     */
    private String calculateChanges(Object oldValues, Object newValues) {
        try {
            if (oldValues == null || newValues == null) {
                return null;
            }
            
            Map<String, Object> oldMap = objectMapper.convertValue(oldValues, Map.class);
            Map<String, Object> newMap = objectMapper.convertValue(newValues, Map.class);
            
            Map<String, Object> changes = new java.util.HashMap<>();
            
            // Find changed fields
            for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();
                Object oldValue = oldMap.get(key);
                
                if (!java.util.Objects.equals(oldValue, newValue)) {
                    changes.put(key, Map.of(
                            "old", oldValue != null ? oldValue : "null",
                            "new", newValue != null ? newValue : "null"
                    ));
                }
            }
            
            return changes.isEmpty() ? null : objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            log.warn("Failed to calculate changes: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get HTTP request from context.
     */
    private HttpServletRequest getHttpRequest() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get client IP address from request.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Generate audit report for a verification.
     */
    public String generateAuditReport(UUID verificationId, LocalDateTime startDate, LocalDateTime endDate) {
        var auditEntries = auditTrailRepository.findByVerificationAndDateRange(
                verificationId, startDate, endDate);
        
        StringBuilder report = new StringBuilder();
        report.append("AUDIT REPORT\n");
        report.append("============\n\n");
        report.append("Verification ID: ").append(verificationId).append("\n");
        report.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        report.append("Total Entries: ").append(auditEntries.size()).append("\n");
        report.append("Hash Chain Status: ")
              .append(verifyHashChain(verificationId) ? "VALID" : "INVALID").append("\n\n");
        
        report.append("AUDIT TRAIL:\n");
        report.append("-----------\n");
        
        for (AuditTrail entry : auditEntries) {
            report.append("\n[").append(entry.getCreatedAt()).append("] ");
            report.append(entry.getAction()).append(" - ");
            report.append(entry.getEntityType()).append(" (").append(entry.getEntityId()).append(")\n");
            report.append("  Auditor: ").append(entry.getAuditorId()).append("\n");
            report.append("  Description: ").append(entry.getDescription()).append("\n");
            report.append("  IP: ").append(entry.getIpAddress()).append("\n");
            report.append("  Hash: ").append(entry.getLogHash()).append("\n");
        }
        
        return report.toString();
    }
}
