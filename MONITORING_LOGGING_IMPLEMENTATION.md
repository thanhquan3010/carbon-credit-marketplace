# Monitoring & Logging Implementation Guide

This document describes the monitoring and logging implementation for the Carbon Credit Marketplace backend services.

## Overview

We have implemented comprehensive logging and monitoring using:
- **Logback** for structured logging
- **Micrometer** for application metrics
- **Prometheus** for metrics collection
- **Spring Boot Actuator** for application health and management endpoints

## Components Implemented

### 1. Logging Configuration (logback-spring.xml)

Each service now has a `logback-spring.xml` configuration file in `src/main/resources/` with the following features:

#### Features:
- **Console Appender**: Outputs logs to console with ISO8601 timestamps
- **File Appender**: Rotates logs daily with 30-day retention
- **Async Appender**: Non-blocking logging for better performance
- **Service-specific loggers**: Customized log levels for different packages
- **Trace support**: Includes traceId and spanId for distributed tracing

#### Log Files Location:
```
logs/
├── transaction-service.log
├── payment-service.log
├── marketplace-service.log
├── user-service.log
├── carbon-credit-service.log
├── verification-service.log
├── notification-service.log
├── analytics-service.log
└── vehicle-service.log
```

#### Example Log Output:
```
2025-10-27T10:30:45.123 [http-nio-8080-exec-1] INFO  c.c.t.controller.TransactionController - Creating purchase transaction for listing: 123e4567-e89b-12d3-a456-426614174000
```

### 2. Metrics Configuration

#### MetricsConfig Class
Each service has a `MetricsConfig` class that:
- Enables `@Timed` annotation support via `TimedAspect`
- Adds common tags to all metrics (application, service)
- Provides a `MetricsCollector` bean for custom business metrics

#### Services with MetricsConfig:
- `backend/transaction-service/src/main/java/com/carbonmarketplace/transactionservice/config/MetricsConfig.java`
- `backend/payment-service/src/main/java/com/carbonmarketplace/paymentservice/config/MetricsConfig.java`
- `backend/marketplace-service/src/main/java/com/carbonmarketplace/marketplaceservice/config/MetricsConfig.java`
- `backend/user-service/src/main/java/com/carbonmarketplace/userservice/config/MetricsConfig.java`

### 3. Custom Metrics

#### Transaction Service Metrics:
```java
// Counter metrics
transactions.created (status: success|failed)
refunds.requested (status: created)
settlements.processed (status: approved)
escrow.operations (operation: early_release)

// Summary metrics
transaction.amount (type: purchase)

// Timing metrics
transaction.creation.time
refund.request.time
settlement.approval.time
escrow.early.release.time
```

#### Payment Service Metrics:
```java
// Counter metrics
payments.initiated (method: MOMO|VNPAY|BANK_TRANSFER, provider: momo|vnpay|bank)
payments.completed (method: MOMO|VNPAY|BANK_TRANSFER, status: success|failed)
payments.retries (provider: momo|vnpay|bank)
payments.refunds.processed (method: MOMO|VNPAY|BANK_TRANSFER)
webhooks.received (provider: momo|vnpay|bank, status: success|failed)

// Summary metrics
payment.amount (method: MOMO|VNPAY|BANK_TRANSFER)

// Timing metrics
payment.creation.time
payment.status.query.time
payment.refund.time
payment.statistics.time
```

#### Marketplace Service Metrics:
```java
// Counter metrics
listings.created (type: fixed|auction)
listings.sold (type: fixed|auction)
bids.placed (listingType: auction)
search.queries (type: advanced|basic)
```

#### User Service Metrics:
```java
// Counter metrics
users.registered (type: individual|corporate)
auth.login.attempts (status: success|failed)
auth.password.resets
auth.2fa.enabled
users.profile.updates
```

## How to Use

### 1. Accessing Metrics

Metrics are exposed via Spring Boot Actuator endpoints:

```bash
# Prometheus format (for scraping)
curl http://localhost:8080/actuator/prometheus

# General metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/transaction.creation.time
```

### 2. Actuator Endpoints Configuration

Add to your `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:dev}
    export:
      prometheus:
        enabled: true
```

### 3. Adding Custom Metrics

#### Using @Timed Annotation:
```java
@PostMapping("/transactions")
@Timed(value = "transaction.creation.time", description = "Time taken to create a transaction")
public ResponseEntity<Transaction> createTransaction() {
    // Your code here
}
```

#### Using MetricsCollector:
```java
@RestController
@RequiredArgsConstructor
public class MyController {
    private final MetricsCollector metricsCollector;
    
    @PostMapping("/process")
    public void process() {
        metricsCollector.recordTransactionCreated("success");
    }
}
```

#### Using MeterRegistry Directly:
```java
@RestController
@RequiredArgsConstructor
public class MyController {
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/process")
    public void process() {
        // Counter
        meterRegistry.counter("custom.counter", "tag", "value").increment();
        
        // Gauge
        meterRegistry.gauge("custom.gauge", 42.0);
        
        // Timer
        Timer.Sample sample = Timer.start(meterRegistry);
        // ... do work ...
        sample.stop(meterRegistry.timer("custom.timer"));
    }
}
```

### 4. Viewing Logs

#### Docker Compose:
```bash
# View real-time logs
docker logs -f carbon-marketplace-transaction-service

# View logs from last 10 minutes
docker logs --since 10m carbon-marketplace-payment-service

# View logs with timestamps
docker logs -t carbon-marketplace-marketplace-service
```

#### File System:
```bash
# View latest log
tail -f backend/transaction-service/logs/transaction-service.log

# Search for errors
grep ERROR backend/payment-service/logs/payment-service.log

# View logs from specific date
cat backend/marketplace-service/logs/marketplace-service.2025-10-27.log
```

## Integration with Prometheus & Grafana

### 1. Prometheus Configuration

Add to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'spring-boot-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'transaction-service:8080'
        - 'payment-service:8087'
        - 'marketplace-service:8083'
        - 'user-service:8081'
        labels:
          application: 'carbon-marketplace'
```

### 2. Example Prometheus Queries

```promql
# Transaction creation rate
rate(transactions_created_total[5m])

# Payment success rate
sum(rate(payments_completed_total{status="success"}[5m])) / sum(rate(payments_completed_total[5m]))

# Average payment amount
avg(payment_amount_sum / payment_amount_count)

# 95th percentile transaction creation time
histogram_quantile(0.95, transaction_creation_time_seconds_bucket)
```

### 3. Grafana Dashboard Examples

#### Panel 1: Transaction Rate
```promql
Query: sum(rate(transactions_created_total[5m])) by (status)
Type: Graph
```

#### Panel 2: Payment Methods Distribution
```promql
Query: sum(payments_initiated_total) by (method)
Type: Pie Chart
```

#### Panel 3: API Response Times
```promql
Query: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
Type: Graph
```

## Monitoring Best Practices

### 1. What to Monitor

#### Golden Signals:
- **Latency**: Response times (@Timed metrics)
- **Traffic**: Request rates (counter metrics)
- **Errors**: Error rates (failed transaction counters)
- **Saturation**: Resource usage (JVM metrics)

#### Business Metrics:
- Transaction volumes and amounts
- Payment success/failure rates
- User registration and activity
- System health and availability

### 2. Alerting Rules

Example Prometheus alerting rules:

```yaml
groups:
  - name: carbon_marketplace
    rules:
      - alert: HighErrorRate
        expr: rate(transactions_created_total{status="failed"}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High transaction failure rate"
          
      - alert: SlowPaymentProcessing
        expr: histogram_quantile(0.95, payment_creation_time_seconds_bucket) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Payment processing is slow"
```

### 3. Log Levels

Configure appropriate log levels for different environments:

**Development:**
```yaml
logging:
  level:
    com.carbonmarketplace: DEBUG
    org.springframework.web: DEBUG
```

**Production:**
```yaml
logging:
  level:
    com.carbonmarketplace: INFO
    com.carbonmarketplace.critical: WARN
    org.springframework.web: WARN
```

## Troubleshooting

### Issue: Metrics not appearing

**Solution:**
1. Check actuator endpoints are enabled:
   ```bash
   curl http://localhost:8080/actuator
   ```
2. Verify `micrometer-registry-prometheus` dependency is in pom.xml
3. Ensure `management.endpoints.web.exposure.include` contains `prometheus`

### Issue: Logs not being written to file

**Solution:**
1. Check logs directory exists and has write permissions
2. Verify logback-spring.xml is in `src/main/resources/`
3. Check for logback errors in console output

### Issue: High memory usage due to metrics

**Solution:**
1. Limit metric cardinality (avoid high-cardinality tags like user IDs)
2. Configure metric retention:
   ```yaml
   management:
     metrics:
       distribution:
         percentiles-histogram:
           http.server.requests: true
         sla:
           http.server.requests: 10ms,50ms,100ms,200ms,500ms,1s
   ```

## Dependencies Added

The following dependencies were added to support monitoring and logging:

```xml
<!-- Micrometer for Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Next Steps

1. **Set up Prometheus** to scrape metrics from all services
2. **Configure Grafana** with dashboards for key metrics
3. **Set up alerting** using Prometheus AlertManager
4. **Implement centralized logging** with ELK stack or similar
5. **Add distributed tracing** using Zipkin or Jaeger

## References

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [Prometheus Documentation](https://prometheus.io/docs)
- [Grafana Documentation](https://grafana.com/docs)

