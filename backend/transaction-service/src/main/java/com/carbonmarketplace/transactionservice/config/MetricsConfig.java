package com.carbonmarketplace.transactionservice.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Arrays;

/**
 * Metrics Configuration for Transaction Service
 * 
 * Configures Micrometer metrics, custom tags, and enables @Timed aspect.
 */
@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class MetricsConfig {

    @Value("${spring.application.name:transaction-service}")
    private String applicationName;

    /**
     * Enable @Timed annotation support
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Add common tags to all metrics
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                Arrays.asList(
                        Tag.of("application", applicationName),
                        Tag.of("service", "transaction-service")));
    }

    /**
     * Configure custom metrics
     */
    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MetricsCollector(meterRegistry);
    }

    /**
     * Custom metrics collector for transaction-specific metrics
     */
    public static class MetricsCollector {
        private final MeterRegistry meterRegistry;

        public MetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void recordTransactionCreated(String status) {
            meterRegistry.counter("transactions.created",
                    "status", status).increment();
        }

        public void recordTransactionAmount(Double amount, String type) {
            meterRegistry.summary("transaction.amount",
                    "type", type).record(amount);
        }

        public void recordRefundRequest(String status) {
            meterRegistry.counter("refunds.requested",
                    "status", status).increment();
        }

        public void recordSettlementProcessed(String status) {
            meterRegistry.counter("settlements.processed",
                    "status", status).increment();
        }

        public void recordEscrowOperation(String operation) {
            meterRegistry.counter("escrow.operations",
                    "operation", operation).increment();
        }
    }
}
