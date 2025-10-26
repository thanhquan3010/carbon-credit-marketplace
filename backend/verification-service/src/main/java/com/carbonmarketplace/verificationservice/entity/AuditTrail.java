package com.carbonmarketplace.verificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an immutable audit trail with hash chain.
 */
@Entity
@Table(name = "verification_audit_trail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "auditor_id", nullable = false)
    private UUID auditorId;

    @Column(name = "auditor_name")
    private String auditorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    @Column(name = "changes", columnDefinition = "jsonb")
    private String changes;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_id")
    private String requestId;

    // Hash Chain fields for tamper detection
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "log_hash", length = 64, nullable = false)
    private String logHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enums
    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        APPROVE,
        REJECT,
        REQUEST_INFO,
        ASSIGN,
        UNASSIGN,
        ISSUE_CREDITS,
        ADJUST_AMOUNT,
        ADD_NOTES,
        UPLOAD_DOCUMENT,
        DOWNLOAD_DOCUMENT,
        RESOLVE_ANOMALY,
        ESCALATE
    }

    /**
     * Generates content for hash calculation.
     */
    public String getHashContent() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                verificationId,
                auditorId,
                action,
                entityType,
                entityId,
                oldValues != null ? oldValues : "",
                newValues != null ? newValues : "",
                description != null ? description : "",
                ipAddress != null ? ipAddress : "",
                createdAt
        );
    }
}
