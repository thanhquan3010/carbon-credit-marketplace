package com.carbonmarketplace.transactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Transaction Service Application
 * 
 * This service handles all transaction-related operations in the Carbon Credit Marketplace,
 * including:
 * - Transaction orchestration using Saga pattern
 * - Escrow account management
 * - Automatic T+2 settlement processing
 * - Refund workflow with approval mechanisms
 * - Compensating transactions for failure scenarios
 * 
 * @author Carbon Marketplace Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@EnableStateMachine
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
