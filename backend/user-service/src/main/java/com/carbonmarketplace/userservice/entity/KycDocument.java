package com.carbonmarketplace.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_kyc_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kyc_doc_id")
    private UUID kycDocId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Document Information
    @Column(name = "kyc_level", nullable = false)
    private Integer kycLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false, length = 50)
    private IdType idType;

    @Column(name = "id_number", nullable = false, length = 100)
    private String idNumber;

    // Document Files
    @Column(name = "id_front_url", columnDefinition = "TEXT", nullable = false)
    private String idFrontUrl;

    @Column(name = "id_back_url", columnDefinition = "TEXT")
    private String idBackUrl;

    @Column(name = "selfie_url", columnDefinition = "TEXT", nullable = false)
    private String selfieUrl;

    @Column(name = "address_proof_url", columnDefinition = "TEXT")
    private String addressProofUrl;

    @Column(name = "business_license_url", columnDefinition = "TEXT")
    private String businessLicenseUrl;

    // Verification
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VerificationStatus status = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Metadata
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum IdType {
        NATIONAL_ID, PASSPORT, DRIVER_LICENSE
    }

    public enum VerificationStatus {
        PENDING, APPROVED, REJECTED
    }
}
