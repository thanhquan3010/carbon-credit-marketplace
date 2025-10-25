# System Architecture
## Carbon Credit Marketplace for EV Owners

**Version**: 1.0  
**Last Updated**: October 25, 2025  
**Architecture Style**: Microservices / Service-Oriented Architecture

---

## Table of Contents

1. [System Overview](#system-overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Architecture Principles](#architecture-principles)
4. [Component Architecture](#component-architecture)
5. [Data Flow](#data-flow)
6. [Infrastructure Architecture](#infrastructure-architecture)
7. [Security Architecture](#security-architecture)
8. [Deployment Architecture](#deployment-architecture)
9. [Scalability & Performance](#scalability--performance)
10. [Monitoring & Observability](#monitoring--observability)
11. [Disaster Recovery](#disaster-recovery)

---

## System Overview

### Vision
A **scalable, secure, and reliable** platform that connects EV owners, corporate buyers, and carbon verifiers in a transparent marketplace for verified carbon credits.

### Key Architectural Goals
- ✅ **High Availability**: 99.5% uptime SLA
- ✅ **Scalability**: Support 10x growth without architecture changes
- ✅ **Security**: Enterprise-grade security for financial transactions
- ✅ **Performance**: Sub-second response times for critical operations
- ✅ **Maintainability**: Modular, testable, well-documented codebase
- ✅ **Extensibility**: Easy to add new features and integrations

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                 │
├──────────────────────────────────────────────────────────────────────┤
│  Web App (React)  │  Mobile PWA  │  Admin Portal  │  CVA Portal      │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                     ┌────────────▼────────────┐
                     │   CDN + Load Balancer   │
                     │    (CloudFront/ALB)     │
                     └────────────┬────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
┌────────▼────────┐   ┌──────────▼──────────┐   ┌────────▼────────┐
│  API Gateway    │   │   WebSocket Server  │   │  Static Assets  │
│  (Kong/NGINX)   │   │  (Real-time Events) │   │   (S3/CDN)      │
└────────┬────────┘   └──────────┬──────────┘   └─────────────────┘
         │                       │
         │         ┌─────────────┴────────────────────────┐
         │         │         SERVICE MESH                  │
         │         │      (Service Discovery & LB)        │
         │         └──────────────────────────────────────┘
         │                       │
┌────────┴───────────────────────┴───────────────────────────────────┐
│                      MICROSERVICES LAYER                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   User       │  │   Vehicle    │  │   Trip       │             │
│  │   Service    │  │   Service    │  │   Service    │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   Carbon     │  │ Verification │  │  Marketplace │             │
│  │   Credit     │  │   Service    │  │   Service    │             │
│  │   Service    │  └──────────────┘  └──────────────┘             │
│  └──────────────┘                                                   │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ Transaction  │  │   Payment    │  │ Certificate  │             │
│  │   Service    │  │   Service    │  │   Service    │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ Notification │  │   Admin      │  │   Analytics  │             │
│  │   Service    │  │   Service    │  │   Service    │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                        DATA LAYER                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  PostgreSQL  │  │    Redis     │  │ Elasticsearch│             │
│  │  (Primary)   │  │   (Cache)    │  │  (Search)    │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   S3/GCS     │  │   RabbitMQ   │  │  TimescaleDB │             │
│  │  (Storage)   │  │  (Message    │  │  (Analytics) │             │
│  │              │  │   Queue)     │  │              │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                   EXTERNAL INTEGRATIONS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   VinFast    │  │    Tesla     │  │  Payment     │             │
│  │     API      │  │     API      │  │  Gateways    │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  Government  │  │   SMS/Email  │  │  Google Maps │             │
│  │  Databases   │  │   Services   │  │     API      │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Architecture Principles

### 1. Separation of Concerns
- Each microservice has a **single responsibility**
- Clear boundaries between services
- Services communicate via well-defined APIs

### 2. Scalability by Design
- **Horizontal scaling**: Add more instances to handle load
- **Stateless services**: Session state in Redis, not in-memory
- **Database read replicas**: Distribute read load
- **Async processing**: Use message queues for heavy operations

### 3. Resilience & Fault Tolerance
- **Circuit breakers**: Prevent cascade failures (Hystrix pattern)
- **Retries with exponential backoff**: Handle transient failures
- **Graceful degradation**: Core functions work even if non-critical services fail
- **Health checks**: Automatic unhealthy instance removal

### 4. Security First
- **Defense in depth**: Multiple security layers
- **Zero trust**: Verify every request
- **Encryption everywhere**: At rest and in transit
- **Least privilege**: Minimal permissions for each service

### 5. Data Consistency
- **ACID transactions**: For financial operations
- **Eventual consistency**: For non-critical data
- **Idempotency**: Safe to retry operations
- **Saga pattern**: Distributed transactions across services

### 6. Observability
- **Comprehensive logging**: Structured logs (JSON)
- **Distributed tracing**: Track requests across services
- **Metrics**: Business and technical KPIs
- **Alerting**: Proactive issue detection

---

## Component Architecture

### Frontend Applications

#### 1. Web Application (React + Next.js)

```
src/
├── components/          # Reusable UI components
│   ├── common/         # Buttons, inputs, cards
│   ├── marketplace/    # Listing cards, search
│   ├── dashboard/      # Charts, widgets
│   └── forms/          # Form components
├── pages/              # Next.js pages (SSR/SSG)
│   ├── index.tsx       # Landing page
│   ├── dashboard/      # User dashboard
│   ├── marketplace/    # Marketplace pages
│   ├── transactions/   # Transaction history
│   └── admin/          # Admin portal
├── services/           # API client services
│   ├── api.ts          # Axios instance
│   ├── auth.ts         # Authentication
│   ├── marketplace.ts  # Marketplace API
│   └── wallet.ts       # Wallet API
├── state/              # State management (Redux/Zustand)
│   ├── slices/         # Redux slices
│   └── store.ts        # Store configuration
├── hooks/              # Custom React hooks
├── utils/              # Utility functions
└── styles/             # Global styles
```

**Key Features**:
- Server-Side Rendering (SSR) for SEO
- Progressive Web App (PWA) capabilities
- Code splitting for performance
- Responsive design (mobile-first)

---

#### 2. Mobile PWA

**Features**:
- **Offline mode**: Service workers for offline functionality
- **Push notifications**: Real-time alerts
- **Install prompt**: Add to home screen
- **Camera access**: KYC document upload
- **Geolocation**: Trip validation

**Technologies**:
- Workbox for service worker management
- Web Push API for notifications
- IndexedDB for offline storage

---

### Backend Services

#### 1. User Service

**Responsibilities**:
- User registration and authentication
- Profile management
- KYC verification workflow
- Session management
- 2FA implementation

**Endpoints**:
- `POST /auth/register`
- `POST /auth/login`
- `GET /users/me`
- `PATCH /users/me`
- `POST /users/me/kyc`

**Database Tables**:
- users
- user_kyc_documents
- user_sessions
- user_2fa_secrets

**External Integrations**:
- OAuth providers (Google, Facebook, Apple)
- SMS provider (Twilio)
- Email service (SendGrid)

---

#### 2. Vehicle Service

**Responsibilities**:
- Vehicle registration and management
- OEM API integration (VinFast, Tesla)
- OBD-II device pairing
- Trip data synchronization
- Data validation

**Endpoints**:
- `POST /vehicles`
- `GET /vehicles/{id}`
- `POST /vehicles/{id}/sync`
- `GET /vehicles/{id}/trips`

**Database Tables**:
- vehicles
- trips (partitioned)
- trip_sync_jobs

**External Integrations**:
- VinFast API (OAuth 2.0)
- Tesla API (OAuth 2.0)
- Government vehicle database (optional)

**Async Jobs**:
- Daily trip sync (scheduled)
- Manual sync on demand
- Data validation pipeline

---

#### 3. Carbon Credit Service

**Responsibilities**:
- CO₂ calculation engine
- Carbon credit wallet management
- Credit issuance and tracking
- Transaction history

**Endpoints**:
- `GET /carbon-credits/wallet`
- `GET /carbon-credits`
- `POST /carbon-credits/verification-request`

**Database Tables**:
- carbon_credits
- credit_transactions

**Calculation Engine**:
```java
@Service
public class CarbonCalculationService {
    
    private static final double ICE_EMISSION_FACTOR = 0.15; // kg CO2/km
    private static final double EV_EMISSION_FACTOR = 0.05;  // kg CO2/km
    
    /**
     * Calculate CO2 saved from EV trip
     * 
     * @param distanceKm Trip distance in kilometers
     * @return CO2 saved in kilograms
     */
    public BigDecimal calculateCo2Saved(BigDecimal distanceKm) {
        BigDecimal netReduction = BigDecimal.valueOf(ICE_EMISSION_FACTOR - EV_EMISSION_FACTOR);
        BigDecimal co2SavedKg = distanceKm.multiply(netReduction);
        return co2SavedKg.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Convert CO2 kg to carbon credit tons
     * 
     * @param co2Kg CO2 in kilograms
     * @return Carbon credits in tons
     */
    public BigDecimal co2ToCredits(BigDecimal co2Kg) {
        return co2Kg.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
    }
}
```

---

#### 4. Verification Service (CVA)

**Responsibilities**:
- Verification request management
- Auditor assignment
- Data validation tools
- Credit issuance
- Audit trail logging

**Endpoints**:
- `GET /verifications/queue`
- `GET /verifications/{id}`
- `POST /verifications/{id}/approve`
- `POST /verifications/{id}/reject`

**Database Tables**:
- verification_requests
- audit_logs (partitioned)

**Workflow Engine**:
- State machine for verification status
- SLA tracking and alerts
- Automated assignment algorithm

---

#### 5. Marketplace Service

**Responsibilities**:
- Listing management (create, update, delete)
- Search and discovery
- Auction bidding engine
- Price recommendation (ML)
- Watchlist and saved searches

**Endpoints**:
- `GET /marketplace/listings`
- `POST /marketplace/listings`
- `POST /marketplace/listings/{id}/bids`
- `GET /marketplace/listings/price-recommendation`

**Database Tables**:
- listings
- auction_bids
- saved_searches
- watchlist

**Search Engine** (Elasticsearch):
```json
{
  "mappings": {
    "properties": {
      "listing_id": { "type": "keyword" },
      "seller_name": { "type": "text" },
      "credit_amount_tons": { "type": "float" },
      "price_per_ton_vnd": { "type": "integer" },
      "region": { "type": "keyword" },
      "status": { "type": "keyword" },
      "vintage_year": { "type": "integer" },
      "created_at": { "type": "date" }
    }
  }
}
```

**Real-time Auction**:
- WebSocket for live bid updates
- Redis pub/sub for bid broadcasting
- Distributed locks to prevent race conditions

---

#### 6. Transaction Service

**Responsibilities**:
- Transaction orchestration
- Escrow management
- Settlement processing
- Refund handling
- Transaction history

**Endpoints**:
- `POST /transactions`
- `GET /transactions/{id}`
- `POST /transactions/{id}/refund`

**Database Tables**:
- transactions
- escrow_accounts

**Transaction Flow** (Saga Pattern):
```
1. Create Transaction (PENDING)
2. Initiate Payment → Payment Service
3. Lock Credits in Escrow
4. Wait for Payment Confirmation
5. Transfer Credits to Buyer
6. Generate Certificate → Certificate Service
7. Release Payment to Seller
8. Update Transaction Status (COMPLETED)
9. Send Notifications → Notification Service

Compensating Transactions (if failure):
- Refund Payment
- Unlock Credits
- Update Transaction Status (FAILED)
```

---

#### 7. Payment Service

**Responsibilities**:
- Payment gateway integration
- Payment processing
- Webhook handling
- Reconciliation

**Endpoints**:
- `POST /payments`
- `GET /payments/{id}`
- `POST /webhooks/momo`
- `POST /webhooks/vnpay`

**Database Tables**:
- payments
- payouts

**Payment Gateways**:
- **MoMo**: E-wallet (1.5% fee)
- **VNPay**: E-wallet (1.8% fee)
- **Stripe**: International cards (2.9% + fee)
- **Bank Transfer**: Napas (flat fee)

**Security**:
- PCI-DSS compliance (use tokenization)
- Webhook signature verification
- Idempotency keys for retries

---

#### 8. Certificate Service

**Responsibilities**:
- Certificate generation (PDF)
- QR code generation
- Public verification portal
- Blockchain integration (optional)

**Endpoints**:
- `GET /certificates/{id}`
- `GET /certificates/{id}/download`
- `POST /public/certificates/verify`

**Database Tables**:
- certificates
- certificate_verifications

**Certificate Generation**:
- Template: HTML → PDF (Puppeteer/wkhtmltopdf)
- QR Code: Contains certificate number + verification URL
- Digital Signature: Sign PDF with CVA private key
- Storage: S3 with CloudFront CDN

---

#### 9. Notification Service

**Responsibilities**:
- Multi-channel notifications (email, SMS, push, in-app)
- Template management
- Delivery tracking
- Notification preferences

**Endpoints**:
- `POST /notifications/send`
- `GET /notifications` (user notifications)
- `PATCH /notifications/{id}/read`

**Database Tables**:
- notifications
- notification_templates

**Message Queue** (RabbitMQ):
```
Producer (Services) → Exchange → Queues → Consumer (Notification Workers)

Queues:
- email_queue (priority: low)
- sms_queue (priority: high)
- push_queue (priority: medium)
- in_app_queue (priority: low)
```

**Providers**:
- Email: SendGrid, AWS SES
- SMS: Twilio, local provider
- Push: Firebase Cloud Messaging (FCM)

---

#### 10. Analytics Service

**Responsibilities**:
- Data aggregation
- Report generation
- KPI calculation
- Data export

**Endpoints**:
- `GET /analytics/dashboard`
- `POST /analytics/reports`
- `GET /analytics/export`

**Database**:
- TimescaleDB (time-series extension on PostgreSQL)
- Materialized views for pre-computed metrics

**Data Pipeline**:
```
Transactional DB → ETL Jobs → Analytics DB → BI Tools

ETL Schedule:
- Real-time: Critical metrics (updated every 1 min)
- Hourly: Usage metrics
- Daily: Financial reports
- Weekly: Trend analysis
```

---

## Data Flow

### Critical Flow 1: EV Owner Registration to First Carbon Credit Sale

```
┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 1: Registration & Onboarding                                  │
└─────────────────────────────────────────────────────────────────────┘

1. User visits website
2. Clicks "Register as EV Owner"
3. Submits registration form
   → User Service: Create account
   → Notification Service: Send verification email
4. User verifies email (clicks link)
   → User Service: Mark email as verified
5. User logs in
   → User Service: Generate JWT token
6. User adds vehicle
   → Vehicle Service: Register vehicle
   → External: Verify VIN (optional)
7. User connects data source (VinFast API)
   → Vehicle Service: OAuth flow
   → Store encrypted API credentials

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 2: Trip Data Collection                                       │
└─────────────────────────────────────────────────────────────────────┘

8. Daily sync job triggered (2:00 AM)
   → Vehicle Service: Fetch trips from VinFast API
   → Trip Service: Validate and store trips
   → Carbon Credit Service: Calculate CO₂ saved
9. User views dashboard
   → User sees total CO₂ saved
   → Credits accumulate but are PENDING verification

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 3: Verification                                               │
└─────────────────────────────────────────────────────────────────────┘

10. User submits verification request
    → Verification Service: Create request
    → Assign to CVA auditor
    → Notification: Alert auditor
11. CVA auditor reviews request
    → Download trip data
    → Run validation tools
    → Recalculate CO₂
12. CVA approves verification
    → Carbon Credit Service: Issue credits
    → Generate serial numbers
    → Transfer to user's wallet
    → Notification: Alert user

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 4: Listing & Sale                                             │
└─────────────────────────────────────────────────────────────────────┘

13. User creates listing
    → Marketplace Service: Create listing
    → Lock credits (change status to LISTED)
    → Elasticsearch: Index listing for search
14. Buyer searches marketplace
    → Elasticsearch: Return matching listings
15. Buyer purchases credits
    → Transaction Service: Create transaction
    → Payment Service: Process payment
    → Escrow: Hold payment
16. Payment confirmed
    → Carbon Credit Service: Transfer credits
    → Certificate Service: Generate certificate
    → Transaction Service: Release payment to seller
    → Notification: Alert both parties
17. Transaction complete ✓
```

---

### Critical Flow 2: Auction Workflow

```
1. Seller creates auction listing
   → Marketplace Service: Create listing (type: auction)
   → Set starting price, reserve price, duration
   → Schedule auction end job

2. Buyers place bids
   → WebSocket: Real-time bid updates
   → Marketplace Service: Validate bid (minimum increment)
   → Redis: Update current bid (atomic operation)
   → RabbitMQ: Broadcast bid to all watchers
   → Notification: Alert outbid users

3. Auto-bid mechanism
   → User sets max auto-bid amount
   → When outbid, system automatically bids up to max
   → Increment by minimum amount

4. Auction ending soon (<5 min remaining)
   → New bid extends auction by 10 minutes
   → Prevent last-second sniping

5. Auction ends
   → Cron job or scheduled task
   → Marketplace Service: Determine winner
   → Notification: Alert winner (24h to pay)
   → If no payment, offer to 2nd highest bidder

6. Winner completes payment
   → Follow standard transaction flow (Flow 1, steps 15-17)
```

---

## Infrastructure Architecture

### Cloud Provider: AWS (Primary), Multi-cloud capable

```
┌─────────────────────────────────────────────────────────────────────┐
│                          PRODUCTION ENVIRONMENT                      │
│                         Region: ap-southeast-1 (Singapore)           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ VPC (Virtual Private Cloud)                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Public Subnet (AZ-1)       Public Subnet (AZ-2)           │    │
│  ├────────────────────────────────────────────────────────────┤    │
│  │  - Application Load Balancer (ALB)                         │    │
│  │  - NAT Gateway                                             │    │
│  │  - Bastion Host                                            │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Private Subnet (AZ-1)      Private Subnet (AZ-2)          │    │
│  ├────────────────────────────────────────────────────────────┤    │
│  │  - ECS/EKS Cluster (Microservices)                         │    │
│  │  - Auto Scaling Groups                                     │    │
│  │  - Service Mesh (Consul/Istio)                             │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Data Subnet (AZ-1)         Data Subnet (AZ-2)             │    │
│  ├────────────────────────────────────────────────────────────┤    │
│  │  - RDS PostgreSQL (Primary + Replica)                      │    │
│  │  - ElastiCache Redis Cluster                               │    │
│  │  - Elasticsearch Cluster                                   │    │
│  │  - RabbitMQ Cluster (Amazon MQ)                            │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ External Services                                                    │
├─────────────────────────────────────────────────────────────────────┤
│  - S3: File storage, static assets, backups                         │
│  - CloudFront: CDN for global delivery                              │
│  - Route 53: DNS management                                         │
│  - Certificate Manager: SSL/TLS certificates                        │
│  - Secrets Manager: API keys, credentials                           │
│  - CloudWatch: Monitoring, logs, alarms                             │
│  - SQS/SNS: Additional messaging (optional)                         │
│  - Lambda: Serverless functions (image processing, etc.)            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ Disaster Recovery Region: us-west-2 (Oregon)                        │
├─────────────────────────────────────────────────────────────────────┤
│  - RDS Read Replica (cross-region)                                  │
│  - S3 replication                                                    │
│  - Standby infrastructure (can be activated)                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 1: Edge Security                                              │
├─────────────────────────────────────────────────────────────────────┤
│  ✓ CloudFlare / AWS WAF (Web Application Firewall)                  │
│  ✓ DDoS Protection                                                   │
│  ✓ Rate Limiting                                                     │
│  ✓ Geo-blocking (if needed)                                         │
│  ✓ Bot detection                                                     │
└─────────────────────────────────────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 2: Network Security                                           │
├─────────────────────────────────────────────────────────────────────┤
│  ✓ VPC with private subnets                                         │
│  ✓ Security Groups (firewall rules)                                 │
│  ✓ Network ACLs                                                      │
│  ✓ VPN for admin access                                             │
│  ✓ No direct internet access for databases                          │
└─────────────────────────────────────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 3: Application Security                                       │
├─────────────────────────────────────────────────────────────────────┤
│  ✓ JWT authentication                                                │
│  ✓ OAuth 2.0 for third-party integrations                           │
│  ✓ 2FA for high-value operations                                    │
│  ✓ RBAC (Role-Based Access Control)                                 │
│  ✓ Input validation & sanitization                                  │
│  ✓ OWASP Top 10 protection                                          │
│  ✓ API rate limiting per user                                       │
└─────────────────────────────────────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 4: Data Security                                              │
├─────────────────────────────────────────────────────────────────────┤
│  ✓ Encryption at rest (AES-256)                                     │
│  ✓ Encryption in transit (TLS 1.3)                                  │
│  ✓ Database encryption                                               │
│  ✓ Field-level encryption for PII                                   │
│  ✓ Secure key management (AWS KMS)                                  │
└─────────────────────────────────────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 5: Monitoring & Response                                      │
├─────────────────────────────────────────────────────────────────────┤
│  ✓ Security Information and Event Management (SIEM)                 │
│  ✓ Intrusion Detection System (IDS)                                 │
│  ✓ Audit logging (all actions logged)                               │
│  ✓ Anomaly detection (ML-based)                                     │
│  ✓ Incident response plan                                           │
│  ✓ Regular security audits                                          │
└─────────────────────────────────────────────────────────────────────┘
```

### Authentication & Authorization Flow

```
1. User Login
   ↓
2. User Service validates credentials
   ↓
3. Generate JWT token (15-min expiry) + Refresh token (7-day expiry)
   ↓
4. User makes API request with Bearer token
   ↓
5. API Gateway validates JWT signature
   ↓
6. Extract user_id and role from JWT
   ↓
7. Check permissions for requested resource
   ↓
8. Allow/Deny request
```

**JWT Payload**:
```json
{
  "sub": "user_id_uuid",
  "email": "user@example.com",
  "role": "evowner",
  "kyc_level": 1,
  "iat": 1635177600,
  "exp": 1635181200
}
```

---

## Deployment Architecture

### Containerization (Docker + Kubernetes)

**Dockerfile Example** (Java Spring Boot service):
```dockerfile
# Multi-stage build for optimization
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Kubernetes Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: marketplace-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: marketplace-service
  template:
    metadata:
      labels:
        app: marketplace-service
    spec:
      containers:
      - name: marketplace-service
        image: carbonmarketplace/marketplace-service:1.0.0
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
```

### CI/CD Pipeline (GitHub Actions)

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn test
      - name: Run code quality check
        run: mvn checkstyle:check
      - name: Security scan
        run: mvn dependency-check:check

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build with Maven
        run: mvn clean package -DskipTests
      - name: Build Docker image
        run: docker build -t carbonmarketplace/marketplace-service:${{ github.sha }} .
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin
          docker push carbonmarketplace/marketplace-service:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EKS
        run: |
          kubectl set image deployment/marketplace-service \
            marketplace-service=carbonmarketplace/marketplace-service:${{ github.sha }}
          kubectl rollout status deployment/marketplace-service
```

---

## Scalability & Performance

### Horizontal Scaling Strategy

| Component | Scaling Trigger | Min Instances | Max Instances |
|-----------|-----------------|---------------|---------------|
| API Gateway | CPU >70% | 2 | 10 |
| User Service | CPU >70% | 2 | 8 |
| Marketplace Service | CPU >70% | 3 | 15 |
| Transaction Service | Queue depth >100 | 2 | 10 |
| Payment Service | CPU >70% | 2 | 8 |
| Notification Workers | Queue depth >1000 | 2 | 20 |

### Caching Strategy

**Layer 1: Browser Cache**
- Static assets: 1 year
- API responses: No cache (sensitive data)

**Layer 2: CDN Cache (CloudFront)**
- Images, CSS, JS: 30 days
- Static pages: 1 hour
- API: No cache

**Layer 3: Application Cache (Redis)**
```
- User sessions: 30 minutes
- Platform settings: 1 hour
- Marketplace listings: 5 minutes
- Price recommendations: 1 hour
- User wallet balance: 1 minute
```

**Layer 4: Database Query Cache**
- Materialized views refreshed every 15 minutes
- Read replicas for read scaling

### Database Optimization

**1. Indexing**
- All foreign keys indexed
- Composite indexes for common queries
- Partial indexes for filtered queries

**2. Partitioning**
- `trips`: Monthly partitions
- `audit_logs`: Monthly partitions
- `notifications`: Monthly partitions

**3. Connection Pooling**
- Max connections: 100
- Connection timeout: 30s
- Idle timeout: 10min

**4. Read Replicas**
- 2 replicas in same region
- 1 replica in DR region
- Read-write splitting in application

---

## Monitoring & Observability

### Metrics Collection (Prometheus + Grafana)

**Application Metrics**:
- Request rate (requests/second)
- Error rate (%)
- Response time (p50, p95, p99)
- Active users
- Transactions per minute

**Business Metrics**:
- GMV (Gross Merchandise Value)
- New user signups
- Carbon credits traded
- Average transaction value
- Conversion funnel

**Infrastructure Metrics**:
- CPU, Memory, Disk usage
- Network I/O
- Database connections
- Queue depth

### Logging (ELK Stack)

**Structured Logging**:
```json
{
  "timestamp": "2025-10-25T10:30:00Z",
  "level": "INFO",
  "service": "marketplace-service",
  "trace_id": "abc123",
  "user_id": "uuid",
  "action": "create_listing",
  "message": "Listing created successfully",
  "metadata": {
    "listing_id": "uuid",
    "credit_amount": 1.5,
    "price": 2500000
  }
}
```

### Distributed Tracing (Jaeger)

Trace requests across microservices:
```
User Request (trace_id: abc123)
├─ API Gateway (span: 1) - 50ms
├─ Marketplace Service (span: 2) - 120ms
│  ├─ Database Query (span: 2.1) - 15ms
│  └─ Redis Get (span: 2.2) - 2ms
├─ Carbon Credit Service (span: 3) - 80ms
│  ├─ Database Query (span: 3.1) - 20ms
│  └─ Validation (span: 3.2) - 30ms
└─ Response - Total: 250ms
```

### Alerting (PagerDuty / OpsGenie)

**Critical Alerts** (Immediate Response):
- Service down (uptime <99%)
- Payment gateway failure
- Database unavailable
- Error rate >5%

**Warning Alerts** (Next Business Day):
- High response time (p95 >2s)
- High queue depth
- Low disk space (<20%)
- Failed background jobs

---

## Disaster Recovery

### RTO & RPO

- **RTO** (Recovery Time Objective): 4 hours
- **RPO** (Recovery Point Objective): 1 hour

### Backup Strategy

**1. Database Backups**
- Continuous WAL archiving to S3
- Daily snapshots
- Cross-region replication

**2. File Storage**
- S3 versioning enabled
- Cross-region replication
- Lifecycle policies (transition to Glacier after 90 days)

**3. Configuration**
- Infrastructure as Code (Terraform)
- Git repository for all config
- Secrets in AWS Secrets Manager

### Disaster Recovery Plan

**Scenario: Region Failure**

1. **Detection** (0-15 min)
   - Monitoring alerts triggered
   - On-call engineer notified
   - Assess situation

2. **Decision** (15-30 min)
   - Confirm region failure
   - Initiate DR procedure
   - Notify stakeholders

3. **Failover** (30-90 min)
   - Promote DR region read replica to primary
   - Update DNS to point to DR region
   - Start services in DR region
   - Verify functionality

4. **Verification** (90-120 min)
   - Test critical user flows
   - Monitor error rates
   - Confirm data integrity

5. **Communication** (Ongoing)
   - Status page updates
   - Customer notifications
   - Internal updates

**Total Recovery Time**: <2 hours (well within 4-hour RTO)

---

## Technology Stack Summary

### Frontend
- **Framework**: React 18 + Next.js 13
- **Language**: TypeScript
- **State**: Redux Toolkit / Zustand
- **UI**: Material-UI / Tailwind CSS
- **Charts**: Recharts / Chart.js
- **Forms**: React Hook Form + Yup
- **HTTP Client**: Axios

### Backend
- **Language**: Java 17+ / Kotlin
- **Framework**: Spring Boot 3.x (Microservices)
- **API**: Spring Web MVC / Spring WebFlux
- **Security**: Spring Security (JWT, OAuth 2.0)
- **Data Access**: Spring Data JPA / Hibernate
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Testing**: JUnit 5 + Mockito + Spring Boot Test
- **Build Tool**: Maven / Gradle
- **Real-time**: Spring WebSocket + STOMP

### Databases
- **Primary**: PostgreSQL 14
- **Cache**: Redis 7
- **Search**: Elasticsearch 8
- **Time-series**: TimescaleDB
- **Message Queue**: RabbitMQ / Amazon MQ

### Infrastructure
- **Cloud**: AWS (primary)
- **Container**: Docker
- **Orchestration**: Kubernetes (EKS)
- **CI/CD**: GitHub Actions / Jenkins
- **IaC**: Terraform
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK Stack
- **Tracing**: Jaeger / Spring Cloud Sleuth
- **Service Discovery**: Spring Cloud Netflix Eureka / Consul
- **API Gateway**: Spring Cloud Gateway / Kong

---

**Document Version**: 1.0  
**Last Updated**: October 25, 2025  
**Architecture Review Cycle**: Quarterly  
**Next Review**: January 2026

