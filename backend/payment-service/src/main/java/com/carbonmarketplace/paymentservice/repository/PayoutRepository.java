package com.carbonmarketplace.paymentservice.repository;

import com.carbonmarketplace.paymentservice.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    Optional<Payout> findByPayoutId(UUID payoutId);

    Optional<Payout> findByTransactionId(UUID transactionId);

    Optional<Payout> findByPaymentId(UUID paymentId);

    List<Payout> findBySellerId(UUID sellerId);

    Page<Payout> findBySellerId(UUID sellerId, Pageable pageable);

    List<Payout> findByStatus(Payout.PayoutStatus status);

    Page<Payout> findByStatus(Payout.PayoutStatus status, Pageable pageable);

    List<Payout> findByBatchId(UUID batchId);

    @Query("SELECT p FROM Payout p WHERE p.status = 'SCHEDULED' AND p.settlementDate <= :now")
    List<Payout> findPayoutsForSettlement(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payout p WHERE p.sellerId = :sellerId AND p.status = :status")
    List<Payout> findBySellerIdAndStatus(
        @Param("sellerId") UUID sellerId,
        @Param("status") Payout.PayoutStatus status
    );

    @Query("SELECT p FROM Payout p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payout> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(p.netAmount) FROM Payout p WHERE p.sellerId = :sellerId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPayoutBySeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT SUM(p.platformFee) FROM Payout p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPlatformFees(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT p FROM Payout p WHERE p.status = 'FAILED' AND p.retryCount < :maxRetries")
    List<Payout> findFailedPayoutsForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT COUNT(p) FROM Payout p WHERE p.status = :status")
    long countByStatus(@Param("status") Payout.PayoutStatus status);

    @Query("SELECT p FROM Payout p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payout> findStalePendingPayouts(@Param("cutoffTime") LocalDateTime cutoffTime);
}
