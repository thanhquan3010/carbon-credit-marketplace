# Payment Service

## Overview

The Payment Service is a critical microservice in the Carbon Credit Marketplace that handles all payment processing, gateway integrations, and financial transactions. It provides secure, reliable payment processing with support for multiple payment methods popular in the Vietnamese market.

## Features

### Core Features
- **Multi-Gateway Support**: MoMo, VNPay, ZaloPay, Bank Transfer, Stripe
- **Idempotency**: Safe payment retries with idempotency key support
- **Webhook Processing**: Secure webhook handling with signature verification
- **Virtual Accounts**: Dynamic virtual account generation for bank transfers
- **Escrow Management**: Automatic escrow and settlement (T+2)
- **Fee Calculation**: Platform and gateway fee management
- **Refund Processing**: Full and partial refund support
- **PCI-DSS Compliance**: No storage of sensitive card details

### Technical Features
- **Redis Caching**: For idempotency and distributed locks
- **Saga Pattern**: Distributed transaction management
- **Circuit Breaker**: Resilient gateway communication
- **Scheduled Tasks**: Automated settlement and retry mechanisms
- **Comprehensive Logging**: Full audit trail for all transactions

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Payment Service                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   MoMo      │  │   VNPay     │  │Bank Transfer│         │
│  │  Gateway    │  │  Gateway    │  │  Gateway    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│         │                │                │                  │
│         └────────────────┴────────────────┘                  │
│                          │                                   │
│                   ┌──────────────┐                          │
│                   │Gateway Router│                          │
│                   └──────────────┘                          │
│                          │                                   │
│           ┌──────────────┴──────────────┐                   │
│           │                             │                   │
│    ┌─────────────┐              ┌─────────────┐            │
│    │Payment      │              │Webhook      │            │
│    │Service      │              │Service      │            │
│    └─────────────┘              └─────────────┘            │
│           │                             │                   │
│    ┌─────────────────────────────────────┐                 │
│    │         PostgreSQL Database         │                 │
│    └─────────────────────────────────────┘                 │
│                                                             │
│    ┌─────────────────────────────────────┐                 │
│    │           Redis Cache               │                 │
│    └─────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

## API Documentation

### Authentication
All endpoints except webhooks require JWT authentication.

### Endpoints

#### 1. Create Payment
```http
POST /api/v1/payments
Authorization: Bearer {token}

Request:
{
  "transaction_id": "uuid",
  "user_id": "uuid",
  "amount": 5000000,
  "currency": "VND",
  "payment_method": "MOMO",
  "idempotency_key": "unique-key-123",
  "redirect_url": "https://app.carbon.vn/payment/success",
  "callback_url": "https://api.carbon.vn/webhooks/payment",
  "order_info": "Carbon Credit Purchase",
  "customer_info": {
    "name": "Nguyen Van A",
    "email": "user@example.com",
    "phone": "+84901234567"
  }
}

Response:
{
  "payment_id": "uuid",
  "transaction_id": "uuid",
  "amount": 5000000,
  "currency": "VND",
  "payment_method": "MOMO",
  "status": "PENDING",
  "payment_url": "https://payment.momo.vn/...",
  "qr_code": {
    "qr_data": "...",
    "qr_image_url": "..."
  },
  "expires_at": "2025-10-26T15:30:00Z",
  "message": "Payment created successfully"
}
```

#### 2. Get Payment Status
```http
GET /api/v1/payments/{payment_id}
Authorization: Bearer {token}

Response:
{
  "payment_id": "uuid",
  "transaction_id": "uuid",
  "amount": 5000000,
  "status": "COMPLETED",
  "paid_at": "2025-10-26T10:30:00Z",
  "gateway_transaction_id": "MOMO123456"
}
```

#### 3. Process Refund
```http
POST /api/v1/payments/{payment_id}/refund
Authorization: Bearer {token}

Request:
{
  "refund_amount": 5000000,
  "refund_reason": "Customer requested refund",
  "refund_type": "FULL",
  "idempotency_key": "refund-key-123"
}

Response:
{
  "payment_id": "uuid",
  "status": "REFUNDED",
  "refund_amount": 5000000,
  "refund_processed_at": "2025-10-26T11:00:00Z"
}
```

#### 4. Webhook Endpoints

##### MoMo Webhook
```http
POST /api/v1/webhooks/momo
X-Signature: {signature}

Request:
{
  "partnerCode": "MOMO",
  "orderId": "CARBON-uuid",
  "requestId": "uuid",
  "amount": 5000000,
  "orderInfo": "Carbon Credit Purchase",
  "orderType": "momo_wallet",
  "transId": 123456789,
  "resultCode": 0,
  "message": "Success",
  "payType": "webApp",
  "responseTime": 1635324895000,
  "signature": "..."
}
```

##### VNPay IPN
```http
POST /api/v1/webhooks/vnpay

Request:
{
  "vnp_TmnCode": "CARBON01",
  "vnp_TxnRef": "CARBON-uuid",
  "vnp_Amount": "500000000",
  "vnp_OrderInfo": "Carbon Credit Purchase",
  "vnp_ResponseCode": "00",
  "vnp_TransactionNo": "VNP123456",
  "vnp_BankCode": "VCB",
  "vnp_PayDate": "20251026103000",
  "vnp_SecureHash": "..."
}
```

## Payment Methods

### 1. MoMo E-Wallet
- **Gateway Fee**: 1.5%
- **Settlement**: T+1
- **Features**: QR Code, Deep Link
- **Timeout**: 30 minutes

### 2. VNPay
- **Gateway Fee**: 1.8%
- **Settlement**: T+1
- **Features**: Multiple banks, QR Code
- **Timeout**: 15 minutes

### 3. Bank Transfer
- **Gateway Fee**: 0%
- **Settlement**: T+2
- **Features**: Virtual accounts, Reference codes
- **Timeout**: 48 hours

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbon_payments
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Platform Settings
PLATFORM_FEE_PERCENTAGE=5
IDEMPOTENCY_TTL_HOURS=24

# MoMo Configuration
MOMO_PARTNER_CODE=MOMOIQA420180417
MOMO_ACCESS_KEY=F8BBA842ECF85
MOMO_SECRET_KEY=K951B6PE1waDMi640xX08PD3vg6EkVlz
MOMO_ENVIRONMENT=test

# VNPay Configuration
VNPAY_TMN_CODE=CARBON01
VNPAY_HASH_SECRET=KBTHCZMFWCBAQNHUYVGQIXBSRQZAOVFK
VNPAY_ENVIRONMENT=sandbox

# Bank Transfer
BANK_ACCOUNT_NAME=Carbon Credit Marketplace
BANK_MASTER_ACCOUNT=1234567890
BANK_TRANSFER_TIMEOUT_HOURS=48
```

## Database Schema

### payments Table
```sql
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    user_id UUID NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_reference VARCHAR(100),
    gateway_transaction_id VARCHAR(100),
    payment_url VARCHAR(500),
    virtual_account_number VARCHAR(50),
    platform_fee DECIMAL(19,2),
    gateway_fee DECIMAL(19,2),
    net_amount DECIMAL(19,2),
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Security

### Webhook Signature Verification
All webhooks are verified using HMAC-SHA256 or HMAC-SHA512 signatures to ensure authenticity.

### Idempotency
Idempotency keys prevent duplicate payments and ensure safe retries.

### PCI-DSS Compliance
- No storage of card details
- All sensitive data encrypted in transit (TLS 1.3)
- Tokenization for card payments

## Error Handling

### Common Error Codes
- `PAYMENT_001`: Invalid payment method
- `PAYMENT_002`: Insufficient funds
- `PAYMENT_003`: Payment expired
- `PAYMENT_004`: Duplicate payment
- `PAYMENT_005`: Invalid signature
- `PAYMENT_006`: Gateway timeout
- `PAYMENT_007`: Refund amount exceeds original payment
- `PAYMENT_008`: Payment already refunded

## Monitoring

### Health Check
```http
GET /actuator/health
```

### Metrics
```http
GET /actuator/metrics
GET /actuator/prometheus
```

### Key Metrics
- Payment success rate
- Average processing time
- Gateway availability
- Webhook processing latency
- Failed payment rate

## Development

### Prerequisites
- Java 17+
- PostgreSQL 13+
- Redis 6+
- Maven 3.8+

### Running Locally
```bash
# Start dependencies
docker-compose up -d postgres redis

# Run the service
mvn spring-boot:run

# Access Swagger UI
http://localhost:8085/swagger-ui.html
```

### Testing
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test coverage
mvn jacoco:report
```

## Deployment

### Docker
```bash
# Build image
docker build -t payment-service:latest .

# Run container
docker run -p 8085:8085 \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  payment-service:latest
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
      - name: payment-service
        image: payment-service:latest
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

## Support

For issues or questions, contact: support@carbonmarketplace.vn

## License

Copyright 2025 Carbon Credit Marketplace. All rights reserved.
