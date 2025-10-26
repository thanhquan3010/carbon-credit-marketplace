# Carbon Credit Marketplace - Docker Compose Startup Script (PowerShell)
# This script starts all services in the correct order

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Carbon Credit Marketplace - Docker Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
try {
    docker info | Out-Null
    Write-Host "✓ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if docker-compose is available
Write-Host "Checking Docker Compose..." -ForegroundColor Yellow
try {
    docker-compose version | Out-Null
    Write-Host "✓ Docker Compose is available" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose is not available." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 1: Starting Infrastructure Services..." -ForegroundColor Cyan
Write-Host "  - PostgreSQL" -ForegroundColor White
Write-Host "  - Redis" -ForegroundColor White
Write-Host "  - Elasticsearch" -ForegroundColor White
Write-Host "  - RabbitMQ" -ForegroundColor White
Write-Host ""

docker-compose up -d postgres redis elasticsearch rabbitmq

Write-Host ""
Write-Host "Waiting for infrastructure services to be healthy (60 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

Write-Host ""
Write-Host "Step 2: Starting Core Infrastructure..." -ForegroundColor Cyan
Write-Host "  - Eureka Server (Service Discovery)" -ForegroundColor White
Write-Host "  - Config Server" -ForegroundColor White
Write-Host "  - API Gateway" -ForegroundColor White
Write-Host ""

docker-compose up -d eureka-server config-server api-gateway

Write-Host ""
Write-Host "Waiting for Eureka to start (45 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 45

Write-Host ""
Write-Host "Step 3: Starting Backend Microservices..." -ForegroundColor Cyan
Write-Host "  - User Service (Port 8081)" -ForegroundColor White
Write-Host "  - Vehicle Service (Port 8082)" -ForegroundColor White
Write-Host "  - Carbon Credit Service (Port 8083)" -ForegroundColor White
Write-Host "  - Marketplace Service (Port 8084)" -ForegroundColor White
Write-Host "  - Transaction Service (Port 8085)" -ForegroundColor White
Write-Host "  - Notification Service (Port 8086)" -ForegroundColor White
Write-Host "  - Payment Service (Port 8087)" -ForegroundColor White
Write-Host "  - Analytics Service (Port 8088)" -ForegroundColor White
Write-Host "  - Verification Service (Port 8089)" -ForegroundColor White
Write-Host ""

docker-compose up -d user-service vehicle-service carbon-credit-service `
  marketplace-service transaction-service notification-service `
  payment-service verification-service analytics-service

Write-Host ""
Write-Host "Waiting for services to start (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Deployment Status" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

docker-compose ps

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Service Access URLs" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Infrastructure:" -ForegroundColor Yellow
Write-Host "  - API Gateway:         http://localhost:8080" -ForegroundColor White
Write-Host "  - Eureka Dashboard:    http://localhost:8761" -ForegroundColor White
Write-Host "  - RabbitMQ Management: http://localhost:15672 (admin/admin)" -ForegroundColor White
Write-Host "  - Elasticsearch:       http://localhost:9200" -ForegroundColor White
Write-Host ""
Write-Host "Microservices:" -ForegroundColor Yellow
Write-Host "  - User Service:        http://localhost:8081" -ForegroundColor White
Write-Host "  - Vehicle Service:     http://localhost:8082/api" -ForegroundColor White
Write-Host "  - Carbon Credit:       http://localhost:8083" -ForegroundColor White
Write-Host "  - Marketplace:         http://localhost:8084/api" -ForegroundColor White
Write-Host "  - Transaction:         http://localhost:8085/api/transactions" -ForegroundColor White
Write-Host "  - Notification:        http://localhost:8086" -ForegroundColor White
Write-Host "  - Payment:             http://localhost:8087" -ForegroundColor White
Write-Host "  - Analytics:           http://localhost:8088/analytics" -ForegroundColor White
Write-Host "  - Verification:        http://localhost:8089" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Tip: View logs with: docker-compose logs -f <service-name>" -ForegroundColor Yellow
Write-Host "Tip: Stop all services with: docker-compose down" -ForegroundColor Yellow
Write-Host ""

