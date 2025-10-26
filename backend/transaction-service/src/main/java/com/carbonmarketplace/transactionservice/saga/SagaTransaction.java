package com.carbonmarketplace.transactionservice.saga;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Saga Transaction Manager
 * 
 * Manages the execution and compensation of distributed transactions.
 */
@Slf4j
public class SagaTransaction {
    private final String sagaId;
    private final Integer timeoutSeconds;
    private final List<SagaStep> steps = new ArrayList<>();
    private boolean committed = false;
    private boolean rolledBack = false;
    
    public SagaTransaction(String sagaId, Integer timeoutSeconds) {
        this.sagaId = sagaId;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public <T> T execute(String stepName, Supplier<T> action, Runnable compensation) {
        if (rolledBack) {
            throw new IllegalStateException("Saga has been rolled back");
        }
        
        log.debug("Executing saga step: {} for saga: {}", stepName, sagaId);
        
        try {
            T result = action.get();
            steps.add(new SagaStep(stepName, compensation, true));
            log.debug("Saga step {} completed successfully", stepName);
            return result;
        } catch (Exception e) {
            log.error("Saga step {} failed: {}", stepName, e.getMessage());
            steps.add(new SagaStep(stepName, compensation, false));
            throw new SagaExecutionException("Step " + stepName + " failed: " + e.getMessage(), e);
        }
    }
    
    public void commit() {
        if (committed || rolledBack) {
            throw new IllegalStateException("Saga already finalized");
        }
        committed = true;
        log.info("Saga {} committed successfully", sagaId);
    }
    
    public void rollback() {
        if (committed || rolledBack) {
            return;
        }
        
        log.warn("Rolling back saga: {}", sagaId);
        rolledBack = true;
        
        // Execute compensations in reverse order
        Collections.reverse(steps);
        for (SagaStep step : steps) {
            if (step.isCompleted() && step.getCompensation() != null) {
                try {
                    log.debug("Compensating step: {}", step.getName());
                    step.getCompensation().run();
                } catch (Exception e) {
                    log.error("Compensation failed for step {}: {}", step.getName(), e.getMessage());
                }
            }
        }
        
        log.info("Saga {} rolled back", sagaId);
    }
    
    private static class SagaStep {
        private final String name;
        private final Runnable compensation;
        private final boolean completed;
        
        SagaStep(String name, Runnable compensation, boolean completed) {
            this.name = name;
            this.compensation = compensation;
            this.completed = completed;
        }
        
        String getName() { return name; }
        Runnable getCompensation() { return compensation; }
        boolean isCompleted() { return completed; }
    }
    
    public static class SagaExecutionException extends RuntimeException {
        public SagaExecutionException(String message) {
            super(message);
        }
        
        public SagaExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
