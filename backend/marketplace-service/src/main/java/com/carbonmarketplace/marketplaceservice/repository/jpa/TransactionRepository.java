package com.carbonmarketplace.marketplaceservice.repository.jpa;

import com.carbonmarketplace.marketplaceservice.entity.Transaction;
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

/**
 * JPA repository for Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    Page<Transaction> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);
    
    Page<Transaction> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByStatus(@Param("status") Transaction.TransactionStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'COMPLETED' AND t.settlementCompletedAt IS NULL")
    List<Transaction> findUnsettledTransactionsBySeller(@Param("sellerId") UUID sellerId);
    
    @Query("SELECT SUM(t.sellerNetAmountVnd) FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'COMPLETED' AND t.settlementCompletedAt IS NULL")
    BigDecimal calculateUnsettledAmountForSeller(@Param("sellerId") UUID sellerId);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PAYMENT_PROCESSING' AND t.createdAt < :timeout")
    List<Transaction> findTimedOutPayments(@Param("timeout") LocalDateTime timeout);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.buyerId = :buyerId AND t.status = 'COMPLETED'")
    Integer countCompletedTransactionsByBuyer(@Param("buyerId") UUID buyerId);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'COMPLETED'")
    Integer countCompletedTransactionsBySeller(@Param("sellerId") UUID sellerId);
    
    @Query("SELECT AVG(t.pricePerTonVnd) FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "AND (:region IS NULL OR EXISTS (SELECT l FROM Listing l WHERE l.id = t.listingId AND l.region = :region)) " +
           "AND t.completedAt >= :since")
    BigDecimal calculateAveragePrice(@Param("region") String region, @Param("since") LocalDateTime since);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("status") Transaction.TransactionStatus status);
}
