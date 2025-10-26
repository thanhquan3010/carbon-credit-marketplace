# Cursor Implementation Instructions
## Step-by-Step Guide for Carbon Credit Marketplace

**Version**: 1.0  
**Last Updated**: October 26, 2025  
**For**: Cursor IDE with AI Assistant

---

## 🚀 Quick Start Checklist

```bash
# Initial Setup Commands
git clone <repository>
cd carbon-credit-marketplace

# Backend Setup
cd backend
mvn clean install
mvn spring-boot:run

# Frontend Setup
cd ../frontend
npm install
npm run dev

# Database Setup
docker-compose up -d postgres redis
psql -U postgres -d carbon_marketplace -f infrastructure/postgres/init.sql
```

---

## 📋 Pre-Implementation Setup

### 1. Environment Configuration

Create `.env` files:

**backend/.env**:
```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbon_marketplace
DB_USER=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret
JWT_EXPIRY=3600

# Payment Gateways
MOMO_API_KEY=xxx
VNPAY_API_KEY=xxx
STRIPE_SECRET_KEY=xxx

# Email
SENDGRID_API_KEY=xxx

# SMS
TWILIO_ACCOUNT_SID=xxx
TWILIO_AUTH_TOKEN=xxx
```

**frontend/.env.local**:
```properties
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

### 2. Read Documentation Order

1. **REQUIREMENTS.md** - Understand all functional requirements
2. **DATABASE_SCHEMA.md** - Review table structures
3. **API_SPECS.md** - Understand endpoints to implement
4. **ARCHITECTURE.md** - Understand service dependencies
5. **USER_STORIES.md** - Understand user flows

---

## Phase 1: Core Foundation Services

### Service 1.1: User Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (FR-EVO-001, FR-ADM-001) and DATABASE_SCHEMA.md (users table),
create the User Service with:
1. User entity matching the database schema
2. Registration endpoint with email/phone validation
3. JWT authentication with refresh tokens
4. KYC document upload to S3
5. Password reset flow with email
6. Unit tests with >80% coverage

Follow Spring Boot best practices with proper layering:
- Controller -> Service -> Repository pattern
- DTOs for request/response
- Global exception handling
- Input validation using @Valid
```

**Implementation Checklist**:
- [ ] Create project structure:
  ```
  user-service/
  ├── src/main/java/com/carbonmarketplace/userservice/
  │   ├── controller/
  │   │   ├── AuthController.java
  │   │   └── UserController.java
  │   ├── service/
  │   │   ├── UserService.java
  │   │   ├── AuthService.java
  │   │   └── KycService.java
  │   ├── repository/
  │   │   └── UserRepository.java
  │   ├── entity/
  │   │   ├── User.java
  │   │   └── KycDocument.java
  │   ├── dto/
  │   │   ├── RegisterRequest.java
  │   │   ├── LoginRequest.java
  │   │   └── UserResponse.java
  │   ├── security/
  │   │   ├── JwtTokenProvider.java
  │   │   └── JwtAuthenticationFilter.java
  │   └── exception/
  │       └── GlobalExceptionHandler.java
  ```

- [ ] Implement endpoints:
  ```java
  POST /auth/register
  POST /auth/login
  POST /auth/refresh
  POST /auth/logout
  GET  /users/me
  PATCH /users/me
  POST /users/me/kyc
  POST /users/me/2fa/enable
  ```

- [ ] Add validation rules:
  - Email: Valid format, unique
  - Phone: Vietnam format (+84)
  - Password: Min 8 chars, mixed case, numbers, special
  - KYC files: Max 5MB, image formats only

**Success Criteria**:
- ✅ Registration completes in < 2 minutes
- ✅ JWT tokens working with 15-min expiry
- ✅ 2FA setup with TOTP
- ✅ All endpoints return proper HTTP codes

---

### Service 1.2: Vehicle Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (FR-EVO-001, FR-EVO-002) and DATABASE_SCHEMA.md (vehicles, trips tables),
create Vehicle Service with:
1. Vehicle registration with VIN validation
2. OAuth integration with VinFast API (mock for now)
3. Trip data sync with batch processing
4. CSV upload parser with validation
5. Scheduled job for daily sync at 2 AM
6. Data quality scoring algorithm

Implement trip partitioning by month for scalability.
```

**Key Files to Create**:
- `VehicleController.java` - REST endpoints
- `TripSyncService.java` - Sync logic
- `VinFastApiClient.java` - External API
- `CsvParser.java` - File upload handler
- `TripValidator.java` - Data validation
- `SyncScheduler.java` - Cron jobs

**Validation Rules**:
```java
// TripValidator.java
public class TripValidator {
    public ValidationResult validate(Trip trip) {
        // Distance: 0.1 to 500 km
        if (trip.getDistanceKm() < 0.1 || trip.getDistanceKm() > 500) {
            return ValidationResult.invalid("Invalid distance");
        }
        
        // Average speed: < 180 km/h
        if (trip.getAvgSpeed() > 180) {
            return ValidationResult.invalid("Speed too high");
        }
        
        // Energy efficiency: 10-25 kWh/100km
        double efficiency = (trip.getEnergyKwh() / trip.getDistanceKm()) * 100;
        if (efficiency < 10 || efficiency > 25) {
            return ValidationResult.warning("Unusual energy efficiency");
        }
        
        // Trip duration: > 2 minutes
        long durationMinutes = Duration.between(
            trip.getStartTime(), 
            trip.getEndTime()
        ).toMinutes();
        if (durationMinutes < 2) {
            return ValidationResult.invalid("Trip too short");
        }
        
        return ValidationResult.valid();
    }
}
```

---

### Service 1.3: Carbon Credit Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (BR-001) and the calculation formula,
create Carbon Credit Service with:
1. CO2 calculation engine using the formula:
   CO2 Saved (kg) = Distance (km) × (0.15 - 0.05)
2. Carbon wallet management with balance tracking
3. Verification request submission
4. Credit transaction history

Ensure calculation accuracy within ±2% tolerance.
```

**Core Calculation Implementation**:
```java
@Service
public class CarbonCalculationService {
    
    private static final BigDecimal ICE_EMISSION_FACTOR = new BigDecimal("0.15");
    private static final BigDecimal EV_EMISSION_FACTOR = new BigDecimal("0.05");
    private static final BigDecimal KG_TO_TONS = new BigDecimal("1000");
    
    public CarbonCalculationResult calculate(List<Trip> trips) {
        BigDecimal totalDistanceKm = trips.stream()
            .map(Trip::getDistanceKm)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netReductionFactor = ICE_EMISSION_FACTOR
            .subtract(EV_EMISSION_FACTOR);
        
        BigDecimal co2SavedKg = totalDistanceKm
            .multiply(netReductionFactor)
            .setScale(4, RoundingMode.HALF_UP);
        
        BigDecimal creditTons = co2SavedKg
            .divide(KG_TO_TONS, 4, RoundingMode.HALF_UP);
        
        return CarbonCalculationResult.builder()
            .totalDistanceKm(totalDistanceKm)
            .co2SavedKg(co2SavedKg)
            .creditAmountTons(creditTons)
            .methodology("CDM ACM0018")
            .calculationDate(LocalDateTime.now())
            .build();
    }
}
```

---

### Service 1.4: Notification Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (NFR-U-003) and DATABASE_SCHEMA.md (notifications table),
create Notification Service with:
1. Multi-channel support (email, SMS, in-app, push)
2. Template management with variables
3. RabbitMQ integration for async processing
4. Delivery tracking and retry logic
5. SendGrid for email, Twilio for SMS

Set up different priority queues for time-sensitive notifications.
```

**RabbitMQ Configuration**:
```java
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable("email.queue")
            .withArgument("x-priority", 5)
            .build();
    }
    
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable("sms.queue")
            .withArgument("x-priority", 10)
            .build();
    }
    
    @Bean
    public Queue inAppQueue() {
        return QueueBuilder.durable("in-app.queue")
            .withArgument("x-priority", 3)
            .build();
    }
}
```

---

## Phase 2: Marketplace Engine

### Service 2.1: Verification Service

**Cursor Prompt Template**:
```
Based on USER_STORIES.md (US-020, US-023) and DATABASE_SCHEMA.md (verification_requests),
create Verification Service with:
1. CVA auditor portal with queue management
2. Data validation tools with anomaly detection
3. Auto-assignment algorithm based on workload
4. Credit issuance with serial number generation
5. Immutable audit trail using hash chain

Serial number format: VN-EV-YYYY-CVA-SEQUENCE
```

**Anomaly Detection**:
```java
@Service
public class AnomalyDetectionService {
    
    public List<Anomaly> detectAnomalies(List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Check for impossible speeds
        trips.stream()
            .filter(t -> t.getAvgSpeed() > 150)
            .forEach(t -> anomalies.add(
                new Anomaly(t.getTripId(), "HIGH_SPEED", "critical")
            ));
        
        // Check for duplicate trips
        Map<String, Long> tripCounts = trips.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStartTime() + "-" + t.getEndTime(),
                Collectors.counting()
            ));
        
        tripCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .forEach(e -> anomalies.add(
                new Anomaly(null, "DUPLICATE_TRIP", "warning")
            ));
        
        // Check energy efficiency outliers
        double avgEfficiency = calculateAvgEfficiency(trips);
        trips.stream()
            .filter(t -> Math.abs(getEfficiency(t) - avgEfficiency) > 50)
            .forEach(t -> anomalies.add(
                new Anomaly(t.getTripId(), "EFFICIENCY_OUTLIER", "warning")
            ));
        
        return anomalies;
    }
}
```

---

### Service 2.2: Marketplace Service

**Cursor Prompt Template**:
```
Based on USER_STORIES.md (US-010 to US-017) and API_SPECS.md (marketplace endpoints),
create Marketplace Service with:
1. Listing CRUD with fixed price and auction types
2. Elasticsearch integration for search
3. WebSocket for real-time auction updates
4. Basic price recommendation using historical data
5. Cart with 30-minute reservation

Implement auto-extension logic: extend by 10 min if bid in last 5 min.
```

**WebSocket Configuration**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}

// Auction Service
@Service
public class AuctionService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void placeBid(String listingId, BidRequest request) {
        // Validate bid
        // Save to database
        // Broadcast to all watchers
        messagingTemplate.convertAndSend(
            "/topic/auction/" + listingId,
            new BidUpdate(request.getBidAmount(), request.getBidderId())
        );
        
        // Check for auto-extension
        if (shouldExtendAuction(listing)) {
            extendAuction(listing, 10, TimeUnit.MINUTES);
        }
    }
}
```

---

## Phase 3: Financial & Compliance

### Service 3.1: Payment Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (BR-004) and API_SPECS.md (payment endpoints),
create Payment Service with:
1. MoMo integration with webhook handling
2. VNPay integration with IPN callback
3. Bank transfer with virtual account numbers
4. Idempotency keys for safe retries
5. Webhook signature verification

Implement PCI-DSS compliance by never storing card details.
```

**Payment Gateway Integration**:
```java
@Service
public class MoMoPaymentService implements PaymentGateway {
    
    @Value("${momo.partner.code}")
    private String partnerCode;
    
    @Value("${momo.secret.key}")
    private String secretKey;
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        String requestId = UUID.randomUUID().toString();
        String orderId = "CARBON-" + request.getTransactionId();
        
        Map<String, Object> data = new HashMap<>();
        data.put("partnerCode", partnerCode);
        data.put("requestId", requestId);
        data.put("orderId", orderId);
        data.put("amount", request.getAmount());
        data.put("orderInfo", "Carbon Credit Purchase");
        data.put("redirectUrl", request.getRedirectUrl());
        data.put("ipnUrl", request.getCallbackUrl());
        data.put("requestType", "captureWallet");
        
        String signature = generateSignature(data);
        data.put("signature", signature);
        
        // Call MoMo API
        return callMoMoApi(data);
    }
    
    @Override
    public boolean verifyWebhook(String payload, String signature) {
        String expectedSignature = generateSignature(payload);
        return MessageDigest.isEqual(
            signature.getBytes(),
            expectedSignature.getBytes()
        );
    }
}
```

---

### Service 3.2: Transaction Service

**Cursor Prompt Template**:
```
Based on ARCHITECTURE.md (Transaction Service Saga Pattern) and DATABASE_SCHEMA.md,
create Transaction Service with:
1. Saga pattern for distributed transactions
2. Escrow account management
3. Automatic settlement T+2
4. Refund workflow with approval
5. Compensating transactions for failures

Implement atomic operations using database transactions.
```

**Saga Implementation**:
```java
@Service
@Transactional
public class TransactionSagaService {
    
    public TransactionResult executPurchase(PurchaseRequest request) {
        SagaTransaction saga = new SagaTransaction();
        
        try {
            // Step 1: Create transaction record
            Transaction transaction = saga.execute(
                () -> createTransaction(request),
                () -> deleteTransaction(request.getTransactionId())
            );
            
            // Step 2: Process payment
            Payment payment = saga.execute(
                () -> processPayment(transaction),
                () -> refundPayment(payment.getPaymentId())
            );
            
            // Step 3: Lock credits in escrow
            EscrowLock escrow = saga.execute(
                () -> lockCreditsInEscrow(transaction),
                () -> releaseCreditsFromEscrow(escrow.getEscrowId())
            );
            
            // Step 4: Transfer credits to buyer
            CreditTransfer transfer = saga.execute(
                () -> transferCredits(transaction),
                () -> reverseTransfer(transfer.getTransferId())
            );
            
            // Step 5: Generate certificate
            Certificate certificate = saga.execute(
                () -> generateCertificate(transaction),
                () -> voidCertificate(certificate.getCertificateId())
            );
            
            // Step 6: Release payment to seller
            Payout payout = saga.execute(
                () -> releasePayout(transaction),
                () -> cancelPayout(payout.getPayoutId())
            );
            
            // Step 7: Send notifications
            saga.execute(
                () -> sendNotifications(transaction),
                () -> {} // No compensation needed
            );
            
            saga.commit();
            return TransactionResult.success(transaction);
            
        } catch (Exception e) {
            saga.rollback();
            return TransactionResult.failure(e.getMessage());
        }
    }
}
```

---

## Phase 4: Analytics & Enhancement

### Service 4.1: Analytics Service

**Cursor Prompt Template**:
```
Based on REQUIREMENTS.md (FR-ADM-004) and USER_STORIES.md (US-035),
create Analytics Service with:
1. Real-time dashboard with 15-minute data refresh
2. TimescaleDB integration for time-series data
3. Materialized views for performance
4. Custom report builder with export
5. KPI calculations and trend analysis

Use Spring Batch for ETL pipelines.
```

**ETL Pipeline**:
```java
@Configuration
@EnableBatchProcessing
public class AnalyticsETLConfig {
    
    @Bean
    public Job analyticsJob() {
        return jobBuilderFactory.get("analyticsJob")
            .start(extractTransactionDataStep())
            .next(transformMetricsStep())
            .next(loadToTimescaleStep())
            .build();
    }
    
    @Bean
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public Step extractTransactionDataStep() {
        return stepBuilderFactory.get("extractTransactions")
            .<Transaction, TransactionMetric>chunk(1000)
            .reader(transactionReader())
            .processor(transactionProcessor())
            .writer(metricsWriter())
            .build();
    }
}
```

---

## Frontend Implementation Guide

### Component Structure

```
frontend/src/
├── components/
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   └── KycUpload.tsx
│   ├── marketplace/
│   │   ├── ListingCard.tsx
│   │   ├── SearchFilters.tsx
│   │   ├── AuctionBidder.tsx
│   │   └── PriceChart.tsx
│   ├── dashboard/
│   │   ├── CO2Widget.tsx
│   │   ├── EarningsChart.tsx
│   │   └── TripHistory.tsx
│   └── common/
│       ├── Header.tsx
│       ├── Footer.tsx
│       └── LoadingSpinner.tsx
├── pages/
│   ├── auth/
│   ├── marketplace/
│   ├── dashboard/
│   └── admin/
├── hooks/
│   ├── useAuth.ts
│   ├── useWebSocket.ts
│   └── useNotification.ts
├── services/
│   ├── api.ts
│   ├── authService.ts
│   └── marketplaceService.ts
└── store/
    ├── authSlice.ts
    ├── marketplaceSlice.ts
    └── store.ts
```

### Key Frontend Implementations

**WebSocket Hook for Auctions**:
```typescript
// hooks/useWebSocket.ts
import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export const useAuctionWebSocket = (listingId: string) => {
  const [currentBid, setCurrentBid] = useState(null);
  const [connected, setConnected] = useState(false);
  
  useEffect(() => {
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect({}, () => {
      setConnected(true);
      
      stompClient.subscribe(`/topic/auction/${listingId}`, (message) => {
        const bid = JSON.parse(message.body);
        setCurrentBid(bid);
      });
    });
    
    return () => {
      if (stompClient.connected) {
        stompClient.disconnect();
      }
    };
  }, [listingId]);
  
  return { currentBid, connected };
};
```

---

## 5: Testing Strategy

### Unit Testing Template

**Cursor Prompt for Tests**:
```
Create comprehensive unit tests for [ServiceName] with:
1. Positive test cases for all public methods
2. Negative test cases for validation
3. Edge cases (null, empty, boundary values)
4. Mocked dependencies using Mockito
5. Test coverage > 80%

Follow AAA pattern (Arrange, Act, Assert).
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {
    
    @Test
    public void testRegistrationFlow() {
        // Test complete registration flow
        // Including OTP verification
        // KYC submission
        // Login with JWT
    }
}
```

---

## 6: Deployment Instructions

### Docker Compose Setup

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: carbon_marketplace
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - ./infrastructure/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"

  elasticsearch:
    image: elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  user-service:
    build: ./backend/user-service
    depends_on:
      - postgres
      - redis
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8081:8080"

  # Add other services...
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: carbonmarketplace/user-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 7: Common Pitfalls & Solutions

### Issue 1: JWT Token Expiry
**Problem**: Tokens expire during long operations  
**Solution**: Implement refresh token rotation
```java
// Auto-refresh before expiry
if (token.expiresIn() < 60) {
    token = refreshToken();
}
```

### Issue 2: Database Connection Pool
**Problem**: Connection pool exhaustion  
**Solution**: Configure HikariCP properly
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Issue 3: Elasticsearch Memory
**Problem**: Out of memory errors  
**Solution**: Set JVM heap size (already configured in docker-compose.yml)
```bash
ES_JAVA_OPTS="-Xms2g -Xmx2g"
```

### Issue 4: Payment Webhook Failures
**Problem**: Webhooks not received during local testing  
**Solution**: Use ngrok for local testing

#### Quick Setup:
```bash
# 1. Install ngrok and authenticate
ngrok config add-authtoken YOUR_AUTH_TOKEN

# 2. Start ngrok tunnel to payment service
ngrok http 8087

# 3. Update webhook URLs with ngrok URL
export WEBHOOK_BASE_URL="https://your-ngrok-url.ngrok-free.app"
export MOMO_WEBHOOK_URL="$WEBHOOK_BASE_URL/api/v1/webhooks/momo"
export VNPAY_IPN_URL="$WEBHOOK_BASE_URL/api/v1/webhooks/vnpay"
export BANK_WEBHOOK_URL="$WEBHOOK_BASE_URL/api/v1/webhooks/bank-transfer"

# 4. Restart payment service
docker-compose restart payment-service

# 5. Monitor webhook traffic
open http://localhost:4040
```

#### Testing Webhooks:
```bash
# Test MoMo webhook
curl -X POST $MOMO_WEBHOOK_URL \
  -H "Content-Type: application/json" \
  -d '{"orderId":"TEST-001","resultCode":0}'

# Check logs
docker logs carbon-marketplace-payment-service -f
```

**Note**: See WEBHOOK_NGROK_SETUP.md for detailed instructions

---

## Monitoring & Logging

### Logging Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/carbon-marketplace.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/carbon-marketplace.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Metrics with Micrometer

```java
@RestController
public class MetricsController {
    
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/transactions")
    @Timed(value = "transaction.creation.time")
    public Transaction createTransaction() {
        // Count transactions
        meterRegistry.counter("transactions.created").increment();
        
        // Record amount
        meterRegistry.gauge("transaction.amount", transaction.getAmount());
        
        return transaction;
    }
}
```

---

## Final Checklist

### Before Going Live

- [ ] All unit tests passing (>80% coverage)
- [ ] Integration tests completed
- [ ] Security audit performed
- [ ] Performance testing done
- [ ] Database indexes optimized
- [ ] API documentation updated
- [ ] Error messages user-friendly
- [ ] Monitoring dashboards configured
- [ ] Backup strategy tested
- [ ] SSL certificates installed
- [ ] Rate limiting configured
- [ ] CORS settings verified
- [ ] Environment variables secured
- [ ] Logging properly configured
- [ ] Health checks working

---

**Document Version**: 1.0  
**Last Updated**: October 26, 2025  
**For Cursor Version**: 2.0+
