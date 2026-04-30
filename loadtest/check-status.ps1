# Check status of all load testing services

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Load Test Infrastructure Status" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
Write-Host "Checking Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    Write-Host "✓ Docker is installed" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed or not running" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Check services
Write-Host "Checking services..." -ForegroundColor Yellow
$services = @("postgres", "redis", "kafka", "app", "prometheus", "grafana")
$running = @()
$stopped = @()

foreach ($service in $services) {
    $status = docker compose -f docker-compose.loadtest.yml ps $service 2>&1
    if ($status -match "running") {
        $running += $service
        Write-Host "  ✓ $service" -ForegroundColor Green
    } else {
        $stopped += $service
        Write-Host "  ✗ $service" -ForegroundColor Red
    }
}
Write-Host ""

# Service URLs
if ($running.Count -gt 0) {
    Write-Host "Service URLs:" -ForegroundColor Yellow
    
    if ($running -contains "app") {
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2
            $healthStatus = $health.status
            Write-Host "  Application:  http://localhost:8080 [$healthStatus]" -ForegroundColor Green
        } catch {
            Write-Host "  Application:  http://localhost:8080 [NOT RESPONDING]" -ForegroundColor Red
        }
    }
    
    if ($running -contains "grafana") {
        Write-Host "  Grafana:      http://localhost:3000 (admin/admin)" -ForegroundColor Cyan
    }
    
    if ($running -contains "prometheus") {
        Write-Host "  Prometheus:   http://localhost:9090" -ForegroundColor Cyan
    }
    
    Write-Host ""
}

# Resource usage
Write-Host "Resource Usage:" -ForegroundColor Yellow
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>&1 | Select-String -Pattern "sportsbetting"
Write-Host ""

# Summary
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  Running: $($running.Count)/$($services.Count) services" -ForegroundColor $(if ($running.Count -eq $services.Count) { "Green" } else { "Yellow" })
Write-Host "  Stopped: $($stopped.Count)/$($services.Count) services" -ForegroundColor $(if ($stopped.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host ""

if ($running.Count -eq $services.Count) {
    Write-Host "✓ All services are running!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ready to run tests:" -ForegroundColor Yellow
    Write-Host "  docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit" -ForegroundColor Cyan
} elseif ($running.Count -eq 0) {
    Write-Host "⚠ No services are running. Start them with:" -ForegroundColor Yellow
    Write-Host "  docker compose -f docker-compose.loadtest.yml up -d" -ForegroundColor Cyan
} else {
    Write-Host "⚠ Some services are not running. Restart with:" -ForegroundColor Yellow
    Write-Host "  docker compose -f docker-compose.loadtest.yml restart" -ForegroundColor Cyan
}
Write-Host ""
