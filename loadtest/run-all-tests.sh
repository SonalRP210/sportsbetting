#!/bin/bash
# Run all load tests sequentially

set -e

echo "=================================="
echo "Sports Betting Load Test Suite"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if services are running
echo -e "${YELLOW}Checking if services are running...${NC}"
if ! docker compose -f docker-compose.loadtest.yml ps | grep -q "app.*running"; then
    echo "Error: Application is not running. Please start services first:"
    echo "  docker compose -f docker-compose.loadtest.yml up -d"
    exit 1
fi

echo -e "${GREEN}✓ Services are running${NC}"
echo ""

# Wait for app to be healthy
echo -e "${YELLOW}Waiting for application to be healthy...${NC}"
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy${NC}"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "Error: Application did not become healthy in time"
    exit 1
fi

echo ""
echo "=================================="
echo "Starting Load Tests"
echo "=================================="
echo ""
echo "Open Grafana to watch live metrics:"
echo "  http://localhost:3000 (admin/LiveLife23#)"
echo ""
echo "Dashboard: Load Testing → Sports Betting Load Test Dashboard"
echo ""
sleep 5

# Test 1: Rate Limiting
echo -e "${YELLOW}[1/4] Running Rate Limit Test (3.5 min)...${NC}"
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit
echo -e "${GREEN}✓ Rate Limit Test Complete${NC}"
echo ""
sleep 10

# Test 2: Bet Placement
echo -e "${YELLOW}[2/4] Running Bet Placement Test (5 min)...${NC}"
docker compose -f docker-compose.loadtest.yml run --rm k6-bets
echo -e "${GREEN}✓ Bet Placement Test Complete${NC}"
echo ""
sleep 10

# Test 3: Settlement Contention
echo -e "${YELLOW}[3/4] Running Settlement Test (2 min)...${NC}"
docker compose -f docker-compose.loadtest.yml run --rm k6-settlement
echo -e "${GREEN}✓ Settlement Test Complete${NC}"
echo ""
sleep 10

# Test 4: Odds Burst
echo -e "${YELLOW}[4/4] Running Odds Burst Test (3.5 min)...${NC}"
docker compose -f docker-compose.loadtest.yml run --rm k6-odds
echo -e "${GREEN}✓ Odds Burst Test Complete${NC}"
echo ""

echo "=================================="
echo -e "${GREEN}All Tests Complete!${NC}"
echo "=================================="
echo ""
echo "Results saved in: loadtest/k6/results/"
echo ""
echo "View detailed metrics in Grafana:"
echo "  http://localhost:3000"
echo ""
echo "View Prometheus metrics:"
echo "  http://localhost:9090"
echo ""
