package com.carbonmarketplace.transactionservice.repository;

import com.carbonmarketplace.transactionservice.entity.RefundRequest;
import com.carbonmarketplace.transactionservice.entity.RefundRequest.RefundStatus;
import com.carbonmarketplace.transactionservice.entity.RefundRequest.RefundType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    // Find by refund number
    Optional<RefundRequest> findByRefundNumber(String refundNumber);

    // Find by transaction ID
    List<RefundRequest> findByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    // Find by idempotency key
    Optional<RefundRequest> findByIdempotencyKey(String idempotencyKey);

    // Find pending approval requests
    @Query("SELECT r FROM RefundRequest r WHERE r.status = 'AWAITING_APPROVAL' " +
           "AND r.requiresApproval = true " +
           "ORDER BY r.createdAt ASC")
    List<RefundRequest> findPendingApprovalRequests();

    // Find auto-approvable requests
    @Query("SELECT r FROM RefundRequest r WHERE r.status = 'PENDING' " +
           "AND r.refundAmountVnd <= :autoApproveThreshold " +
           "AND r.refundType = 'FULL'")
    List<RefundRequest> findAutoApprovableRequests(
            @Param("autoApproveThreshold") BigDecimal autoApproveThreshold);

    // Find requests for processing
    @Query("SELECT r FROM RefundRequest r WHERE r.status = 'APPROVED' " +
           "ORDER BY r.approvedAt ASC")
    List<RefundRequest> findRequestsForProcessing();

    // Find expired requests
    @Query("SELECT r FROM RefundRequest r WHERE r.status IN ('PENDING', 'AWAITING_APPROVAL') " +
           "AND r.expiresAt < :now")
    List<RefundRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    // Find requests by requester
    Page<RefundRequest> findByRequesterIdOrderByCreatedAtDesc(
            UUID requesterId, 
            Pageable pageable);

    // Find requests by status
    Page<RefundRequest> findByStatusOrderByCreatedAtDesc(
            RefundStatus status, 
            Pageable pageable);

    // Find failed requests for retry
    @Query("SELECT r FROM RefundRequest r WHERE r.status = 'FAILED' " +
           "AND r.retryCount < r.maxRetries " +
           "AND r.lastRetryAt < :retryAfter")
    List<RefundRequest> findRequestsForRetry(@Param("retryAfter") LocalDateTime retryAfter);

    // Count refunds by transaction
    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE r.transactionId = :transactionId " +
           "AND r.status NOT IN ('REJECTED', 'CANCELLED', 'EXPIRED')")
    Long countActiveRefundsByTransaction(@Param("transactionId") UUID transactionId);

    // Calculate total refunded amount for transaction
    @Query("SELECT COALESCE(SUM(r.refundAmountVnd), 0) FROM RefundRequest r " +
           "WHERE r.transactionId = :transactionId " +
           "AND r.status = 'COMPLETED'")
    BigDecimal calculateTotalRefundedAmount(@Param("transactionId") UUID transactionId);

    // Statistics queries
    @Query("SELECT COUNT(r), SUM(r.refundAmountVnd) FROM RefundRequest r " +
           "WHERE r.status = 'COMPLETED' " +
           "AND r.completedAt BETWEEN :startDate AND :endDate")
    List<Object[]> getRefundStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r.reasonCategory, COUNT(r) FROM RefundRequest r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY r.reasonCategory")
    List<Object[]> getRefundReasonDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Update refund status
    @Modifying
    @Query("UPDATE RefundRequest r SET r.status = :status, " +
           "r.approvedBy = :approvedBy, r.approvedAt = :approvedAt " +
           "WHERE r.refundId = :refundId")
    void approveRefund(
            @Param("refundId") UUID refundId,
            @Param("status") RefundStatus status,
            @Param("approvedBy") UUID approvedBy,
            @Param("approvedAt") LocalDateTime approvedAt);

    @Modifying
    @Query("UPDATE RefundRequest r SET r.status = 'REJECTED', " +
           "r.rejectedBy = :rejectedBy, r.rejectedAt = :rejectedAt, " +
           "r.rejectionReason = :reason WHERE r.refundId = :refundId")
    void rejectRefund(
            @Param("refundId") UUID refundId,
            @Param("rejectedBy") UUID rejectedBy,
            @Param("rejectedAt") LocalDateTime rejectedAt,
            @Param("reason") String reason);

    // Check if refund exists for transaction
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM RefundRequest r WHERE r.transactionId = :transactionId " +
           "AND r.status IN ('PENDING', 'AWAITING_APPROVAL', 'APPROVED', 'PROCESSING')")
    boolean hasActiveRefundRequest(@Param("transactionId") UUID transactionId);
}
