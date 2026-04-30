# PowerShell script to run all load tests sequentially

$ErrorActionPreference = "Stop"

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Sports Betting Load Test Suite" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if services are running
Write-Host "Checking if services are running..." -ForegroundColor Yellow
$services = docker compose -f docker-compose.loadtest.yml ps
if ($services -notmatch "app.*running") {
    Write-Host "Error: Application is not running. Please start services first:" -ForegroundColor Red
    Write-Host "  docker compose -f docker-compose.loadtest.yml up -d" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Services are running" -ForegroundColor Green
Write-Host ""

# Wait for app to be healthy
Write-Host "Waiting for application to be healthy..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$healthy = $false

while ($attempt -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ Application is healthy" -ForegroundColor Green
            $healthy = $true
            break
        }
    }
    catch {
        # Ignore errors and retry
    }
    $attempt++
    Write-Host "  Attempt $attempt/$maxAttempts..."
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    Write-Host "Error: Application did not become healthy in time" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Starting Load Tests" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Open Grafana to watch live metrics:" -ForegroundColor Yellow
Write-Host "  http://localhost:3000 (admin/LiveLife23#)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Dashboard: Load Testing → Sports Betting Load Test Dashboard" -ForegroundColor Yellow
Write-Host ""
Start-Sleep -Seconds 5

# Test 1: Rate Limiting
Write-Host "[1/4] Running Rate Limit Test (3.5 min)..." -ForegroundColor Yellow
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit
Write-Host "✓ Rate Limit Test Complete" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 10

# Test 2: Bet Placement
Write-Host "[2/4] Running Bet Placement Test (5 min)..." -ForegroundColor Yellow
docker compose -f docker-compose.loadtest.yml run --rm k6-bets
Write-Host "✓ Bet Placement Test Complete" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 10

# Test 3: Settlement Contention
Write-Host "[3/4] Running Settlement Test (2 min)..." -ForegroundColor Yellow
docker compose -f docker-compose.loadtest.yml run --rm k6-settlement
Write-Host "✓ Settlement Test Complete" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 10

# Test 4: Odds Burst
Write-Host "[4/4] Running Odds Burst Test (3.5 min)..." -ForegroundColor Yellow
docker compose -f docker-compose.loadtest.yml run --rm k6-odds
Write-Host "✓ Odds Burst Test Complete" -ForegroundColor Green
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "All Tests Complete!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Results saved in: loadtest\k6\results\" -ForegroundColor Yellow
Write-Host ""
Write-Host "View detailed metrics in Grafana:" -ForegroundColor Yellow
Write-Host "  http://localhost:3000" -ForegroundColor Cyan
Write-Host ""
Write-Host "View Prometheus metrics:" -ForegroundColor Yellow
Write-Host "  http://localhost:9090" -ForegroundColor Cyan
Write-Host ""
