package com.carbonmarketplace.paymentservice.repository;

import com.carbonmarketplace.paymentservice.entity.Payment;
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

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentId(UUID paymentId);

    Optional<Payment> findByTransactionId(UUID transactionId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByExternalReference(String externalReference);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findByUserId(UUID userId);

    Page<Payment> findByUserId(UUID userId, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);

    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(@Param("status") Payment.PaymentStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findStalePayments(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    List<Payment> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.virtualAccountNumber = :accountNumber")
    Optional<Payment> findByVirtualAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") Payment.PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :startDate AND :endDate ORDER BY p.paidAt DESC")
    Page<Payment> findCompletedPaymentsByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT p FROM Payment p WHERE p.retryCount > 0 AND p.status IN ('FAILED', 'PENDING')")
    List<Payment> findPaymentsForRetry();

    @Query(value = "SELECT * FROM payments p WHERE p.metadata::jsonb @> :metadata", nativeQuery = true)
    List<Payment> findByMetadata(@Param("metadata") String metadata);
}
