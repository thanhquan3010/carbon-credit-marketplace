package com.carbonmarketplace.paymentservice.repository;

import com.carbonmarketplace.paymentservice.entity.PaymentWebhook;
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
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, UUID> {

    Optional<PaymentWebhook> findByWebhookId(UUID webhookId);

    List<PaymentWebhook> findByPaymentId(UUID paymentId);

    List<PaymentWebhook> findByGateway(String gateway);

    Page<PaymentWebhook> findByGateway(String gateway, Pageable pageable);

    List<PaymentWebhook> findByProcessingStatus(PaymentWebhook.ProcessingStatus status);

    @Query("SELECT w FROM PaymentWebhook w WHERE w.signature = :signature AND w.createdAt > :since")
    Optional<PaymentWebhook> findDuplicateWebhook(
        @Param("signature") String signature,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT w FROM PaymentWebhook w WHERE w.processingStatus = 'FAILED' AND w.retryCount < :maxRetries")
    List<PaymentWebhook> findFailedWebhooksForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT w FROM PaymentWebhook w WHERE w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    Page<PaymentWebhook> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(w) FROM PaymentWebhook w WHERE w.processingStatus = :status")
    long countByProcessingStatus(@Param("status") PaymentWebhook.ProcessingStatus status);

    @Query("SELECT w FROM PaymentWebhook w WHERE w.isValidSignature = false")
    List<PaymentWebhook> findInvalidSignatures();
}
