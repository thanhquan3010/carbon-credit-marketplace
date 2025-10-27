package com.carbonmarketplace.paymentservice.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Metrics Configuration for Payment Service
 * 
 * Configures Micrometer metrics, custom tags, and enables @Timed aspect.
 */
@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class MetricsConfig {

    @Value("${spring.application.name:payment-service}")
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
                        Tag.of("service", "payment-service")));
    }

    /**
     * Configure custom metrics
     */
    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MetricsCollector(meterRegistry);
    }

    /**
     * Custom metrics collector for payment-specific metrics
     */
    public static class MetricsCollector {
        private final MeterRegistry meterRegistry;

        public MetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void recordPaymentInitiated(String method, String provider) {
            meterRegistry.counter("payments.initiated",
                    "method", method,
                    "provider", provider).increment();
        }

        public void recordPaymentCompleted(String method, String status) {
            meterRegistry.counter("payments.completed",
                    "method", method,
                    "status", status).increment();
        }

        public void recordPaymentAmount(BigDecimal amount, String method) {
            if (amount != null) {
                meterRegistry.summary("payment.amount",
                        "method", method).record(amount.doubleValue());
            }
        }

        public void recordWebhookReceived(String provider, String status) {
            meterRegistry.counter("webhooks.received",
                    "provider", provider,
                    "status", status).increment();
        }

        public void recordPaymentRetry(String provider) {
            meterRegistry.counter("payments.retries",
                    "provider", provider).increment();
        }
    }
}
