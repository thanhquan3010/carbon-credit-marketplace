package com.carbonmarketplace.userservice.config;

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
 * Metrics Configuration for User Service
 * 
 * Configures Micrometer metrics, custom tags, and enables @Timed aspect.
 */
@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class MetricsConfig {

    @Value("${spring.application.name:user-service}")
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
                        Tag.of("service", "user-service")));
    }

    /**
     * Configure custom metrics
     */
    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MetricsCollector(meterRegistry);
    }

    /**
     * Custom metrics collector for user-specific metrics
     */
    public static class MetricsCollector {
        private final MeterRegistry meterRegistry;

        public MetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void recordUserRegistration(String userType) {
            meterRegistry.counter("users.registered",
                    "type", userType).increment();
        }

        public void recordLoginAttempt(String status) {
            meterRegistry.counter("auth.login.attempts",
                    "status", status).increment();
        }

        public void recordPasswordReset() {
            meterRegistry.counter("auth.password.resets").increment();
        }

        public void recordTwoFactorEnabled() {
            meterRegistry.counter("auth.2fa.enabled").increment();
        }

        public void recordProfileUpdate() {
            meterRegistry.counter("users.profile.updates").increment();
        }
    }
}
