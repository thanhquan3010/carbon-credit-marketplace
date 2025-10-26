#!/bin/bash

# Carbon Credit Marketplace - Docker Compose Startup Script (Bash)
# This script starts all services in the correct order

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================"
echo -e "Carbon Credit Marketplace - Docker Setup"
echo -e "========================================${NC}"
echo ""

# Check if Docker is running
echo -e "${YELLOW}Checking Docker status...${NC}"
if docker info > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker is running${NC}"
else
    echo -e "${RED}✗ Docker is not running. Please start Docker.${NC}"
    exit 1
fi

# Check if docker-compose is available
echo -e "${YELLOW}Checking Docker Compose...${NC}"
if command -v docker-compose > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker Compose is available${NC}"
else
    echo -e "${RED}✗ Docker Compose is not available.${NC}"
    exit 1
fi

echo ""
echo -e "${CYAN}Step 1: Starting Infrastructure Services...${NC}"
echo -e "${WHITE}  - PostgreSQL"
echo -e "  - Redis"
echo -e "  - Elasticsearch"
echo -e "  - RabbitMQ${NC}"
echo ""

docker-compose up -d postgres redis elasticsearch rabbitmq

echo ""
echo -e "${YELLOW}Waiting for infrastructure services to be healthy (60 seconds)...${NC}"
sleep 60

echo ""
echo -e "${CYAN}Step 2: Starting Core Infrastructure...${NC}"
echo -e "${WHITE}  - Eureka Server (Service Discovery)"
echo -e "  - Config Server"
echo -e "  - API Gateway${NC}"
echo ""

docker-compose up -d eureka-server config-server api-gateway

echo ""
echo -e "${YELLOW}Waiting for Eureka to start (45 seconds)...${NC}"
sleep 45

echo ""
echo -e "${CYAN}Step 3: Starting Backend Microservices...${NC}"
echo -e "${WHITE}  - User Service (Port 8081)"
echo -e "  - Vehicle Service (Port 8082)"
echo -e "  - Carbon Credit Service (Port 8083)"
echo -e "  - Marketplace Service (Port 8084)"
echo -e "  - Transaction Service (Port 8085)"
echo -e "  - Notification Service (Port 8086)"
echo -e "  - Payment Service (Port 8087)"
echo -e "  - Analytics Service (Port 8088)"
echo -e "  - Verification Service (Port 8089)${NC}"
echo ""

docker-compose up -d user-service vehicle-service carbon-credit-service \
  marketplace-service transaction-service notification-service \
  payment-service verification-service analytics-service

echo ""
echo -e "${YELLOW}Waiting for services to start (30 seconds)...${NC}"
sleep 30

echo ""
echo -e "${GREEN}========================================"
echo -e "Deployment Status"
echo -e "========================================${NC}"
echo ""

docker-compose ps

echo ""
echo -e "${CYAN}========================================"
echo -e "Service Access URLs"
echo -e "========================================${NC}"
echo ""
echo -e "${YELLOW}Infrastructure:${NC}"
echo -e "${WHITE}  - API Gateway:         http://localhost:8080"
echo -e "  - Eureka Dashboard:    http://localhost:8761"
echo -e "  - RabbitMQ Management: http://localhost:15672 (admin/admin)"
echo -e "  - Elasticsearch:       http://localhost:9200${NC}"
echo ""
echo -e "${YELLOW}Microservices:${NC}"
echo -e "${WHITE}  - User Service:        http://localhost:8081"
echo -e "  - Vehicle Service:     http://localhost:8082/api"
echo -e "  - Carbon Credit:       http://localhost:8083"
echo -e "  - Marketplace:         http://localhost:8084/api"
echo -e "  - Transaction:         http://localhost:8085/api/transactions"
echo -e "  - Notification:        http://localhost:8086"
echo -e "  - Payment:             http://localhost:8087"
echo -e "  - Analytics:           http://localhost:8088/analytics"
echo -e "  - Verification:        http://localhost:8089${NC}"
echo ""
echo -e "${GREEN}========================================"
echo -e "Deployment Complete!"
echo -e "========================================${NC}"
echo ""
echo -e "${YELLOW}Tip: View logs with: docker-compose logs -f <service-name>${NC}"
echo -e "${YELLOW}Tip: Stop all services with: docker-compose down${NC}"
echo ""

