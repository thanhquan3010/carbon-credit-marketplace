package com.carbonmarketplace.userservice.repository;

import com.carbonmarketplace.userservice.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByUserUserId(UUID userId);

    Optional<KycDocument> findTopByUserUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT k FROM KycDocument k WHERE k.user.userId = :userId AND k.status = :status")
    List<KycDocument> findByUserIdAndStatus(@Param("userId") UUID userId,
            @Param("status") KycDocument.VerificationStatus status);

    @Query("SELECT k FROM KycDocument k WHERE k.user.userId = :userId AND k.kycLevel = :kycLevel")
    Optional<KycDocument> findByUserIdAndKycLevel(@Param("userId") UUID userId,
            @Param("kycLevel") Integer kycLevel);

    @Query("SELECT COUNT(k) > 0 FROM KycDocument k WHERE k.user.userId = :userId AND k.status = 'APPROVED'")
    boolean hasApprovedKycDocument(@Param("userId") UUID userId);

    @Query("SELECT k FROM KycDocument k WHERE k.status = 'PENDING' ORDER BY k.submittedAt ASC")
    List<KycDocument> findPendingKycDocuments();
}
