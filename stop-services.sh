#!/bin/bash

# Carbon Credit Marketplace - Docker Compose Stop Script (Bash)

# Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================"
echo -e "Stopping All Services"
echo -e "========================================${NC}"
echo ""

read -p "Do you want to remove volumes (delete data)? (y/N): " choice

if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
    echo ""
    echo -e "${YELLOW}Stopping services and removing volumes...${NC}"
    docker-compose down -v
    echo ""
    echo -e "${GREEN}✓ All services stopped and data removed${NC}"
else
    echo ""
    echo -e "${YELLOW}Stopping services (keeping data)...${NC}"
    docker-compose down
    echo ""
    echo -e "${GREEN}✓ All services stopped (data preserved)${NC}"
fi

echo ""

