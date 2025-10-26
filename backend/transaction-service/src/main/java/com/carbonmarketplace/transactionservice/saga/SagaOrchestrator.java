package com.carbonmarketplace.transactionservice.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga Orchestrator
 * 
 * Coordinates saga execution using Spring State Machine.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final StateMachineFactory<SagaStates, SagaEvents> stateMachineFactory;

    public enum SagaStates {
        STARTED,
        TRANSACTION_CREATED,
        CREDITS_LOCKED,
        ESCROW_CREATED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        CREDITS_TRANSFERRED,
        CERTIFICATE_GENERATED,
        SETTLEMENT_SCHEDULED,
        COMPLETED,
        COMPENSATING,
        FAILED
    }

    public enum SagaEvents {
        START,
        CREATE_TRANSACTION,
        LOCK_CREDITS,
        CREATE_ESCROW,
        PROCESS_PAYMENT,
        CONFIRM_PAYMENT,
        TRANSFER_CREDITS,
        GENERATE_CERTIFICATE,
        SCHEDULE_SETTLEMENT,
        COMPLETE,
        FAIL,
        COMPENSATE
    }

    /**
     * Start a new saga
     */
    public StateMachine<SagaStates, SagaEvents> startSaga(String sagaId) {
        log.info("Starting saga: {}", sagaId);
        
        StateMachine<SagaStates, SagaEvents> stateMachine = stateMachineFactory.getStateMachine(sagaId);
        stateMachine.start();
        
        return stateMachine;
    }

    /**
     * Send event to saga
     */
    public void sendEvent(StateMachine<SagaStates, SagaEvents> stateMachine, SagaEvents event) {
        log.debug("Sending event {} to saga", event);
        stateMachine.sendEvent(event);
    }

    /**
     * Get current state
     */
    public SagaStates getCurrentState(StateMachine<SagaStates, SagaEvents> stateMachine) {
        return stateMachine.getState().getId();
    }

    /**
     * Check if saga is completed
     */
    public boolean isCompleted(StateMachine<SagaStates, SagaEvents> stateMachine) {
        SagaStates state = getCurrentState(stateMachine);
        return state == SagaStates.COMPLETED || state == SagaStates.FAILED;
    }

    /**
     * Trigger compensation
     */
    public void compensate(StateMachine<SagaStates, SagaEvents> stateMachine) {
        log.warn("Triggering compensation for saga");
        sendEvent(stateMachine, SagaEvents.COMPENSATE);
    }
}
