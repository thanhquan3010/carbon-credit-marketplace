package com.carbonmarketplace.verificationservice.repository;

import com.carbonmarketplace.verificationservice.entity.AuditTrail;
import com.carbonmarketplace.verificationservice.entity.AuditTrail.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for audit trail operations.
 */
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, UUID> {

    Page<AuditTrail> findByVerificationId(UUID verificationId, Pageable pageable);

    Page<AuditTrail> findByAuditorId(UUID auditorId, Pageable pageable);

    List<AuditTrail> findByVerificationIdOrderByCreatedAtDesc(UUID verificationId);

    Page<AuditTrail> findByAction(AuditAction action, Pageable pageable);

    @Query("SELECT at FROM AuditTrail at WHERE at.verificationId = :verificationId " +
           "AND at.createdAt >= :startDate AND at.createdAt <= :endDate " +
           "ORDER BY at.createdAt DESC")
    List<AuditTrail> findByVerificationAndDateRange(@Param("verificationId") UUID verificationId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT at FROM AuditTrail at WHERE at.entityType = :entityType " +
           "AND at.entityId = :entityId ORDER BY at.createdAt DESC")
    List<AuditTrail> findByEntity(@Param("entityType") String entityType, 
                                  @Param("entityId") String entityId);

    @Query("SELECT at FROM AuditTrail at ORDER BY at.createdAt DESC LIMIT 1")
    Optional<AuditTrail> findLastAuditEntry();

    @Query("SELECT at.previousHash FROM AuditTrail at " +
           "WHERE at.auditId = :auditId")
    String getPreviousHashForAudit(@Param("auditId") UUID auditId);

    @Query("SELECT COUNT(at) FROM AuditTrail at " +
           "WHERE at.logHash = at.previousHash")
    Long countHashChainBreaks();

    @Query("SELECT at.action, COUNT(at) FROM AuditTrail at " +
           "WHERE at.createdAt >= :startDate GROUP BY at.action")
    List<Object[]> getActionDistribution(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT at.auditorId, at.auditorName, COUNT(at) FROM AuditTrail at " +
           "WHERE at.createdAt >= :startDate AND at.action IN ('APPROVE', 'REJECT') " +
           "GROUP BY at.auditorId, at.auditorName")
    List<Object[]> getAuditorActivityStats(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT * FROM verification_audit_trail " +
           "WHERE verification_id = :verificationId " +
           "AND action IN ('APPROVE', 'REJECT', 'ISSUE_CREDITS') " +
           "ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<AuditTrail> findKeyActionsForVerification(@Param("verificationId") UUID verificationId);

    @Query("SELECT at FROM AuditTrail at WHERE at.createdAt >= :startDate " +
           "AND at.createdAt <= :endDate AND at.auditorId = :auditorId " +
           "ORDER BY at.createdAt DESC")
    List<AuditTrail> generateAuditorReport(@Param("auditorId") UUID auditorId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT at.verificationId) FROM AuditTrail at " +
           "WHERE at.auditorId = :auditorId AND at.action = :action " +
           "AND at.createdAt >= :startDate")
    Long countDistinctVerificationsByAuditorAndAction(@Param("auditorId") UUID auditorId,
                                                      @Param("action") AuditAction action,
                                                      @Param("startDate") LocalDateTime startDate);
}
