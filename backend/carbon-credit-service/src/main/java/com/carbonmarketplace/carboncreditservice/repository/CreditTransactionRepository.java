package com.carbonmarketplace.carboncreditservice.repository;

import com.carbonmarketplace.carboncreditservice.entity.CreditTransaction;
import com.carbonmarketplace.carboncreditservice.entity.CreditTransaction.TransactionStatus;
import com.carbonmarketplace.carboncreditservice.entity.CreditTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for CreditTransaction entity
 */
@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    
    Page<CreditTransaction> findByFromUserId(UUID userId, Pageable pageable);
    
    Page<CreditTransaction> findByToUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT t FROM CreditTransaction t WHERE (t.fromUserId = :userId OR t.toUserId = :userId) " +
           "ORDER BY t.createdAt DESC")
    Page<CreditTransaction> findUserTransactions(@Param("userId") UUID userId, Pageable pageable);
    
    Page<CreditTransaction> findByCarbonCreditCreditId(UUID creditId, Pageable pageable);
    
    Page<CreditTransaction> findByStatus(TransactionStatus status, Pageable pageable);
    
    List<CreditTransaction> findByStatusAndCreatedAtBefore(
            TransactionStatus status, 
            LocalDateTime createdBefore
    );
    
    @Query("SELECT t FROM CreditTransaction t WHERE t.fromUserId = :userId " +
           "AND t.transactionType = :type AND t.createdAt >= :since " +
           "ORDER BY t.createdAt DESC")
    List<CreditTransaction> findUserTransactionsByTypeAndDate(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT SUM(t.amountTons) FROM CreditTransaction t WHERE t.toUserId = :userId " +
           "AND t.status = 'COMPLETED' AND t.transactionType IN ('SALE', 'TRANSFER') " +
           "AND t.completedAt >= :since")
    BigDecimal calculateTotalCreditsReceived(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT SUM(t.amountTons) FROM CreditTransaction t WHERE t.fromUserId = :userId " +
           "AND t.status = 'COMPLETED' AND t.transactionType IN ('SALE', 'TRANSFER') " +
           "AND t.completedAt >= :since")
    BigDecimal calculateTotalCreditsSent(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT COUNT(t) FROM CreditTransaction t WHERE (t.fromUserId = :userId OR t.toUserId = :userId) " +
           "AND t.status = 'COMPLETED'")
    Long countCompletedTransactionsByUser(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(t.totalValueVnd) FROM CreditTransaction t WHERE t.toUserId = :userId " +
           "AND t.status = 'COMPLETED' AND t.transactionType = 'SALE' " +
           "AND t.completedAt >= :since")
    BigDecimal calculateTotalEarnings(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT SUM(t.platformFeeVnd) FROM CreditTransaction t WHERE t.status = 'COMPLETED' " +
           "AND t.completedAt >= :startDate AND t.completedAt <= :endDate")
    BigDecimal calculatePlatformRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
