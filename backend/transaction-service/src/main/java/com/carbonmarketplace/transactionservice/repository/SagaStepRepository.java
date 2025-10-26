package com.carbonmarketplace.transactionservice.repository;

import com.carbonmarketplace.transactionservice.entity.SagaStep;
import com.carbonmarketplace.transactionservice.entity.SagaStep.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {

    // Find steps by transaction
    List<SagaStep> findByTransactionTransactionIdOrderByStepOrder(UUID transactionId);

    // Find steps by saga ID
    List<SagaStep> findBySagaIdOrderByStepOrder(String sagaId);

    // Find next pending step in saga
    @Query("SELECT s FROM SagaStep s WHERE s.sagaId = :sagaId " +
           "AND s.status = 'PENDING' " +
           "ORDER BY s.stepOrder ASC")
    Optional<SagaStep> findNextPendingStep(@Param("sagaId") String sagaId);

    // Find failed steps for retry
    @Query("SELECT s FROM SagaStep s WHERE s.status = 'FAILED' " +
           "AND s.retryCount < s.maxRetries " +
           "AND s.nextRetryAt <= :now")
    List<SagaStep> findStepsForRetry(@Param("now") LocalDateTime now);

    // Find steps requiring compensation
    @Query("SELECT s FROM SagaStep s WHERE s.status = 'COMPLETED' " +
           "AND s.compensationStatus = 'PENDING' " +
           "AND s.transaction.transactionId = :transactionId " +
           "ORDER BY s.stepOrder DESC")
    List<SagaStep> findStepsForCompensation(@Param("transactionId") UUID transactionId);

    // Find timeout steps
    @Query("SELECT s FROM SagaStep s WHERE s.status = 'IN_PROGRESS' " +
           "AND s.startedAt < :timeoutThreshold")
    List<SagaStep> findTimeoutSteps(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    // Update step status
    @Modifying
    @Query("UPDATE SagaStep s SET s.status = :status, " +
           "s.completedAt = :completedAt WHERE s.stepId = :stepId")
    void updateStepStatus(
            @Param("stepId") UUID stepId,
            @Param("status") StepStatus status,
            @Param("completedAt") LocalDateTime completedAt);

    // Mark step as compensated
    @Modifying
    @Query("UPDATE SagaStep s SET s.compensationStatus = 'COMPLETED', " +
           "s.compensatedAt = :compensatedAt WHERE s.stepId = :stepId")
    void markStepCompensated(
            @Param("stepId") UUID stepId,
            @Param("compensatedAt") LocalDateTime compensatedAt);

    // Check if all steps completed
    @Query("SELECT CASE WHEN COUNT(s) = 0 THEN true ELSE false END " +
           "FROM SagaStep s WHERE s.sagaId = :sagaId " +
           "AND s.status NOT IN ('COMPLETED', 'SKIPPED')")
    boolean areAllStepsCompleted(@Param("sagaId") String sagaId);

    // Check if any step failed
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM SagaStep s WHERE s.sagaId = :sagaId " +
           "AND s.status = 'FAILED'")
    boolean hasFailedSteps(@Param("sagaId") String sagaId);

    // Get saga execution time
    @Query("SELECT SUM(s.executionTimeMs) FROM SagaStep s WHERE s.sagaId = :sagaId")
    Long getTotalExecutionTime(@Param("sagaId") String sagaId);
}
