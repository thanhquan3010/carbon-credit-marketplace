package com.carbonmarketplace.transactionservice.repository;

import com.carbonmarketplace.transactionservice.entity.EscrowAccount;
import com.carbonmarketplace.transactionservice.entity.EscrowAccount.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowAccountRepository extends JpaRepository<EscrowAccount, UUID> {

    // Find with lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EscrowAccount e WHERE e.escrowAccountId = :id")
    Optional<EscrowAccount> findByIdWithLock(@Param("id") UUID id);

    // Find by transaction
    Optional<EscrowAccount> findByTransactionTransactionId(UUID transactionId);

    // Find by account number
    Optional<EscrowAccount> findByAccountNumber(String accountNumber);

    // Find accounts ready for auto-release
    @Query("SELECT e FROM EscrowAccount e WHERE e.status = 'HELD' " +
           "AND e.autoReleaseEnabled = true " +
           "AND e.scheduledReleaseDate <= :releaseDate")
    List<EscrowAccount> findAccountsForAutoRelease(@Param("releaseDate") LocalDateTime releaseDate);

    // Find expired holds
    @Query("SELECT e FROM EscrowAccount e WHERE e.status = 'HELD' " +
           "AND e.holdExpiresAt IS NOT NULL " +
           "AND e.holdExpiresAt < :now")
    List<EscrowAccount> findExpiredHolds(@Param("now") LocalDateTime now);

    // Find accounts by status
    List<EscrowAccount> findByStatus(EscrowStatus status);

    // Count active escrows
    @Query("SELECT COUNT(e) FROM EscrowAccount e WHERE e.status IN ('HELD', 'FUNDS_RECEIVED')")
    Long countActiveEscrows();

    // Update status
    @Modifying
    @Query("UPDATE EscrowAccount e SET e.status = :status, " +
           "e.actualReleaseDate = :releaseDate WHERE e.escrowAccountId = :id")
    void updateEscrowReleased(
            @Param("id") UUID id,
            @Param("status") EscrowStatus status,
            @Param("releaseDate") LocalDateTime releaseDate);

    // Mark credits as locked
    @Modifying
    @Query("UPDATE EscrowAccount e SET e.creditsLocked = true, " +
           "e.creditsLockId = :lockId WHERE e.escrowAccountId = :id")
    void markCreditsLocked(
            @Param("id") UUID id,
            @Param("lockId") String lockId);
}
