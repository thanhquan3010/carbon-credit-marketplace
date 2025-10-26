package com.carbonmarketplace.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches", indexes = {
    @Index(name = "idx_settlement_batch_date", columnList = "settlement_date"),
    @Index(name = "idx_settlement_batch_status", columnList = "status"),
    @Index(name = "idx_settlement_batch_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "batchId")
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "batch_number", unique = true, nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BatchStatus status = BatchStatus.PENDING;

    // Batch Statistics
    @Column(name = "total_transactions")
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "successful_transactions")
    @Builder.Default
    private Integer successfulTransactions = 0;

    @Column(name = "failed_transactions")
    @Builder.Default
    private Integer failedTransactions = 0;

    @Column(name = "total_amount_vnd", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountVnd = BigDecimal.ZERO;

    @Column(name = "total_fees_vnd", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalFeesVnd = BigDecimal.ZERO;

    @Column(name = "total_payouts_vnd", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalPayoutsVnd = BigDecimal.ZERO;

    // Processing Information
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Column(name = "processing_time_seconds")
    private Long processingTimeSeconds;

    @Column(name = "cutoff_time")
    private LocalDateTime cutoffTime;

    // Error Tracking
    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    // Reconciliation
    @Column(name = "reconciled")
    @Builder.Default
    private Boolean reconciled = false;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by")
    private UUID reconciledBy;

    @Column(name = "reconciliation_notes", columnDefinition = "TEXT")
    private String reconciliationNotes;

    // Approval for Large Batches
    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

    // Relationships
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_batch_id")
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    public enum BatchStatus {
        PENDING,           // Waiting to be processed
        COLLECTING,        // Collecting transactions
        AWAITING_APPROVAL, // Requires manual approval
        APPROVED,          // Approved for processing
        PROCESSING,        // Currently being processed
        PARTIALLY_COMPLETED, // Some transactions completed
        COMPLETED,         // All transactions completed
        FAILED,           // Processing failed
        CANCELLED,        // Batch cancelled
        RECONCILING,      // Under reconciliation
        RECONCILED        // Fully reconciled
    }

    // Business Methods
    public void generateBatchNumber() {
        this.batchNumber = "STL-" + settlementDate.toString().replace("-", "") + 
                          "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean canProcess() {
        return (status == BatchStatus.PENDING || status == BatchStatus.APPROVED) &&
               cutoffTime != null &&
               LocalDateTime.now().isAfter(cutoffTime);
    }

    public boolean requiresManualApproval() {
        // Require approval for batches > 100M VND or > 50 transactions
        return totalAmountVnd.compareTo(new BigDecimal("100000000")) > 0 ||
               totalTransactions > 50;
    }

    public void calculateStatistics() {
        if (transactions != null && !transactions.isEmpty()) {
            this.totalTransactions = transactions.size();
            this.totalAmountVnd = transactions.stream()
                    .map(Transaction::getTotalAmountVnd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.totalFeesVnd = transactions.stream()
                    .map(Transaction::getPlatformFeeVnd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.totalPayoutsVnd = transactions.stream()
                    .map(Transaction::getSellerReceivesVnd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.successfulTransactions = (int) transactions.stream()
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                    .count();
            this.failedTransactions = (int) transactions.stream()
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED)
                    .count();
        }
    }

    public void recordProcessingTime() {
        if (processingStartedAt != null && processingCompletedAt != null) {
            this.processingTimeSeconds = java.time.Duration
                    .between(processingStartedAt, processingCompletedAt)
                    .getSeconds();
        }
    }
}
