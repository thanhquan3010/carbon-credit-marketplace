package com.carbonmarketplace.carboncreditservice.repository;

import com.carbonmarketplace.carboncreditservice.entity.VerificationRequest;
import com.carbonmarketplace.carboncreditservice.entity.VerificationRequest.VerificationStatus;
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
 * Repository for VerificationRequest entity
 */
@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
    
    Page<VerificationRequest> findByOwnerId(UUID ownerId, Pageable pageable);
    
    Page<VerificationRequest> findByOwnerIdAndStatus(UUID ownerId, VerificationStatus status, Pageable pageable);
    
    Page<VerificationRequest> findByAssignedAuditorId(UUID auditorId, Pageable pageable);
    
    Page<VerificationRequest> findByStatus(VerificationStatus status, Pageable pageable);
    
    List<VerificationRequest> findByStatusInAndSlaDeadlineBefore(
            List<VerificationStatus> statuses, 
            LocalDateTime deadline
    );
    
    @Query("SELECT v FROM VerificationRequest v WHERE v.vehicleId = :vehicleId " +
           "AND v.status = 'PENDING' " +
           "AND ((v.tripDateStart <= :endDate AND v.tripDateEnd >= :startDate))")
    List<VerificationRequest> findOverlappingPendingRequests(
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT COUNT(v) FROM VerificationRequest v WHERE v.ownerId = :ownerId " +
           "AND v.status IN ('PENDING', 'IN_REVIEW')")
    Long countPendingRequestsByOwner(@Param("ownerId") UUID ownerId);
    
    @Query("SELECT COUNT(v) FROM VerificationRequest v WHERE v.assignedAuditorId = :auditorId " +
           "AND v.status = 'IN_REVIEW'")
    Long countActiveAssignments(@Param("auditorId") UUID auditorId);
    
    Optional<VerificationRequest> findTopByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    
    @Query("SELECT v FROM VerificationRequest v WHERE v.status = 'PENDING' " +
           "ORDER BY v.priority DESC, v.createdAt ASC")
    Page<VerificationRequest> findPendingRequestsForAssignment(Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM VerificationRequest v WHERE v.ownerId = :ownerId " +
           "AND v.status = 'APPROVED' " +
           "AND v.approvedAt >= :since")
    Long countApprovedRequestsSince(
            @Param("ownerId") UUID ownerId,
            @Param("since") LocalDateTime since
    );
}
