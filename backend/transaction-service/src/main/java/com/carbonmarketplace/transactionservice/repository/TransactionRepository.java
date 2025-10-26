package com.carbonmarketplace.transactionservice.repository;

import com.carbonmarketplace.transactionservice.entity.Transaction;
import com.carbonmarketplace.transactionservice.entity.Transaction.TransactionStatus;
import com.carbonmarketplace.transactionservice.entity.Transaction.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Find with pessimistic lock for updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :id")
    Optional<Transaction> findByIdWithLock(@Param("id") UUID id);

    // Find by idempotency key to prevent duplicates
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Find by listing
    List<Transaction> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    // Find by buyer
    Page<Transaction> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    // Find by seller
    Page<Transaction> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    // Find transactions by status
    List<Transaction> findByStatus(TransactionStatus status);
    
    Page<Transaction> findByStatusIn(List<TransactionStatus> statuses, Pageable pageable);

    // Find transactions for settlement
    @Query("SELECT t FROM Transaction t WHERE t.status = :status " +
           "AND t.escrowStatus = 'HELD' " +
           "AND t.settlementDate <= :settlementDate " +
           "AND t.settlementBatchId IS NULL")
    List<Transaction> findTransactionsForSettlement(
            @Param("status") TransactionStatus status,
            @Param("settlementDate") LocalDateTime settlementDate);

    // Find transactions in a settlement batch
    List<Transaction> findBySettlementBatchId(UUID settlementBatchId);

    // Find expired transactions
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'PROCESSING') " +
           "AND t.createdAt < :expiryTime")
    List<Transaction> findExpiredTransactions(@Param("expiryTime") LocalDateTime expiryTime);

    // Find refundable transactions
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :transactionId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.paymentStatus = 'COMPLETED' " +
           "AND t.completedAt > :refundableAfter")
    Optional<Transaction> findRefundableTransaction(
            @Param("transactionId") UUID transactionId,
            @Param("refundableAfter") LocalDateTime refundableAfter);

    // Statistics queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.buyerId = :userId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countUserTransactionsInPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.totalAmountVnd) FROM Transaction t WHERE t.buyerId = :userId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumUserSpendingInPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.sellerReceivesVnd) FROM Transaction t WHERE t.sellerId = :sellerId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumSellerEarningsInPeriod(
            @Param("sellerId") UUID sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Platform metrics
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "AND t.completedAt BETWEEN :startDate AND :endDate")
    Long countCompletedTransactionsInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.platformFeeVnd) FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "AND t.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPlatformFeesInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.creditAmountTons) FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "AND t.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCarbonCreditsTradedInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Update queries
    @Modifying
    @Query("UPDATE Transaction t SET t.status = :status, t.failureReason = :reason, " +
           "t.failedAt = CURRENT_TIMESTAMP WHERE t.transactionId = :id")
    void updateTransactionFailed(
            @Param("id") UUID id,
            @Param("status") TransactionStatus status,
            @Param("reason") String reason);

    @Modifying
    @Query("UPDATE Transaction t SET t.settlementBatchId = :batchId, " +
           "t.status = 'IN_SETTLEMENT' WHERE t.transactionId IN :transactionIds")
    void assignToSettlementBatch(
            @Param("batchId") UUID batchId,
            @Param("transactionIds") List<UUID> transactionIds);

    // Check daily transaction limit
    @Query("SELECT COALESCE(SUM(t.totalAmountVnd), 0) FROM Transaction t " +
           "WHERE t.buyerId = :userId " +
           "AND t.status NOT IN ('FAILED', 'CANCELLED', 'REFUNDED') " +
           "AND t.createdAt >= :startOfDay")
    BigDecimal calculateDailyTransactionTotal(
            @Param("userId") UUID userId,
            @Param("startOfDay") LocalDateTime startOfDay);

    // Find transactions requiring manual review
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'PROCESSING') " +
           "AND t.totalAmountVnd > :threshold " +
           "AND t.createdAt > :since")
    List<Transaction> findTransactionsRequiringReview(
            @Param("threshold") BigDecimal threshold,
            @Param("since") LocalDateTime since);

    // Cleanup old cancelled/failed transactions
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.status IN ('CANCELLED', 'FAILED') " +
           "AND t.createdAt < :before")
    int deleteOldFailedTransactions(@Param("before") LocalDateTime before);
}
