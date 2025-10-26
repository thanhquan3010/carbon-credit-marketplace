package com.carbonmarketplace.paymentservice.repository;

import com.carbonmarketplace.paymentservice.entity.BankAccount;
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
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    Optional<BankAccount> findByAccountId(UUID accountId);

    List<BankAccount> findByUserId(UUID userId);

    Page<BankAccount> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT b FROM BankAccount b WHERE b.userId = :userId AND b.status = :status")
    List<BankAccount> findByUserIdAndStatus(
        @Param("userId") UUID userId,
        @Param("status") BankAccount.AccountStatus status
    );

    @Query("SELECT b FROM BankAccount b WHERE b.userId = :userId AND b.isPrimary = true")
    Optional<BankAccount> findPrimaryAccountByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BankAccount b WHERE b.accountNumber = :accountNumber AND b.bankCode = :bankCode")
    Optional<BankAccount> findByAccountNumberAndBankCode(
        @Param("accountNumber") String accountNumber,
        @Param("bankCode") String bankCode
    );

    @Query("SELECT b FROM BankAccount b WHERE b.status = 'PENDING_VERIFICATION'")
    List<BankAccount> findPendingVerificationAccounts();

    @Query("SELECT b FROM BankAccount b WHERE b.userId = :userId AND b.deletedAt IS NULL")
    List<BankAccount> findActiveAccountsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(b) FROM BankAccount b WHERE b.userId = :userId AND b.status = 'ACTIVE'")
    long countActiveAccountsByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BankAccount b WHERE b.isVerified = false AND b.createdAt < :cutoffTime")
    List<BankAccount> findUnverifiedAccountsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}
