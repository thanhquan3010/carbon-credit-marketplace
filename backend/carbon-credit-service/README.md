# Carbon Credit Service

## Overview
The Carbon Credit Service is a microservice responsible for managing carbon credits, CO2 calculations, verification requests, and carbon wallet management in the Carbon Credit Marketplace platform.

## Features Implemented

### 1. CO2 Calculation Engine
- **Formula**: CO2 Saved (kg) = Distance (km) × (0.15 - 0.05)
- **Accuracy**: Within ±2% tolerance
- **Methodology**: CDM ACM0018 standard
- Supports both trip-based and quick distance-based calculations
- Data quality score adjustment

### 2. Carbon Wallet Management
- User carbon credit wallet with balance tracking
- Available, pending, and locked balance management
- Credit transfers between users
- Credit retirement functionality
- Transaction history tracking
- Wallet status management (active, frozen, suspended, closed)

### 3. Verification Request System
- Submit verification requests for trip data
- Auditor assignment and review workflow
- Approval/rejection with notes
- Carbon credit issuance upon approval
- SLA tracking based on priority
- Overlapping request validation

### 4. Credit Transaction Management
- Complete transaction history
- Multiple transaction types (issuance, transfer, sale, retirement)
- Platform fee calculation
- Transaction status tracking

## Project Structure

```
carbon-credit-service/
├── src/main/java/com/carbonmarketplace/carboncreditservice/
│   ├── CarbonCreditServiceApplication.java
│   ├── entity/                    # JPA entities
│   │   ├── Trip.java
│   │   ├── VerificationRequest.java
│   │   ├── CarbonCredit.java
│   │   ├── CreditTransaction.java
│   │   └── CarbonWallet.java
│   ├── repository/                # Data repositories
│   │   ├── TripRepository.java
│   │   ├── VerificationRequestRepository.java
│   │   ├── CarbonCreditRepository.java
│   │   ├── CreditTransactionRepository.java
│   │   └── CarbonWalletRepository.java
│   ├── service/                   # Business logic
│   │   ├── CarbonCalculationService.java
│   │   ├── CarbonWalletService.java
│   │   └── VerificationService.java
│   ├── controller/                # REST controllers
│   │   ├── CarbonCalculationController.java
│   │   ├── CarbonWalletController.java
│   │   └── VerificationController.java
│   ├── dto/                       # Data transfer objects
│   │   ├── CarbonCalculationResult.java
│   │   ├── request/
│   │   └── response/
│   ├── exception/                 # Custom exceptions
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InsufficientBalanceException.java
│   │   ├── WalletNotFoundException.java
│   │   └── VerificationException.java
│   ├── config/                    # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── OpenApiConfig.java
│   └── client/                    # Feign clients
│       ├── UserServiceClient.java
│       └── VehicleServiceClient.java
├── src/main/resources/
│   └── application.yml           # Application configuration
├── Dockerfile                     # Docker configuration
└── pom.xml                       # Maven dependencies
```

## API Endpoints

### Carbon Calculation
- `POST /api/v1/carbon/calculate` - Calculate carbon credits from trips
- `GET /api/v1/carbon/calculate/estimate` - Quick estimation based on distance
- `POST /api/v1/carbon/calculate/validate` - Validate calculation accuracy

### Carbon Wallet
- `GET /api/v1/carbon/wallet/my-wallet` - Get user's wallet details
- `GET /api/v1/carbon/wallet/user/{userId}` - Get specific user's wallet (admin)
- `POST /api/v1/carbon/wallet/transfer` - Transfer credits to another user
- `POST /api/v1/carbon/wallet/retire` - Retire credits permanently
- `GET /api/v1/carbon/wallet/transactions` - Get transaction history

### Verification
- `POST /api/v1/carbon/verification` - Submit verification request
- `GET /api/v1/carbon/verification/{verificationId}` - Get verification details
- `GET /api/v1/carbon/verification/my-requests` - Get user's verification requests
- `POST /api/v1/carbon/verification/{verificationId}/approve` - Approve verification
- `POST /api/v1/carbon/verification/{verificationId}/reject` - Reject verification
- `POST /api/v1/carbon/verification/{verificationId}/assign` - Assign to auditor
- `GET /api/v1/carbon/verification/{verificationId}/credits` - Get issued credits

## Technologies Used
- Java 17
- Spring Boot 3.1.5
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- OpenAPI 3.0 (Swagger)
- Lombok
- Maven

## Configuration
Key configuration parameters in `application.yml`:
- Database connection settings
- Carbon calculation parameters (emission factors)
- Verification settings (min trips, min distance)
- JWT security settings
- Service URLs for integration

## Running the Service

### Local Development
```bash
mvn clean install
mvn spring-boot:run
```

### Docker
```bash
docker build -t carbon-credit-service .
docker run -p 8083:8083 carbon-credit-service
```

### Environment Variables
- `DB_HOST`: PostgreSQL host
- `DB_PORT`: PostgreSQL port
- `DB_NAME`: Database name
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret

## API Documentation
Once running, access the Swagger UI at:
- http://localhost:8083/swagger-ui.html

## Health Check
- http://localhost:8083/actuator/health

## Dependencies
- User Service (port 8081) - For user information
- Vehicle Service (port 8082) - For vehicle verification

## Security
- JWT-based authentication
- Role-based authorization (EVOWNER, BUYER, VERIFIER, ADMIN)
- CORS configuration for frontend integration

## Database Schema
The service manages the following main tables:
- `trips` - EV trip data
- `verification_requests` - Carbon credit verification requests
- `carbon_credits` - Issued carbon credits
- `credit_transactions` - Transaction history
- `carbon_wallets` - User wallet balances

## Testing
Run tests with:
```bash
mvn test
```

## Monitoring
- Prometheus metrics available at `/actuator/prometheus`
- Health endpoint at `/actuator/health`
- Info endpoint at `/actuator/info`
