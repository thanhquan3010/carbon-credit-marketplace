package com.carbonmarketplace.carboncreditservice.repository;

import com.carbonmarketplace.carboncreditservice.entity.CarbonWallet;
import com.carbonmarketplace.carboncreditservice.entity.CarbonWallet.WalletStatus;
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

/**
 * Repository for CarbonWallet entity
 */
@Repository
public interface CarbonWalletRepository extends JpaRepository<CarbonWallet, UUID> {
    
    Optional<CarbonWallet> findByUserId(UUID userId);
    
    Page<CarbonWallet> findByStatus(WalletStatus status, Pageable pageable);
    
    @Query("SELECT w FROM CarbonWallet w WHERE w.availableBalanceTons > :minBalance " +
           "ORDER BY w.availableBalanceTons DESC")
    Page<CarbonWallet> findWalletsWithMinimumBalance(
            @Param("minBalance") BigDecimal minBalance, 
            Pageable pageable
    );
    
    @Query("SELECT SUM(w.availableBalanceTons) FROM CarbonWallet w WHERE w.status = 'ACTIVE'")
    BigDecimal calculateTotalActiveBalance();
    
    @Query("SELECT SUM(w.totalEarnedTons) FROM CarbonWallet w")
    BigDecimal calculateTotalEarnedCredits();
    
    @Query("SELECT SUM(w.totalSoldTons) FROM CarbonWallet w")
    BigDecimal calculateTotalSoldCredits();
    
    @Query("SELECT SUM(w.totalRetiredTons) FROM CarbonWallet w")
    BigDecimal calculateTotalRetiredCredits();
    
    @Query("SELECT w FROM CarbonWallet w WHERE w.lastTransactionDate >= :since " +
           "ORDER BY w.lastTransactionDate DESC")
    Page<CarbonWallet> findActiveWalletsSince(
            @Param("since") LocalDateTime since, 
            Pageable pageable
    );
    
    @Query("SELECT COUNT(w) FROM CarbonWallet w WHERE w.status = 'ACTIVE' " +
           "AND w.availableBalanceTons > 0")
    Long countActiveWalletsWithBalance();
    
    List<CarbonWallet> findByStatusAndFrozenAtBefore(
            WalletStatus status, 
            LocalDateTime frozenBefore
    );
    
    @Modifying
    @Query("UPDATE CarbonWallet w SET w.pendingBalanceTons = w.pendingBalanceTons + :amount, " +
           "w.updatedAt = :now WHERE w.userId = :userId")
    int addPendingBalance(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("now") LocalDateTime now
    );
    
    @Modifying
    @Query("UPDATE CarbonWallet w SET " +
           "w.pendingBalanceTons = w.pendingBalanceTons - :amount, " +
           "w.availableBalanceTons = w.availableBalanceTons + :amount, " +
           "w.totalEarnedTons = w.totalEarnedTons + :amount, " +
           "w.totalVerifications = w.totalVerifications + 1, " +
           "w.lastTransactionDate = :now, " +
           "w.updatedAt = :now " +
           "WHERE w.userId = :userId")
    int confirmPendingCredits(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("now") LocalDateTime now
    );
    
    @Query("SELECT w FROM CarbonWallet w WHERE w.totalTransactions >= :minTransactions " +
           "ORDER BY w.totalTransactions DESC")
    Page<CarbonWallet> findTopTransactors(
            @Param("minTransactions") Integer minTransactions,
            Pageable pageable
    );
}
