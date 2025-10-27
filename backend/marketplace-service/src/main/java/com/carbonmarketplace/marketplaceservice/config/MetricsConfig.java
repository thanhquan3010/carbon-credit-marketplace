package com.carbonmarketplace.marketplaceservice.config;

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
 * Metrics Configuration for Marketplace Service
 * 
 * Configures Micrometer metrics, custom tags, and enables @Timed aspect.
 */
@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class MetricsConfig {

    @Value("${spring.application.name:marketplace-service}")
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
                        Tag.of("service", "marketplace-service")));
    }

    /**
     * Configure custom metrics
     */
    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MetricsCollector(meterRegistry);
    }

    /**
     * Custom metrics collector for marketplace-specific metrics
     */
    public static class MetricsCollector {
        private final MeterRegistry meterRegistry;

        public MetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void recordListingCreated(String type) {
            meterRegistry.counter("listings.created",
                    "type", type).increment();
        }

        public void recordListingSold(String type) {
            meterRegistry.counter("listings.sold",
                    "type", type).increment();
        }

        public void recordBidPlaced(String listingType) {
            meterRegistry.counter("bids.placed",
                    "listingType", listingType).increment();
        }

        public void recordSearchQuery(String searchType) {
            meterRegistry.counter("search.queries",
                    "type", searchType).increment();
        }
    }
}
