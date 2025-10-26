#!/bin/bash

# Carbon Credit Marketplace - Webhook Configuration Verification Script (Linux/macOS)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================"
echo -e " Carbon Credit Marketplace Configuration Verification"
echo -e "========================================${NC}"
echo ""

# Function to test service health
test_service_health() {
    local service_name=$1
    local health_url=$2
    
    echo -e "${YELLOW}Testing $service_name...${NC}"
    
    response=$(curl -s -X GET "$health_url" --connect-timeout 5 2>/dev/null)
    status=$(echo "$response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$status" = "UP" ]; then
        echo -e "  ${GREEN}✓ $service_name is healthy${NC}"
        return 0
    elif [ -n "$status" ]; then
        echo -e "  ${RED}✗ $service_name is unhealthy: $status${NC}"
        return 1
    else
        echo -e "  ${RED}✗ $service_name is not reachable at $health_url${NC}"
        return 1
    fi
}

# Function to check Elasticsearch memory
test_elasticsearch_memory() {
    echo -e "${YELLOW}Checking Elasticsearch Configuration...${NC}"
    
    response=$(curl -s -X GET "http://localhost:9200/_nodes/stats/jvm" 2>/dev/null)
    
    if [ -z "$response" ]; then
        echo -e "  ${RED}✗ Could not check Elasticsearch memory${NC}"
        return 1
    fi
    
    heap_max_bytes=$(echo "$response" | grep -o '"heap_max_in_bytes":[0-9]*' | head -1 | cut -d: -f2)
    
    if [ -n "$heap_max_bytes" ]; then
        heap_max_gb=$(echo "scale=2; $heap_max_bytes / 1024 / 1024 / 1024" | bc)
        
        if (( $(echo "$heap_max_gb >= 1.5" | bc -l) )); then
            echo -e "  ${GREEN}✓ Elasticsearch heap size: ${heap_max_gb}GB (Configured correctly)${NC}"
            return 0
        else
            echo -e "  ${YELLOW}⚠ Elasticsearch heap size: ${heap_max_gb}GB (Should be 2GB)${NC}"
            return 1
        fi
    else
        echo -e "  ${RED}✗ Could not parse Elasticsearch memory configuration${NC}"
        return 1
    fi
}

# Function to check webhook configuration
test_webhook_config() {
    echo -e "${YELLOW}Checking Webhook Configuration...${NC}"
    
    declare -A webhook_vars=(
        ["WEBHOOK_BASE_URL"]="$WEBHOOK_BASE_URL"
        ["MOMO_WEBHOOK_URL"]="$MOMO_WEBHOOK_URL"
        ["VNPAY_IPN_URL"]="$VNPAY_IPN_URL"
        ["BANK_WEBHOOK_URL"]="$BANK_WEBHOOK_URL"
    )
    
    has_ngrok=false
    
    for key in "${!webhook_vars[@]}"; do
        value="${webhook_vars[$key]}"
        if [ -n "$value" ]; then
            if [[ "$value" == *"ngrok"* ]]; then
                echo -e "  ${GREEN}✓ $key = $value${NC}"
                has_ngrok=true
            else
                echo -e "  ${YELLOW}⚠ $key = $value (Not using ngrok)${NC}"
            fi
        else
            echo -e "  ${YELLOW}⚠ $key not set (using default localhost)${NC}"
        fi
    done
    
    if [ "$has_ngrok" = true ]; then
        echo -e "  ${GREEN}✓ Webhook URLs configured for ngrok${NC}"
    else
        echo -e "  ${CYAN}ℹ Webhook URLs using localhost (ngrok not configured)${NC}"
    fi
    
    return 0
}

# Function to test webhook endpoints
test_webhook_endpoints() {
    echo -e "${YELLOW}Testing Webhook Endpoints...${NC}"
    
    declare -a endpoints=(
        "MoMo Webhook|http://localhost:8087/api/v1/webhooks/momo"
        "VNPay Webhook|http://localhost:8087/api/v1/webhooks/vnpay"
        "Bank Transfer Webhook|http://localhost:8087/api/v1/webhooks/bank-transfer"
    )
    
    for endpoint in "${endpoints[@]}"; do
        IFS='|' read -r name url <<< "$endpoint"
        
        body='{"orderId":"TEST-VERIFY-001","amount":100000,"resultCode":0,"message":"Configuration test"}'
        
        http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
            -H "Content-Type: application/json" \
            -d "$body" \
            --connect-timeout 5 2>/dev/null)
        
        if [ "$http_code" = "200" ] || [ "$http_code" = "400" ] || [ "$http_code" = "401" ]; then
            if [ "$http_code" = "401" ]; then
                echo -e "  ${GREEN}✓ $name endpoint is accessible (auth required)${NC}"
            else
                echo -e "  ${GREEN}✓ $name endpoint is accessible${NC}"
            fi
        elif [ "$http_code" = "000" ]; then
            echo -e "  ${RED}✗ $name is not accessible${NC}"
        else
            echo -e "  ${YELLOW}⚠ $name returned status: $http_code${NC}"
        fi
    done
}

# Function to check ngrok status
test_ngrok_status() {
    echo -e "${YELLOW}Checking ngrok Status...${NC}"
    
    response=$(curl -s -X GET "http://localhost:4040/api/tunnels" --connect-timeout 2 2>/dev/null)
    
    if [ -z "$response" ]; then
        echo -e "  ${CYAN}ℹ ngrok is not running (this is OK for local-only testing)${NC}"
        return 1
    fi
    
    tunnel_count=$(echo "$response" | grep -o '"public_url"' | wc -l)
    
    if [ "$tunnel_count" -gt 0 ]; then
        echo -e "  ${GREEN}✓ ngrok is running${NC}"
        
        # Extract and display tunnel URLs
        echo "$response" | grep -o '"public_url":"[^"]*"' | while read -r tunnel; do
            url=$(echo "$tunnel" | cut -d'"' -f4)
            echo -e "    ${CYAN}- $url${NC}"
        done
        return 0
    else
        echo -e "  ${YELLOW}⚠ ngrok is running but no tunnels active${NC}"
        return 1
    fi
}

# Function to check Redis
test_redis() {
    echo -e "${YELLOW}Testing Redis...${NC}"
    
    if command -v redis-cli &> /dev/null; then
        if redis-cli -h localhost -p 6379 ping &> /dev/null; then
            echo -e "  ${GREEN}✓ Redis is healthy${NC}"
            return 0
        else
            echo -e "  ${RED}✗ Redis is not responding${NC}"
            return 1
        fi
    else
        # Try nc as fallback
        if command -v nc &> /dev/null; then
            if nc -z localhost 6379 2>/dev/null; then
                echo -e "  ${GREEN}✓ Redis port is open (6379)${NC}"
                return 0
            else
                echo -e "  ${RED}✗ Redis port is not accessible${NC}"
                return 1
            fi
        else
            echo -e "  ${CYAN}ℹ Redis check requires redis-cli (manual verification needed)${NC}"
            return 0
        fi
    fi
}

# Main verification process
echo -e "${WHITE}1. SERVICE HEALTH CHECKS"
echo -e "------------------------${NC}"

all_healthy=true

# Test Elasticsearch
if ! test_service_health "Elasticsearch" "http://localhost:9200/_cluster/health"; then
    all_healthy=false
fi

# Test Payment Service
if ! test_service_health "Payment Service" "http://localhost:8087/actuator/health"; then
    all_healthy=false
fi

# Test Redis
if ! test_redis; then
    all_healthy=false
fi

echo ""
echo -e "${WHITE}2. ELASTICSEARCH MEMORY CHECK"
echo -e "-----------------------------${NC}"
test_elasticsearch_memory

echo ""
echo -e "${WHITE}3. WEBHOOK CONFIGURATION"
echo -e "------------------------${NC}"
test_webhook_config

echo ""
echo -e "${WHITE}4. WEBHOOK ENDPOINTS"
echo -e "-------------------${NC}"
test_webhook_endpoints

echo ""
echo -e "${WHITE}5. NGROK STATUS"
echo -e "--------------${NC}"
ngrok_running=false
if test_ngrok_status; then
    ngrok_running=true
fi

echo ""
echo -e "${CYAN}========================================"
echo -e " VERIFICATION SUMMARY"
echo -e "========================================${NC}"

if [ "$all_healthy" = true ]; then
    echo -e "${GREEN}✓ Core services are healthy${NC}"
else
    echo -e "${YELLOW}⚠ Some services need attention${NC}"
fi

if [ "$ngrok_running" = true ]; then
    echo -e "${GREEN}✓ ngrok tunnel is active${NC}"
    echo ""
    echo -e "${YELLOW}NEXT STEPS:${NC}"
    echo -e "${WHITE}1. Update payment gateway settings with your ngrok URL"
    echo -e "2. Monitor webhook traffic at http://localhost:4040${NC}"
else
    echo -e "${CYAN}ℹ ngrok is not running${NC}"
    echo ""
    echo -e "${YELLOW}TO ENABLE WEBHOOK TESTING:${NC}"
    echo -e "${WHITE}1. Start ngrok: ngrok http 8087"
    echo -e "2. Set webhook URLs with ngrok URL:"
    echo -e "${CYAN}   export WEBHOOK_BASE_URL=\"https://your-ngrok-url.ngrok-free.app\"${NC}"
    echo -e "${WHITE}3. Restart payment service: docker-compose restart payment-service${NC}"
fi

echo ""
echo -e "${CYAN}For detailed webhook setup, see: WEBHOOK_NGROK_SETUP.md${NC}"
echo ""
