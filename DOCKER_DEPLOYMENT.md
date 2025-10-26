# Docker Deployment Guide - Carbon Credit Marketplace

This guide provides instructions for deploying the Carbon Credit Marketplace using Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available
- At least 20GB free disk space

## Architecture Overview

The platform consists of:

### Infrastructure Services (4)
- **PostgreSQL** - Main database (Port 5432)
- **Redis** - Caching and session management (Port 6379)
- **Elasticsearch** - Search and analytics (Port 9200, 9300)
- **RabbitMQ** - Message broker (Port 5672, 15672)

### Core Microservices (9)
- **User Service** - Port 8081
- **Vehicle Service** - Port 8082
- **Carbon Credit Service** - Port 8083
- **Marketplace Service** - Port 8084
- **Transaction Service** - Port 8085
- **Notification Service** - Port 8086 (internal: 8084)
- **Payment Service** - Port 8087 (internal: 8085)
- **Analytics Service** - Port 8088
- **Verification Service** - Port 8089 (internal: 8085)

### Infrastructure Microservices
- **Eureka Server** - Service discovery (Port 8761)
- **Config Server** - Centralized configuration (Port 8888)
- **API Gateway** - API routing (Port 8080)

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd carbon-credit-marketplace
```

### 2. Configure Environment Variables

Create a `.env` file in the project root with your configuration:

```bash
# Email Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Payment Gateway Configuration
MOMO_PARTNER_CODE=your-momo-partner-code
MOMO_ACCESS_KEY=your-momo-access-key
MOMO_SECRET_KEY=your-momo-secret-key
VNPAY_TMN_CODE=your-vnpay-tmn-code
VNPAY_HASH_SECRET=your-vnpay-hash-secret

# Optional: External Services
SENDGRID_API_KEY=your-sendgrid-api-key
TWILIO_ACCOUNT_SID=your-twilio-sid
TWILIO_AUTH_TOKEN=your-twilio-token
```

### 3. Start Infrastructure Services

Start the infrastructure layer first (databases, caches, message brokers):

```bash
docker-compose up -d postgres redis elasticsearch rabbitmq
```

Wait for all services to be healthy (check with `docker-compose ps`).

### 4. Start Core Infrastructure Services

Start Eureka, Config Server, and API Gateway:

```bash
docker-compose up -d eureka-server config-server api-gateway
```

Wait for Eureka to be fully up (about 30-60 seconds).

### 5. Start All Microservices

Start all backend microservices:

```bash
docker-compose up -d user-service vehicle-service carbon-credit-service \
  marketplace-service transaction-service notification-service \
  payment-service verification-service analytics-service
```

### 6. Verify Deployment

Check all services are running:

```bash
docker-compose ps
```

All services should show status as "healthy" or "Up".

## Service Access URLs

Once deployed, you can access:

- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (admin/admin)
- **Elasticsearch**: http://localhost:9200
- **User Service**: http://localhost:8081
- **Vehicle Service**: http://localhost:8082/api
- **Carbon Credit Service**: http://localhost:8083
- **Marketplace Service**: http://localhost:8084/api
- **Transaction Service**: http://localhost:8085/api/transactions
- **Notification Service**: http://localhost:8086
- **Payment Service**: http://localhost:8087
- **Analytics Service**: http://localhost:8088/analytics
- **Verification Service**: http://localhost:8089

## Service Health Checks

Each service exposes health endpoints:

```bash
# Check User Service
curl http://localhost:8081/actuator/health

# Check Vehicle Service
curl http://localhost:8082/api/actuator/health

# Check Marketplace Service
curl http://localhost:8084/api/actuator/health
```

## Swagger API Documentation

Each service provides Swagger UI for API documentation:

- User Service: http://localhost:8081/swagger-ui.html
- Vehicle Service: http://localhost:8082/api/swagger-ui.html
- Carbon Credit Service: http://localhost:8083/swagger-ui.html
- Marketplace Service: http://localhost:8084/api/swagger-ui.html
- Transaction Service: http://localhost:8085/api/transactions/swagger-ui.html
- Payment Service: http://localhost:8087/swagger-ui.html
- Analytics Service: http://localhost:8088/analytics/swagger-ui.html
- Verification Service: http://localhost:8089/swagger-ui.html

## Database Initialization

The PostgreSQL database is automatically initialized with the schema from `infrastructure/postgres/init.sql` on first startup.

### Manual Database Access

```bash
docker exec -it carbon-marketplace-postgres psql -U postgres -d carbon_marketplace
```

## Common Operations

### View Logs

View logs for a specific service:

```bash
docker-compose logs -f user-service
```

View all logs:

```bash
docker-compose logs -f
```

### Restart a Service

```bash
docker-compose restart user-service
```

### Stop All Services

```bash
docker-compose down
```

### Stop and Remove Volumes

```bash
docker-compose down -v
```

**Warning**: This will delete all data!

### Rebuild Services

After code changes, rebuild and restart:

```bash
docker-compose build user-service
docker-compose up -d user-service
```

Rebuild all services:

```bash
docker-compose build
docker-compose up -d
```

## Scaling Services

Scale a service to multiple instances:

```bash
docker-compose up -d --scale user-service=3
```

## Monitoring

### Container Stats

```bash
docker stats
```

### Health Status

```bash
docker-compose ps
```

### Check Service Discovery

Visit Eureka dashboard at http://localhost:8761 to see all registered services.

## Troubleshooting

### Service Won't Start

1. Check logs:
   ```bash
   docker-compose logs service-name
   ```

2. Check if dependencies are healthy:
   ```bash
   docker-compose ps postgres redis rabbitmq elasticsearch
   ```

3. Restart dependencies:
   ```bash
   docker-compose restart postgres redis
   ```

### Database Connection Issues

1. Verify PostgreSQL is running:
   ```bash
   docker-compose ps postgres
   ```

2. Check PostgreSQL logs:
   ```bash
   docker-compose logs postgres
   ```

3. Test connection:
   ```bash
   docker exec -it carbon-marketplace-postgres pg_isready -U postgres
   ```

### Memory Issues

If services are slow or crashing:

1. Check available memory:
   ```bash
   docker system df
   ```

2. Increase Docker memory limit in Docker Desktop settings

3. Reduce Elasticsearch memory:
   ```yaml
   # In docker-compose.yml
   elasticsearch:
     environment:
       - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
   ```

### Port Conflicts

If you get port binding errors:

1. Check which process is using the port:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```

2. Change the port mapping in `docker-compose.yml`:
   ```yaml
   ports:
     - "9080:8080"  # Use port 9080 instead
   ```

### Service Registration Delays

Services may take 30-60 seconds to register with Eureka. Wait before making requests.

## Performance Tuning

### Optimize for Development

Use fewer resources:

```yaml
postgres:
  environment:
    - POSTGRES_SHARED_BUFFERS=128MB

elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
```

### Optimize for Production

Increase resources:

```yaml
postgres:
  environment:
    - POSTGRES_SHARED_BUFFERS=2GB
    - POSTGRES_EFFECTIVE_CACHE_SIZE=6GB

elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
```

## Security Considerations

### For Production Deployment

1. **Change Default Passwords**: Update all passwords in environment variables
2. **Use Secrets Management**: Consider Docker Secrets or external secret managers
3. **Enable SSL/TLS**: Configure SSL certificates for all services
4. **Network Segmentation**: Use multiple Docker networks for security layers
5. **Update JWT Secrets**: Change all JWT_SECRET environment variables
6. **Limit Exposed Ports**: Only expose necessary ports to the host

### Environment Variables to Change

```bash
# Database passwords
POSTGRES_PASSWORD=<strong-password>

# RabbitMQ credentials
RABBITMQ_DEFAULT_USER=<username>
RABBITMQ_DEFAULT_PASS=<strong-password>

# JWT secrets (minimum 256 bits)
JWT_SECRET=<your-secure-random-string>

# API keys and credentials
MOMO_SECRET_KEY=<your-production-key>
VNPAY_HASH_SECRET=<your-production-key>
```

## Backup and Recovery

### Backup Database

```bash
docker exec carbon-marketplace-postgres pg_dump -U postgres carbon_marketplace > backup.sql
```

### Restore Database

```bash
cat backup.sql | docker exec -i carbon-marketplace-postgres psql -U postgres carbon_marketplace
```

### Backup Volumes

```bash
docker run --rm -v carbon-credit-marketplace_postgres_data:/data -v $(pwd):/backup \
  ubuntu tar czf /backup/postgres_backup.tar.gz /data
```

## Production Deployment Notes

For production deployment, consider:

1. **Use External Databases**: Use managed database services (AWS RDS, Azure Database)
2. **Container Orchestration**: Migrate to Kubernetes for better scalability
3. **Load Balancing**: Use external load balancers (Nginx, AWS ALB)
4. **Monitoring**: Implement Prometheus + Grafana for monitoring
5. **Logging**: Use ELK stack or cloud logging services
6. **CI/CD**: Set up automated builds and deployments
7. **High Availability**: Deploy multiple instances of each service
8. **Disaster Recovery**: Implement automated backups and recovery procedures

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Elasticsearch Docker Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html)

## Support

For issues or questions:
- Check logs: `docker-compose logs -f`
- Review service health: `docker-compose ps`
- Consult project documentation in `/project-docs`

---

**Last Updated**: October 26, 2025  
**Version**: 1.0

