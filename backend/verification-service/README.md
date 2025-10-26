# Verification Service

## Overview
The Verification Service is a critical component of the Carbon Credit Marketplace that handles the verification of carbon credits by CVA (Carbon Verification Authority) auditors. This service manages the entire verification workflow from submission through approval and credit issuance.

## Features

### Core Functionality
- **Verification Request Management**: Submit, review, and track verification requests
- **Anomaly Detection**: Advanced algorithms to detect data anomalies in trip records
- **Auto-Assignment**: Intelligent workload distribution among auditors
- **Credit Issuance**: Generate unique serial numbers (VN-EV-YYYY-CVA-SEQUENCE)
- **Immutable Audit Trail**: Hash-chain based audit logging for compliance
- **SLA Management**: Track and enforce service level agreements

### Anomaly Detection Capabilities
- High/Low speed detection
- Duplicate trip identification  
- Energy efficiency outlier detection
- Time overlap validation
- GPS jump detection
- Distance/time consistency checks
- Data completeness validation

### Security Features
- OAuth2 JWT authentication
- Role-based access control (RBAC)
- Hash-chain audit trail for tamper detection
- Request signature verification

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.1.5
- **Language**: Java 17
- **Database**: PostgreSQL 14+
- **Message Queue**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Security**: Spring Security with OAuth2

### Key Components

#### Services
- **VerificationService**: Main orchestration service
- **AnomalyDetectionService**: Trip data validation and anomaly detection
- **CreditIssuanceService**: Carbon credit generation and management
- **AuditService**: Immutable audit trail with hash chain

#### Entities
- **VerificationRequest**: Core verification request entity
- **Anomaly**: Detected anomalies in trip data
- **CarbonCredit**: Issued carbon credits
- **AuditTrail**: Immutable audit log entries
- **Trip**: Vehicle trip data (read-only)

## API Endpoints

### Verification Management
```
POST   /api/v1/verifications                 - Submit verification request
GET    /api/v1/verifications/{id}            - Get verification details
PUT    /api/v1/verifications/{id}/review     - Review verification (auditors)
GET    /api/v1/verifications/my              - Get user's verifications
GET    /api/v1/verifications/queue           - Get auditor queue
GET    /api/v1/verifications/statistics      - Get statistics
POST   /api/v1/verifications/{id}/assign     - Assign to auditor
GET    /api/v1/verifications/{id}/report     - Download report
GET    /api/v1/verifications/{id}/verify-audit - Verify audit trail
```

## Configuration

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbon_marketplace
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
JWT_ISSUER_URI=http://localhost:8080/realms/carbon-marketplace
JWT_JWK_SET_URI=http://localhost:8080/realms/carbon-marketplace/protocol/openid-connect/certs

# Eureka
EUREKA_URL=http://localhost:8761/eureka/

# Server
SERVER_PORT=8085
```

### Application Properties
Key configurations in `application.yml`:
- SLA thresholds (24/48/72 hours for high/normal/low priority)
- Anomaly detection parameters
- Auto-assignment settings
- Credit issuance configuration

## Running the Service

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Apache Kafka
- Eureka Server (optional for local development)

### Local Development
```bash
# Build the project
mvn clean install

# Run the service
mvn spring-boot:run

# Or using Java
java -jar target/verification-service-1.0-SNAPSHOT.jar
```

### Docker
```bash
# Build Docker image
docker build -t verification-service:latest .

# Run container
docker run -d \
  --name verification-service \
  -p 8085:8085 \
  -e DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  verification-service:latest
```

### Docker Compose
```bash
docker-compose up -d verification-service
```

## Database Schema

### Main Tables
- `verification_requests` - Verification request records
- `verification_anomalies` - Detected anomalies
- `carbon_credits` - Issued carbon credits
- `verification_audit_trail` - Immutable audit log
- `trips` - Vehicle trip data (shared table)

## Serial Number Format
```
VN-EV-YYYY-CVA-SEQUENCE
```
- **VN**: Country code (Vietnam)
- **EV**: Electric Vehicle
- **YYYY**: Current year
- **CVA**: CVA organization code (e.g., VCS, GS, TUV)
- **SEQUENCE**: 9-digit sequential number

Example: `VN-EV-2025-VCS-000001234`

## Anomaly Detection Algorithm

### Speed Anomalies
- Max speed threshold: 150 km/h
- Min speed threshold: 5 km/h

### Energy Efficiency
- Normal range: 10-30 kWh/100km
- Statistical outlier detection using 3σ rule

### Duplicate Detection
- Groups trips by date and distance
- Identifies potential duplicates

### Time Overlap
- Detects overlapping trip times
- Tolerance: 5 minutes

## Audit Trail

### Hash Chain Implementation
- SHA-256 hashing algorithm
- Each entry contains hash of previous entry
- Genesis hash: `0000000000000000000000000000000000000000000000000000000000000000`
- Tamper detection through chain verification

### Audit Actions
- CREATE, UPDATE, DELETE
- APPROVE, REJECT, REQUEST_INFO
- ASSIGN, UNASSIGN
- ISSUE_CREDITS, ADJUST_AMOUNT
- RESOLVE_ANOMALY, ESCALATE

## Monitoring

### Health Endpoints
```
GET /actuator/health         - Service health
GET /actuator/health/liveness  - Liveness probe
GET /actuator/health/readiness - Readiness probe
GET /actuator/metrics        - Metrics
GET /actuator/prometheus     - Prometheus metrics
```

### Key Metrics
- Verification processing time
- Anomaly detection rate
- Credit issuance volume
- SLA compliance rate
- Auditor productivity

## Security

### Authentication
- OAuth2 JWT tokens required
- Token validation via JWK Set URI

### Authorization Roles
- `ROLE_EV_OWNER` - Submit and view own verifications
- `ROLE_CVA_AUDITOR` - Review and approve verifications
- `ROLE_ADMIN` - Full system access

### API Security
- CORS configuration
- Rate limiting (configured at API Gateway)
- Request signing for critical operations

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

### API Testing
Swagger UI available at: `http://localhost:8085/swagger-ui.html`

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check PostgreSQL is running
   - Verify connection parameters
   - Check network connectivity

2. **Kafka Connection Failed**
   - Ensure Kafka is running
   - Check bootstrap servers configuration
   - Verify topic creation

3. **Authentication Errors**
   - Validate JWT configuration
   - Check token expiration
   - Verify issuer URI

4. **Hash Chain Verification Failed**
   - Run integrity check: `GET /api/v1/verifications/{id}/verify-audit`
   - Check for database tampering
   - Review audit logs

## Performance Tuning

### Database
- Connection pool: 20 max, 5 min idle
- Batch size: 20 for bulk operations
- Indexes on frequently queried columns

### Application
- Thread pool: 5-10 threads
- Async processing for non-critical operations
- Caching for reference data

## Support

For issues or questions:
- Email: support@carbonmarketplace.com
- Documentation: [https://docs.carbonmarketplace.com](https://docs.carbonmarketplace.com)
- Issue Tracker: GitHub Issues

## License

Copyright © 2025 Carbon Marketplace. All rights reserved.
