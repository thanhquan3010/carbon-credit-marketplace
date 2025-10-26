# Analytics Service

## Overview

The Analytics Service is a comprehensive data analytics and reporting microservice for the Carbon Credit Marketplace platform. It provides real-time dashboards, KPI calculations, trend analysis, custom report generation, and environmental impact metrics.

## Features

### Core Capabilities

- **Real-time Dashboards**: Executive dashboards with 15-minute data refresh
- **ETL Pipelines**: Spring Batch-based ETL for data processing
- **Time-series Analytics**: TimescaleDB integration for efficient time-series data storage
- **Materialized Views**: Performance-optimized views for fast query execution
- **Custom Reports**: Dynamic report generation with multiple export formats (PDF, Excel, CSV, JSON)
- **KPI Tracking**: Automated calculation and tracking of key performance indicators
- **Trend Analysis**: Historical trend analysis with forecasting capabilities
- **Cohort Analysis**: User retention and behavior analysis by cohorts

### Key Metrics Tracked

#### Platform Metrics
- Gross Merchandise Value (GMV)
- Total Revenue
- Platform Health Score
- Active Users (DAU/MAU)
- Transaction Volume
- Conversion Rates

#### Environmental Metrics
- Total CO2 Offset
- Credits Issued/Verified
- Distance Tracked
- Environmental Equivalents (trees planted, cars off road, etc.)

#### User Analytics
- User Engagement Score
- Lifetime Value (LTV)
- Churn Probability
- Retention Rates
- Activity Patterns

#### Marketplace Metrics
- Listing Performance
- Auction Statistics
- Pricing Trends
- Top Performers

## Architecture

### Technology Stack

- **Framework**: Spring Boot 3.1.5
- **Database**: PostgreSQL with TimescaleDB extension
- **Cache**: Redis
- **Batch Processing**: Spring Batch
- **Report Generation**: Apache POI (Excel), iText (PDF)
- **Service Discovery**: Netflix Eureka
- **API Documentation**: OpenAPI 3.0/Swagger

### Components

```
analytics-service/
├── batch/              # ETL pipelines and batch jobs
├── controller/         # REST API endpoints
├── dto/               # Data transfer objects
├── entity/            # JPA entities
├── model/             # Domain models
├── repository/        # Data access layer
├── service/           # Business logic
└── config/            # Configuration classes
```

## API Endpoints

### Dashboard Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/dashboard/executive` | Executive dashboard data |
| GET | `/api/v1/analytics/dashboard/realtime` | Real-time platform metrics |
| GET | `/api/v1/analytics/kpis` | KPI metrics by category |
| GET | `/api/v1/analytics/trends/{metricType}` | Trend analysis for specific metric |

### Report Generation

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/analytics/reports/generate` | Generate custom report |
| GET | `/api/v1/analytics/reports/{reportId}/download` | Download generated report |
| GET | `/api/v1/analytics/reports/templates` | Available report templates |

### Analytics Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/users/{userId}/analytics` | User-specific analytics |
| GET | `/api/v1/analytics/marketplace/metrics` | Marketplace performance |
| GET | `/api/v1/analytics/environmental/impact` | Environmental impact metrics |
| GET | `/api/v1/analytics/cohort/analysis` | Cohort analysis data |
| GET | `/api/v1/analytics/forecast/{metricType}` | Metric forecasting |

### Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/analytics/refresh` | Manually refresh analytics |
| GET | `/api/v1/analytics/performance/bottlenecks` | System performance analysis |

## ETL Pipeline Schedule

The service runs automated ETL jobs on the following schedule:

- **Metrics Calculation**: Every 15 minutes
- **Daily Rollup**: 1:00 AM daily
- **Weekly Rollup**: 2:00 AM every Monday
- **Monthly Rollup**: 3:00 AM on the 1st of each month

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbon_marketplace
DB_USER=postgres
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Service Discovery
EUREKA_HOST=localhost
EUREKA_PORT=8761

# Analytics Configuration
ANALYTICS_BATCH_CHUNK_SIZE=1000
ANALYTICS_CACHE_TTL=900000
```

### Application Properties

Key configuration properties in `application.yml`:

```yaml
analytics:
  batch:
    chunk-size: 1000
  etl:
    schedule:
      metrics-calculation: "0 */15 * * * *"
  materialized-views:
    refresh-interval: 900000
  reports:
    retention-days: 30
    max-concurrent-generations: 5
```

## Performance Optimization

### Caching Strategy

- **Redis Cache**: 15-minute TTL for dashboard data
- **Query Cache**: Enabled for frequently accessed metrics
- **Materialized Views**: Pre-computed aggregations refreshed every 15 minutes

### Database Optimization

- **TimescaleDB**: Optimized for time-series data
- **Partitioning**: Time-based partitioning for transaction metrics
- **Compression**: Historical data compressed after 30 days
- **Retention Policy**: Data retained for 365 days

### Batch Processing

- **Chunk Processing**: 1000 records per chunk
- **Parallel Processing**: 5 concurrent threads
- **Transaction Management**: Optimistic locking for concurrent updates

## Running the Service

### Prerequisites

1. Java 17
2. Maven 3.8+
3. PostgreSQL 14+ with TimescaleDB extension
4. Redis 7+
5. Running Eureka server

### Local Development

```bash
# Install TimescaleDB extension
psql -U postgres -d carbon_marketplace
CREATE EXTENSION IF NOT EXISTS timescaledb;

# Build the service
mvn clean package

# Run the service
mvn spring-boot:run

# Or using Java
java -jar target/analytics-service-1.0.0.jar
```

### Docker Deployment

```bash
# Build Docker image
docker build -t analytics-service:1.0.0 .

# Run with Docker Compose
docker-compose up analytics-service

# Or standalone
docker run -p 8088:8088 \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  analytics-service:1.0.0
```

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### API Testing

Access Swagger UI at: `http://localhost:8088/analytics/swagger-ui.html`

## Monitoring

### Health Check

```bash
curl http://localhost:8088/analytics/actuator/health
```

### Metrics

Prometheus metrics available at:
```bash
curl http://localhost:8088/analytics/actuator/prometheus
```

### Logging

Logs are written to:
- Console: INFO level
- File: `logs/analytics-service.log` (DEBUG level)

## Report Templates

### Available Templates

1. **Executive Summary**
   - High-level platform performance
   - Key metrics and trends
   - Health score and recommendations

2. **Financial Report**
   - Revenue analysis
   - Transaction breakdowns
   - Fee calculations
   - Projections

3. **User Analytics Report**
   - User behavior patterns
   - Engagement metrics
   - Retention analysis
   - Cohort performance

4. **Environmental Impact Report**
   - CO2 offset calculations
   - Environmental equivalents
   - Credit issuance statistics
   - Regional impact analysis

## Security

- JWT-based authentication
- Role-based access control (ADMIN, EXECUTIVE, ANALYST)
- API rate limiting
- Data encryption at rest and in transit

## Troubleshooting

### Common Issues

1. **Slow Dashboard Loading**
   - Check Redis connection
   - Verify materialized views are refreshing
   - Review query performance in logs

2. **Report Generation Timeout**
   - Increase query timeout in configuration
   - Check database connection pool settings
   - Review data volume being processed

3. **ETL Job Failures**
   - Check Spring Batch job repository
   - Review error logs for specific step failures
   - Verify data source connectivity

## Contributing

Please refer to the main project's CONTRIBUTING.md for guidelines.

## License

This service is part of the Carbon Credit Marketplace platform.

## Contact

For issues or questions, please contact the platform team.
