# Transaction Service

## Overview

The Transaction Service is a critical microservice in the Carbon Credit Marketplace that orchestrates complex distributed transactions using the Saga pattern. It manages the entire lifecycle of carbon credit purchases, including payment processing, escrow management, T+2 settlement, and refund workflows.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Saga Pattern Implementation](#saga-pattern-implementation)
- [Settlement System](#settlement-system)
- [Refund Management](#refund-management)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Testing](#testing)
- [Deployment](#deployment)

## Features

### Core Capabilities

- **Distributed Transaction Management**: Saga pattern implementation for atomic transactions across microservices
- **Escrow Account System**: Secure fund holding with automated release mechanisms
- **T+2 Settlement**: Automatic settlement processing with configurable batch sizes
- **Refund Workflow**: Multi-tier approval system with auto-approval for small amounts
- **Compensating Transactions**: Automatic rollback for failed transactions
- **Idempotency Support**: Prevents duplicate transactions using idempotency keys
- **Real-time Processing**: Asynchronous processing with progress tracking

### Business Features

- Carbon credit purchase orchestration
- Payment gateway integration
- Certificate generation coordination
- Buyer/Seller notification management
- Platform fee calculation
- Transaction history and reporting
- Audit trail maintenance

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Transaction Service                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │     Saga     │  │   Escrow     │  │  Settlement  │      │
│  │ Orchestrator │  │   Manager    │  │   Engine     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Refund    │  │   Schedule   │  │  Monitoring  │      │
│  │   Manager    │  │   Service    │  │   Metrics    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   External APIs    │
                    ├───────────────────┤
                    │ • Payment Service │
                    │ • Carbon Service  │
                    │ • Certificate Svc │
                    │ • Notification    │
                    └───────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 3.1.5
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis
- **Message Queue**: RabbitMQ
- **State Machine**: Spring Statemachine
- **Scheduler**: Quartz
- **Monitoring**: Prometheus + Grafana
- **Tracing**: Zipkin
- **API Documentation**: OpenAPI 3.0

## API Documentation

### Transaction Endpoints

#### Create Purchase Transaction
```http
POST /api/transactions/purchase
Content-Type: application/json

{
  "listingId": "uuid",
  "buyerId": "uuid",
  "sellerId": "uuid",
  "creditAmount": 10.5,
  "unitPrice": 500000,
  "buyerName": "John Doe",
  "buyerEmail": "john@example.com",
  "idempotencyKey": "unique-key"
}
```

#### Get Transaction Status
```http
GET /api/transactions/{transactionId}
```

#### List User Transactions
```http
GET /api/transactions/user/{userId}?page=0&size=10
```

### Settlement Endpoints

#### Get Settlement Batch
```http
GET /api/transactions/settlements/{batchId}
```

#### Approve Settlement Batch
```http
POST /api/transactions/settlements/{batchId}/approve
```

#### Get Settlement Statistics
```http
GET /api/transactions/settlements/statistics?startDate=2025-01-01&endDate=2025-12-31
```

### Refund Endpoints

#### Request Refund
```http
POST /api/transactions/refunds
Content-Type: application/json

{
  "transactionId": "uuid",
  "refundAmount": 100000,
  "reason": "Service not provided",
  "reasonCategory": "SERVICE_NOT_PROVIDED"
}
```

#### Approve Refund
```http
POST /api/transactions/refunds/{refundId}/approve
```

#### Reject Refund
```http
POST /api/transactions/refunds/{refundId}/reject
```

## Database Schema

### Core Tables

#### transactions
- Stores all transaction records
- Tracks status, payment information, and settlement details
- Links to escrow accounts and certificates

#### escrow_accounts
- Manages fund holding
- Tracks scheduled release dates
- Maintains credit lock information

#### saga_steps
- Records saga execution steps
- Tracks compensation status
- Stores retry information

#### settlement_batches
- Groups transactions for T+2 settlement
- Tracks batch processing status
- Maintains reconciliation records

#### refund_requests
- Manages refund workflows
- Tracks approval status
- Records financial impact

### Indexes
```sql
-- Performance indexes
CREATE INDEX idx_trans_buyer ON transactions(buyer_id);
CREATE INDEX idx_trans_seller ON transactions(seller_id);
CREATE INDEX idx_trans_status ON transactions(status);
CREATE INDEX idx_trans_processing ON transactions(status, payment_status, created_at);

-- Escrow indexes
CREATE INDEX idx_escrow_release_date ON escrow_accounts(scheduled_release_date);
CREATE INDEX idx_escrow_status ON escrow_accounts(status);

-- Settlement indexes
CREATE INDEX idx_settlement_batch_date ON settlement_batches(settlement_date);
CREATE INDEX idx_settlement_batch_status ON settlement_batches(status);
```

## Saga Pattern Implementation

### Transaction Flow

```
1. CREATE_TRANSACTION
   ├── Action: Create transaction record
   └── Compensation: Mark transaction as cancelled

2. LOCK_CREDITS
   ├── Action: Lock credits in inventory
   └── Compensation: Unlock credits

3. CREATE_ESCROW
   ├── Action: Create escrow account
   └── Compensation: Cancel escrow

4. PROCESS_PAYMENT
   ├── Action: Initiate payment
   └── Compensation: Refund payment

5. HOLD_IN_ESCROW
   ├── Action: Hold funds in escrow
   └── Compensation: Release escrow hold

6. TRANSFER_CREDITS
   ├── Action: Transfer credits to buyer
   └── Compensation: Reverse transfer

7. GENERATE_CERTIFICATE
   ├── Action: Generate ownership certificate
   └── Compensation: Void certificate

8. SCHEDULE_SETTLEMENT
   ├── Action: Schedule T+2 settlement
   └── Compensation: Cancel settlement

9. SEND_NOTIFICATIONS
   ├── Action: Send transaction notifications
   └── Compensation: None required
```

### Compensation Strategy

- **Automatic Rollback**: Failed steps trigger automatic compensation
- **Ordered Execution**: Compensations run in reverse order
- **Idempotent Operations**: All compensations are idempotent
- **Timeout Handling**: Steps timeout after configurable duration
- **Retry Logic**: Exponential backoff for failed operations

## Settlement System

### T+2 Settlement Process

1. **Transaction Completion**: Credits transferred, certificate generated
2. **Escrow Hold**: Funds held for 2 business days
3. **Batch Creation**: Daily batch at 4 PM for T+2 transactions
4. **Approval Check**: Large batches require manual approval
5. **Processing**: Parallel processing of batch transactions
6. **Payout**: Funds released to sellers
7. **Reconciliation**: Daily reconciliation of completed batches

### Settlement Configuration

```yaml
transaction:
  settlement:
    batch-size: 100
    processing-threads: 5
    t2-enabled: true
    cut-off-hour: 15  # 3 PM cutoff
```

### Express Settlement

- Available for premium users
- Same-day settlement if before cutoff
- Additional fees apply
- Bypasses standard T+2 hold

## Refund Management

### Refund Workflow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Request    │────▶│   Approval   │────▶│  Processing  │
│   Created    │     │   Required?  │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                             │                     │
                    ┌────────▼────────┐           │
                    │  Auto-Approve   │           │
                    │  if < 1M VND    │           │
                    └─────────────────┘           │
                                          ┌────────▼────────┐
                                          │ Credit Reversal │
                                          │ Payment Refund  │
                                          │ Escrow Release  │
                                          └─────────────────┘
```

### Refund Rules

- **Auto-approval**: Refunds under 1M VND
- **Manual approval**: Required above 10M VND
- **Time limit**: 30 days from transaction completion
- **Partial refunds**: Supported with pro-rata fee adjustment

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: transaction-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/carbon_marketplace
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  redis:
    host: localhost
    port: 6379
  
  rabbitmq:
    host: localhost
    port: 5672

server:
  port: 8085
  servlet:
    context-path: /api/transactions

transaction:
  saga:
    timeout-seconds: 300
    max-retry-attempts: 3
  
  escrow:
    hold-days: 2
    auto-release-enabled: true
    release-schedule-cron: "0 0 2 * * ?"
  
  refund:
    auto-approve-threshold-vnd: 1000000
    approval-required-above: 10000000
    max-refund-days: 30
  
  fees:
    platform-percentage: 5.0
    minimum-fee-vnd: 10000
```

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbon_marketplace
DB_USERNAME=carbon_user
DB_PASSWORD=secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Security
JWT_SECRET=your-secret-key

# External Services
PAYMENT_SERVICE_URL=http://payment-service:8083
CARBON_SERVICE_URL=http://carbon-credit-service:8081
CERTIFICATE_SERVICE_URL=http://certificate-service:8086
```

## Monitoring

### Health Checks

```http
GET /api/transactions/actuator/health
```

### Metrics Endpoints

```http
GET /api/transactions/actuator/metrics
GET /api/transactions/actuator/prometheus
```

### Key Metrics

- `transaction.created.count` - Total transactions created
- `transaction.completed.count` - Successfully completed transactions
- `transaction.failed.count` - Failed transactions
- `saga.execution.time` - Saga execution duration
- `escrow.active.count` - Active escrow accounts
- `settlement.batch.size` - Settlement batch sizes
- `refund.processing.time` - Refund processing duration

### Grafana Dashboards

1. **Transaction Overview**
   - Transaction volume
   - Success/failure rates
   - Average processing time
   - Platform fees collected

2. **Settlement Monitoring**
   - Daily settlement volumes
   - Batch processing status
   - Payout amounts
   - Reconciliation status

3. **Refund Analytics**
   - Refund request trends
   - Approval rates
   - Processing times
   - Reason distribution

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

### Test Coverage
- Target: 80% code coverage
- Critical paths: 100% coverage
- Saga compensations: Full testing

### Load Testing
```bash
# Using Apache JMeter
jmeter -n -t transaction-load-test.jmx -l results.jtl
```

## Deployment

### Docker Build

```dockerfile
FROM openjdk:17-slim
COPY target/transaction-service.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: transaction-service
  template:
    metadata:
      labels:
        app: transaction-service
    spec:
      containers:
      - name: transaction-service
        image: carbonmarketplace/transaction-service:1.0.0
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### Health Probes

```yaml
livenessProbe:
  httpGet:
    path: /api/transactions/actuator/health/liveness
    port: 8085
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/transactions/actuator/health/readiness
    port: 8085
  initialDelaySeconds: 30
  periodSeconds: 5
```

## Troubleshooting

### Common Issues

1. **Saga Timeout**
   - Check external service availability
   - Review timeout configuration
   - Verify network connectivity

2. **Settlement Delays**
   - Check batch processing logs
   - Verify cron schedule configuration
   - Review approval queue

3. **Refund Failures**
   - Verify payment gateway connectivity
   - Check credit reversal status
   - Review transaction eligibility

### Debug Logging

```yaml
logging:
  level:
    com.carbonmarketplace.transactionservice: DEBUG
    org.springframework.statemachine: DEBUG
    org.hibernate.SQL: DEBUG
```

## Support

For issues or questions:
- **Email**: platform-team@carbonmarketplace.com
- **Slack**: #transaction-service
- **Wiki**: https://wiki.carbonmarketplace.com/transaction-service

## License

Copyright © 2025 Carbon Marketplace. All rights reserved.
