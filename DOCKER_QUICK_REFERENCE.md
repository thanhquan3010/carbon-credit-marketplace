# Docker Compose Quick Reference Card
> Carbon Credit Marketplace - Essential Commands

## 🚀 Quick Start

```bash
# Windows PowerShell
.\start-services.ps1

# Linux/Mac
./start-services.sh
```

## 🛑 Stop Services

```bash
# Windows PowerShell
.\stop-services.ps1

# Linux/Mac
./stop-services.sh
```

## 📊 Service URLs

| Service | URL | Notes |
|---------|-----|-------|
| **API Gateway** | http://localhost:8080 | Main entry point |
| **Eureka Dashboard** | http://localhost:8761 | Service registry |
| **RabbitMQ UI** | http://localhost:15672 | admin/admin |
| **Elasticsearch** | http://localhost:9200 | REST API |
| **User Service** | http://localhost:8081 | Auth & users |
| **Vehicle Service** | http://localhost:8082/api | Vehicles & trips |
| **Carbon Credits** | http://localhost:8083 | CO2 & credits |
| **Marketplace** | http://localhost:8084/api | Listings & auctions |
| **Transactions** | http://localhost:8085/api/transactions | Trades |
| **Notifications** | http://localhost:8086 | Alerts |
| **Payments** | http://localhost:8087 | Payment gateway |
| **Analytics** | http://localhost:8088/analytics | Reports |
| **Verification** | http://localhost:8089 | CVA verification |

## 📖 Swagger API Docs

Add `/swagger-ui.html` to any service URL:
- http://localhost:8081/swagger-ui.html
- http://localhost:8082/api/swagger-ui.html
- http://localhost:8083/swagger-ui.html
- etc.

## 🔍 Common Commands

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service

# Last 50 lines
docker-compose logs --tail=50 marketplace-service

# Follow multiple services
docker-compose logs -f user-service vehicle-service
```

### Check Status
```bash
# All services
docker-compose ps

# Specific service
docker ps | grep user-service

# Check if healthy
docker-compose ps | grep healthy
```

### Restart Services
```bash
# Single service
docker-compose restart user-service

# All services
docker-compose restart

# Infrastructure only
docker-compose restart postgres redis elasticsearch rabbitmq
```

### Rebuild Services
```bash
# After code changes
docker-compose build user-service
docker-compose up -d user-service

# Rebuild all
docker-compose build
docker-compose up -d
```

### Scale Services
```bash
# Multiple instances
docker-compose up -d --scale user-service=3

# Reset to single instance
docker-compose up -d --scale user-service=1
```

## 🗄️ Database Access

### PostgreSQL
```bash
# Connect to database
docker exec -it carbon-marketplace-postgres psql -U postgres -d carbon_marketplace

# Run SQL file
docker exec -i carbon-marketplace-postgres psql -U postgres -d carbon_marketplace < backup.sql

# Backup database
docker exec carbon-marketplace-postgres pg_dump -U postgres carbon_marketplace > backup.sql

# List databases
docker exec -it carbon-marketplace-postgres psql -U postgres -c "\l"
```

### Redis
```bash
# Connect to Redis
docker exec -it carbon-marketplace-redis redis-cli

# Common Redis commands
PING
KEYS *
GET key_name
FLUSHALL  # Clear all data (careful!)
```

## 🔧 Troubleshooting

### Service Won't Start
```bash
# Check logs
docker-compose logs service-name

# Check dependency health
docker-compose ps postgres redis rabbitmq

# Restart dependencies
docker-compose restart postgres redis
```

### Port Already in Use
```bash
# Find process using port (Windows)
netstat -ano | findstr :8080

# Find process using port (Linux/Mac)
lsof -i :8080

# Kill process
# Windows: taskkill /F /PID <pid>
# Linux/Mac: kill -9 <pid>
```

### Clear Everything
```bash
# Stop and remove all (keeps volumes)
docker-compose down

# Remove volumes too (deletes data!)
docker-compose down -v

# Clean Docker system
docker system prune -a --volumes
```

### Service Not Registering with Eureka
```bash
# Wait 30-60 seconds
# Check Eureka dashboard: http://localhost:8761

# Check service logs
docker-compose logs -f user-service | grep -i eureka

# Restart service
docker-compose restart user-service
```

### Out of Memory
```bash
# Check Docker memory usage
docker stats

# Reduce Elasticsearch memory (in docker-compose.yml)
# ES_JAVA_OPTS=-Xms256m -Xmx256m

# Increase Docker Desktop memory limit
# Docker Desktop → Settings → Resources → Memory
```

## 🏥 Health Checks

```bash
# Quick health check script
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/api/actuator/health  # Vehicle Service
curl http://localhost:8083/actuator/health  # Carbon Credit
curl http://localhost:8084/api/actuator/health  # Marketplace

# Or check all at once
docker-compose ps | grep healthy
```

## 🔐 Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| PostgreSQL | postgres | postgres |
| RabbitMQ | admin | admin |
| Redis | (none) | (none) |

**⚠️ Change these for production!**

## 📦 Data Volumes

```bash
# List volumes
docker volume ls | grep carbon

# Inspect volume
docker volume inspect carbon-credit-marketplace_postgres_data

# Backup volume
docker run --rm -v carbon-credit-marketplace_postgres_data:/data \
  -v $(pwd):/backup ubuntu tar czf /backup/postgres_backup.tar.gz /data

# Restore volume
docker run --rm -v carbon-credit-marketplace_postgres_data:/data \
  -v $(pwd):/backup ubuntu tar xzf /backup/postgres_backup.tar.gz -C /
```

## 🌐 Network Commands

```bash
# List networks
docker network ls | grep carbon

# Inspect network
docker network inspect carbon-credit-marketplace_carbon-network

# See connected containers
docker network inspect carbon-credit-marketplace_carbon-network \
  --format '{{range .Containers}}{{.Name}} {{end}}'
```

## 💡 Pro Tips

1. **Always check Eureka** (http://localhost:8761) to verify services are registered
2. **Wait for health checks** - Services need 30-60s to fully start
3. **Use logs** - Most issues are visible in logs
4. **Start infrastructure first** - postgres, redis, elasticsearch, rabbitmq
5. **Check dependencies** - Services depend on each other

## 📚 More Information

- **Full Deployment Guide**: [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
- **Implementation Summary**: [IMPLEMENTATION_SUMMARY_DOCKER.md](IMPLEMENTATION_SUMMARY_DOCKER.md)
- **Project Documentation**: [project-docs/](project-docs/)

## 🆘 Getting Help

1. Check logs: `docker-compose logs -f <service>`
2. Check health: `docker-compose ps`
3. Review: [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) → Troubleshooting section
4. Restart service: `docker-compose restart <service>`
5. Full reset: `docker-compose down && docker-compose up -d`

---

**Last Updated**: October 26, 2025  
**Keep this file handy for quick reference!**

