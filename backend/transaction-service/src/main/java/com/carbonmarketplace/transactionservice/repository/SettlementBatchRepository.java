package com.carbonmarketplace.transactionservice.repository;

import com.carbonmarketplace.transactionservice.entity.SettlementBatch;
import com.carbonmarketplace.transactionservice.entity.SettlementBatch.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

    // Find by batch number
    Optional<SettlementBatch> findByBatchNumber(String batchNumber);

    // Find by settlement date
    List<SettlementBatch> findBySettlementDate(LocalDate settlementDate);

    // Find batches ready for processing
    @Query("SELECT s FROM SettlementBatch s WHERE s.status IN ('PENDING', 'APPROVED') " +
           "AND s.cutoffTime <= :now")
    List<SettlementBatch> findBatchesReadyForProcessing(@Param("now") LocalDateTime now);

    // Find batches requiring approval
    List<SettlementBatch> findByStatusAndRequiresApproval(
            BatchStatus status, 
            Boolean requiresApproval);

    // Find unreconciled batches
    @Query("SELECT s FROM SettlementBatch s WHERE s.status = 'COMPLETED' " +
           "AND s.reconciled = false")
    List<SettlementBatch> findUnreconciledBatches();

    // Find batches with errors
    @Query("SELECT s FROM SettlementBatch s WHERE s.errorCount > 0 " +
           "AND s.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<SettlementBatch> findBatchesWithErrors();

    // Get batch statistics for date range
    @Query("SELECT COUNT(s), SUM(s.totalTransactions), SUM(s.totalAmountVnd), " +
           "SUM(s.totalFeesVnd), SUM(s.totalPayoutsVnd) " +
           "FROM SettlementBatch s WHERE s.settlementDate BETWEEN :startDate AND :endDate " +
           "AND s.status = 'COMPLETED'")
    List<Object[]> getBatchStatistics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Page through settlement history
    Page<SettlementBatch> findByStatusInOrderByCreatedAtDesc(
            List<BatchStatus> statuses, 
            Pageable pageable);

    // Update batch status
    @Modifying
    @Query("UPDATE SettlementBatch s SET s.status = :status, " +
           "s.processingCompletedAt = :completedAt WHERE s.batchId = :batchId")
    void updateBatchStatus(
            @Param("batchId") UUID batchId,
            @Param("status") BatchStatus status,
            @Param("completedAt") LocalDateTime completedAt);

    // Mark batch as reconciled
    @Modifying
    @Query("UPDATE SettlementBatch s SET s.reconciled = true, " +
           "s.reconciledAt = :reconciledAt, s.reconciledBy = :reconciledBy " +
           "WHERE s.batchId = :batchId")
    void markBatchReconciled(
            @Param("batchId") UUID batchId,
            @Param("reconciledAt") LocalDateTime reconciledAt,
            @Param("reconciledBy") UUID reconciledBy);

    // Check if batch exists for date
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM SettlementBatch s WHERE s.settlementDate = :date " +
           "AND s.status NOT IN ('CANCELLED', 'FAILED')")
    boolean existsBatchForDate(@Param("date") LocalDate date);
}
