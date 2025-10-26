package com.carbonmarketplace.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_steps", indexes = {
    @Index(name = "idx_saga_transaction", columnList = "transaction_id"),
    @Index(name = "idx_saga_status", columnList = "status"),
    @Index(name = "idx_saga_step_name", columnList = "step_name"),
    @Index(name = "idx_saga_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "stepId")
@ToString(exclude = "transaction")
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_id")
    private UUID stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private StepType stepType;

    // Execution Details
    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "method_name", length = 100)
    private String methodName;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // Retry Information
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // Compensation Information
    @Column(name = "compensation_method", length = 100)
    private String compensationMethod;

    @Column(name = "compensation_payload", columnDefinition = "TEXT")
    private String compensationPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_status", length = 30)
    private CompensationStatus compensationStatus;

    @Column(name = "compensated_at")
    private LocalDateTime compensatedAt;

    @Column(name = "compensation_error", columnDefinition = "TEXT")
    private String compensationError;

    // Timing
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 60;

    // Idempotency
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "is_idempotent")
    @Builder.Default
    private Boolean isIdempotent = true;

    // Metadata
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED,
        SKIPPED,
        TIMEOUT
    }

    public enum StepType {
        ACTION,         // Normal forward action
        COMPENSATION,   // Compensation action
        PIVOT,         // Decision point
        PARALLEL,      // Can run in parallel with others
        SEQUENTIAL     // Must run in sequence
    }

    public enum CompensationStatus {
        NOT_REQUIRED,
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        PARTIALLY_COMPLETED
    }

    // Business Methods
    public boolean canRetry() {
        return status == StepStatus.FAILED && 
               retryCount < maxRetries &&
               !isTimeout();
    }

    public boolean isTimeout() {
        if (startedAt == null || timeoutSeconds == null) {
            return false;
        }
        return startedAt.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
    }

    public boolean requiresCompensation() {
        return status == StepStatus.COMPLETED && 
               compensationMethod != null &&
               (compensationStatus == null || compensationStatus == CompensationStatus.PENDING);
    }

    public void recordExecution(LocalDateTime start, LocalDateTime end) {
        this.startedAt = start;
        this.completedAt = end;
        if (start != null && end != null) {
            this.executionTimeMs = java.time.Duration.between(start, end).toMillis();
        }
    }

    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.lastRetryAt = LocalDateTime.now();
        // Exponential backoff for next retry
        int backoffSeconds = (int) Math.pow(2, this.retryCount) * 10;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(backoffSeconds);
    }
}
