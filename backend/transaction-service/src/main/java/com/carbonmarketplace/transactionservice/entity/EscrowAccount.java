package com.carbonmarketplace.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "escrow_accounts", indexes = {
    @Index(name = "idx_escrow_transaction", columnList = "transaction_id"),
    @Index(name = "idx_escrow_status", columnList = "status"),
    @Index(name = "idx_escrow_release_date", columnList = "scheduled_release_date"),
    @Index(name = "idx_escrow_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "escrowAccountId")
@ToString(exclude = "transaction")
public class EscrowAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "escrow_account_id")
    private UUID escrowAccountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    // Escrow Details
    @Column(name = "amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountVnd;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EscrowStatus status = EscrowStatus.PENDING;

    // Credits Information
    @Column(name = "credit_amount_tons", nullable = false, precision = 10, scale = 4)
    private BigDecimal creditAmountTons;

    @Column(name = "credits_locked")
    @Builder.Default
    private Boolean creditsLocked = false;

    @Column(name = "credits_lock_id")
    private String creditsLockId;

    // Release Information
    @Column(name = "scheduled_release_date", nullable = false)
    private LocalDateTime scheduledReleaseDate;

    @Column(name = "actual_release_date")
    private LocalDateTime actualReleaseDate;

    @Column(name = "release_type", length = 30)
    private String releaseType; // AUTO, MANUAL, EARLY

    @Column(name = "early_release_reason", length = 500)
    private String earlyReleaseReason;

    @Column(name = "early_release_approved_by")
    private UUID earlyReleaseApprovedBy;

    // Refund Information
    @Column(name = "refund_amount_vnd", precision = 15, scale = 2)
    private BigDecimal refundAmountVnd;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_reference", length = 100)
    private String refundReference;

    // Hold Information
    @Column(name = "hold_placed_at")
    private LocalDateTime holdPlacedAt;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "auto_release_enabled")
    @Builder.Default
    private Boolean autoReleaseEnabled = true;

    // Audit Information
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "released_by", length = 100)
    private String releasedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum EscrowStatus {
        PENDING,          // Initial state
        AWAITING_FUNDS,   // Waiting for payment
        FUNDS_RECEIVED,   // Payment received, not yet held
        HELD,            // Funds and credits are held
        RELEASING,       // In process of release
        RELEASED,        // Successfully released to seller
        REFUNDING,       // In process of refund
        REFUNDED,        // Refunded to buyer
        EXPIRED,         // Hold period expired
        CANCELLED        // Transaction cancelled
    }

    // Business Methods
    public boolean canRelease() {
        return status == EscrowStatus.HELD && 
               scheduledReleaseDate != null &&
               scheduledReleaseDate.isBefore(LocalDateTime.now());
    }

    public boolean canRefund() {
        return status == EscrowStatus.HELD || status == EscrowStatus.FUNDS_RECEIVED;
    }

    public boolean isExpired() {
        return holdExpiresAt != null && holdExpiresAt.isBefore(LocalDateTime.now());
    }

    public void generateAccountNumber() {
        this.accountNumber = "ESC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public LocalDateTime calculateScheduledReleaseDate(int settlementDays) {
        LocalDateTime baseDate = holdPlacedAt != null ? holdPlacedAt : LocalDateTime.now();
        // Skip weekends
        LocalDateTime releaseDate = baseDate.plusDays(settlementDays);
        while (releaseDate.getDayOfWeek().getValue() > 5) { // Saturday = 6, Sunday = 7
            releaseDate = releaseDate.plusDays(1);
        }
        return releaseDate;
    }
}
