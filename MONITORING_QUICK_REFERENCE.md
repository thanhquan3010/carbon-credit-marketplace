# Monitoring & Logging - Quick Reference Guide

## Quick Start

### 1. View Metrics

```bash
# Transaction Service metrics
curl http://localhost:8085/actuator/prometheus

# Payment Service metrics  
curl http://localhost:8087/actuator/prometheus

# Health check
curl http://localhost:8085/actuator/health
```

### 2. View Logs

```bash
# Docker logs (live tail)
docker logs -f carbon-marketplace-transaction-service
docker logs -f carbon-marketplace-payment-service

# File logs
tail -f backend/transaction-service/logs/transaction-service.log
tail -f backend/payment-service/logs/payment-service.log
```

## Available Metrics

### Transaction Service
```
# Counters
transactions_created_total{status="success|failed"}
refunds_requested_total{status="created"}
settlements_processed_total{status="approved"}
escrow_operations_total{operation="early_release"}

# Timers
transaction_creation_time_seconds
refund_request_time_seconds
settlement_approval_time_seconds
escrow_early_release_time_seconds

# Summaries
transaction_amount{type="purchase"}
```

### Payment Service
```
# Counters
payments_initiated_total{method="MOMO|VNPAY|BANK_TRANSFER", provider="momo|vnpay|bank"}
payments_completed_total{method="MOMO|VNPAY|BANK_TRANSFER", status="success|failed"}
payments_refunds_processed_total{method="MOMO|VNPAY|BANK_TRANSFER"}
webhooks_received_total{provider="momo|vnpay|bank", status="success|failed"}

# Timers
payment_creation_time_seconds
payment_status_query_time_seconds
payment_refund_time_seconds

# Summaries
payment_amount{method="MOMO|VNPAY|BANK_TRANSFER"}
```

## Common Tasks

### Check Transaction Success Rate

```bash
# Via Prometheus query
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=sum(rate(transactions_created_total{status="success"}[5m])) / sum(rate(transactions_created_total[5m]))'
```

### Check Payment Processing Time

```bash
# 95th percentile
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=histogram_quantile(0.95, rate(payment_creation_time_seconds_bucket[5m]))'
```

### Monitor Active Transactions

```bash
# Check JVM metrics
curl http://localhost:8085/actuator/metrics/jvm.threads.live
```

## Log Levels

### Change log level at runtime

```bash
# Set to DEBUG
curl -X POST http://localhost:8085/actuator/loggers/com.carbonmarketplace.transactionservice \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Set to INFO
curl -X POST http://localhost:8085/actuator/loggers/com.carbonmarketplace.transactionservice \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "INFO"}'
```

## Common Prometheus Queries

### Transaction Metrics

```promql
# Transaction rate (per second)
rate(transactions_created_total[5m])

# Transaction success rate
sum(rate(transactions_created_total{status="success"}[5m])) / sum(rate(transactions_created_total[5m]))

# Average transaction amount
avg(transaction_amount_sum / transaction_amount_count)

# Transaction creation p95 latency
histogram_quantile(0.95, rate(transaction_creation_time_seconds_bucket[5m]))
```

### Payment Metrics

```promql
# Payment volume by method
sum(payments_initiated_total) by (method)

# Payment success rate by provider
sum(rate(payments_completed_total{status="success"}[5m])) by (provider) / 
sum(rate(payments_completed_total[5m])) by (provider)

# Webhook failure rate
sum(rate(webhooks_received_total{status="failed"}[5m])) / 
sum(rate(webhooks_received_total[5m]))

# Payment processing time p99
histogram_quantile(0.99, rate(payment_creation_time_seconds_bucket[5m]))
```

### System Metrics

```promql
# CPU usage
system_cpu_usage

# Memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# HTTP request rate
rate(http_server_requests_seconds_count[5m])

# HTTP error rate (5xx)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

## Grafana Dashboard Examples

### Panel: Transaction Success Rate
```json
{
  "title": "Transaction Success Rate",
  "targets": [{
    "expr": "sum(rate(transactions_created_total{status=\"success\"}[5m])) / sum(rate(transactions_created_total[5m])) * 100"
  }],
  "type": "stat",
  "unit": "percent"
}
```

### Panel: Payment Methods Distribution
```json
{
  "title": "Payment Methods",
  "targets": [{
    "expr": "sum(payments_initiated_total) by (method)"
  }],
  "type": "piechart"
}
```

### Panel: API Response Times
```json
{
  "title": "API Response Times (p95)",
  "targets": [{
    "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
    "legendFormat": "{{uri}}"
  }],
  "type": "graph"
}
```

## Alerting Examples

### High Error Rate Alert

```yaml
- alert: HighTransactionFailureRate
  expr: |
    (sum(rate(transactions_created_total{status="failed"}[5m])) / 
     sum(rate(transactions_created_total[5m]))) > 0.1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "High transaction failure rate (> 10%)"
    description: "Transaction failure rate is {{ $value | humanizePercentage }}"
```

### Slow Payment Processing

```yaml
- alert: SlowPaymentProcessing
  expr: |
    histogram_quantile(0.95, 
      rate(payment_creation_time_seconds_bucket[5m])) > 5
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Payment processing is slow"
    description: "95th percentile payment time is {{ $value }}s"
```

### High Memory Usage

```yaml
- alert: HighMemoryUsage
  expr: |
    (jvm_memory_used_bytes{area="heap"} / 
     jvm_memory_max_bytes{area="heap"}) > 0.9
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High JVM heap memory usage"
    description: "Memory usage is {{ $value | humanizePercentage }}"
```

## Troubleshooting

### No metrics appearing

1. Check actuator is enabled:
   ```bash
   curl http://localhost:8085/actuator
   ```

2. Verify prometheus endpoint:
   ```bash
   curl http://localhost:8085/actuator/prometheus | grep transaction
   ```

### Logs not being written

1. Check logs directory exists:
   ```bash
   ls -la backend/transaction-service/logs/
   ```

2. Check logback configuration:
   ```bash
   grep -r "logback-spring.xml" backend/transaction-service/src/main/resources/
   ```

### High metric cardinality

1. Check number of unique metric combinations:
   ```bash
   curl -s http://localhost:8085/actuator/prometheus | \
     grep -E "^[a-z]" | wc -l
   ```

2. Identify high-cardinality metrics:
   ```bash
   curl -s http://localhost:8085/actuator/prometheus | \
     grep "^transaction" | cut -d'{' -f1 | sort | uniq -c | sort -rn
   ```

## Performance Tips

1. **Use appropriate histogram buckets:**
   ```yaml
   management:
     metrics:
       distribution:
         sla:
           http.server.requests: 10ms,50ms,100ms,200ms,500ms,1s,2s,5s
   ```

2. **Limit metric retention:**
   ```yaml
   management:
     metrics:
       enable:
         jvm: true
         process: true
         system: true
       tags:
         application: ${spring.application.name}
   ```

3. **Use sampling for high-volume metrics:**
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.1  # Sample 10% of requests
   ```

## Useful Commands

```bash
# Export metrics to file
curl -s http://localhost:8085/actuator/prometheus > metrics-$(date +%Y%m%d-%H%M%S).txt

# Search logs for errors
grep ERROR backend/transaction-service/logs/transaction-service.log | tail -20

# Monitor log file in real-time with filtering
tail -f backend/payment-service/logs/payment-service.log | grep -i "payment.*failed"

# Check service health
curl http://localhost:8085/actuator/health | jq .

# Get all available metrics
curl http://localhost:8085/actuator/metrics | jq .

# Get specific metric details
curl http://localhost:8085/actuator/metrics/transaction.creation.time | jq .
```

## Resources

- Full documentation: `MONITORING_LOGGING_IMPLEMENTATION.md`
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- Micrometer: https://micrometer.io/docs
- Prometheus: https://prometheus.io/docs/prometheus/latest/querying/basics/

