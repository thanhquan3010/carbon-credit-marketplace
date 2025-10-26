package com.carbonmarketplace.verificationservice.repository;

import com.carbonmarketplace.verificationservice.entity.VerificationRequest;
import com.carbonmarketplace.verificationservice.entity.VerificationRequest.Priority;
import com.carbonmarketplace.verificationservice.entity.VerificationRequest.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for verification request operations.
 */
@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    Optional<VerificationRequest> findByVerificationId(UUID verificationId);

    Page<VerificationRequest> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<VerificationRequest> findByStatus(VerificationStatus status, Pageable pageable);

    Page<VerificationRequest> findByStatusAndPriority(VerificationStatus status, Priority priority, Pageable pageable);

    Page<VerificationRequest> findByAssignedAuditorId(UUID auditorId, Pageable pageable);

    List<VerificationRequest> findByStatusAndAssignedAuditorIdIsNull(VerificationStatus status);

    @Query("SELECT vr FROM VerificationRequest vr WHERE vr.status = :status AND vr.slaDeadline < :deadline")
    List<VerificationRequest> findOverdueRequests(@Param("status") VerificationStatus status, 
                                                  @Param("deadline") LocalDateTime deadline);

    @Query("SELECT vr FROM VerificationRequest vr WHERE vr.vehicleId = :vehicleId " +
           "AND vr.tripDateStart <= :endDate AND vr.tripDateEnd >= :startDate")
    List<VerificationRequest> findOverlappingRequests(@Param("vehicleId") UUID vehicleId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.assignedAuditorId = :auditorId " +
           "AND vr.status IN ('PENDING', 'IN_REVIEW')")
    Integer countActiveRequestsByAuditor(@Param("auditorId") UUID auditorId);

    @Query("SELECT vr.assignedAuditorId, COUNT(vr) FROM VerificationRequest vr " +
           "WHERE vr.status IN ('PENDING', 'IN_REVIEW') AND vr.assignedAuditorId IS NOT NULL " +
           "GROUP BY vr.assignedAuditorId")
    List<Object[]> getAuditorWorkloadDistribution();

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.status = :status")
    Integer countByStatus(@Param("status") VerificationStatus status);

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.approvedAt >= :startDate")
    Integer countApprovedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(vr) FROM VerificationRequest vr WHERE vr.rejectedAt >= :startDate")
    Integer countRejectedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (vr.reviewedAt - vr.createdAt))/3600) " +
           "FROM VerificationRequest vr WHERE vr.reviewedAt IS NOT NULL " +
           "AND vr.createdAt >= :startDate")
    Double getAverageReviewTimeHours(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(vr.creditAmountTons) FROM VerificationRequest vr " +
           "WHERE vr.status = 'APPROVED' AND vr.approvedAt >= :startDate")
    Double getTotalCreditsApproved(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT * FROM verification_requests " +
           "WHERE status = 'PENDING' AND assigned_auditor_id IS NULL " +
           "ORDER BY " +
           "CASE priority " +
           "  WHEN 'HIGH' THEN 1 " +
           "  WHEN 'NORMAL' THEN 2 " +
           "  WHEN 'LOW' THEN 3 " +
           "END, " +
           "created_at ASC " +
           "LIMIT 1", nativeQuery = true)
    Optional<VerificationRequest> findNextUnassignedRequest();
}
