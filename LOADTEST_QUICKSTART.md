# Load Testing Quick Start Guide

Get your sports betting app load tested in 5 minutes!

## ⚡ Super Quick Start (Copy-Paste)

```bash
# 1. Build and start everything
docker compose -f docker-compose.loadtest.yml build
docker compose -f docker-compose.loadtest.yml up -d

# 2. Wait for healthy (30 seconds)
echo "Waiting for services..." && sleep 30

# 3. Open Grafana dashboard
start http://localhost:3000
# Login: admin/LiveLife23#
# Navigate to: Dashboards → Load Testing → Sports Betting Load Test Dashboard

# 4. Run a quick test
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit
```

## 📊 What You Get

**Monitoring Stack**:

- **Grafana Dashboard**: [http://localhost:3000](http://localhost:3000) (admin/LiveLife23#)
  - Real-time metrics visualization
  - 9 pre-configured panels
  - Auto-refreshes every 5 seconds
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
  - Metrics aggregation
  - PromQL query interface
- **Application Metrics**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
  - Raw Spring Boot metrics
  - Custom business metrics

**4 Load Tests**:

1. **Rate Limiting** - 3.5 min - Tests 429 responses
2. **Bet Placement** - 5 min - Tests DB contention & idempotency
3. **Settlement** - 2 min - Tests pessimistic locking
4. **Odds Burst** - 3.5 min - Tests high-frequency updates

## 🎯 Run Individual Tests

```bash
# Test 1: Rate Limiting (fast, good for first test)
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit

# Test 2: Bet Placement (most realistic)
docker compose -f docker-compose.loadtest.yml run --rm k6-bets

# Test 3: Settlement Contention (tests worst case)
docker compose -f docker-compose.loadtest.yml run --rm k6-settlement

# Test 4: Odds Burst (tests throughput)
docker compose -f docker-compose.loadtest.yml run --rm k6-odds
```

## 🚀 Run All Tests (Windows PowerShell)

```powershell
# Make sure you're in the project root
cd "d:\github personal\sportsbetting"

# Run all tests sequentially (~15 minutes total)
.\loadtest\run-all-tests.ps1
```

## 🐧 Run All Tests (Linux/Mac)

```bash
# Make script executable
chmod +x loadtest/run-all-tests.sh

# Run all tests
./loadtest/run-all-tests.sh
```

## 📈 What to Watch in Grafana

While tests run, watch these panels:

1. **Request Throughput** - Should ramp up according to test profile
2. **Settlement Latency** - P95 should stay < 5s
3. **Lock Contention** - Shows wait times for settlement locks
4. **Idempotency Events** - Shows duplicate request handling
5. **JVM Heap** - Should not continuously grow
6. **CPU Usage** - Should be < 80% sustained

## ✅ Success Criteria

### Rate Limiting Test

- ✅ P95 response time < 500ms
- ✅ ~30% requests get 429 (rate limited)
- ✅ No 500 errors

### Bet Placement Test

- ✅ P95 response time < 2s
- ✅ < 5% failure rate
- ✅ Idempotency works (see replays in metrics)

### Settlement Test

- ✅ P95 settlement < 5s
- ✅ Lock wait < 2s
- ✅ Conflicts properly detected (409)

### Odds Burst Test

- ✅ P95 batch processing < 3s
- ✅ Sustained throughput > 500 updates/sec
- ✅ No timeout errors

## 🔍 Debugging

### Services won't start

```bash
# Check status
docker compose -f docker-compose.loadtest.yml ps

# View logs
docker compose -f docker-compose.loadtest.yml logs app
docker compose -f docker-compose.loadtest.yml logs postgres
```

### App not healthy

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# View app logs
docker compose -f docker-compose.loadtest.yml logs -f app
```

### Grafana shows no data

1. Check Prometheus targets: [http://localhost:9090/targets](http://localhost:9090/targets)
  - Should show `sportsbetting-app` as UP
2. Check app metrics: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
  - Should return metrics text
3. Refresh Grafana dashboard (top right)

### Out of memory

Increase Docker Desktop memory:

- Settings → Resources → Memory → 10GB+

## 🧹 Cleanup

```bash
# Stop all services
docker compose -f docker-compose.loadtest.yml down

# Remove volumes (clears database)
docker compose -f docker-compose.loadtest.yml down -v

# Full cleanup (removes images too)
docker compose -f docker-compose.loadtest.yml down -v --rmi all
```

## 🎓 Next Steps

1. **Baseline Results** - Run tests, save results as baseline
2. **Tune Parameters** - Adjust DB pool, JVM heap, lock timeouts
3. **Scale Testing** - Test with multiple app instances
4. **Production Setup** - Deploy to AWS/GCP for realistic testing

## 📚 Detailed Documentation

See `loadtest/README.md` for:

- Detailed test descriptions
- PromQL query examples
- Database performance monitoring
- Resource limit tuning
- Microservice architecture options

## 🆘 Quick Troubleshooting Commands

```bash
# Is app responding?
curl http://localhost:8080/actuator/health

# See all running containers
docker compose -f docker-compose.loadtest.yml ps

# Restart just the app
docker compose -f docker-compose.loadtest.yml restart app

# View app metrics
curl http://localhost:8080/actuator/prometheus | grep bets_placed

# Check database connections
docker compose -f docker-compose.loadtest.yml exec postgres \
  psql -U sportsbetting -c "SELECT count(*) FROM pg_stat_activity;"

# Check Redis
docker compose -f docker-compose.loadtest.yml exec redis redis-cli ping
```

## 💡 Pro Tips

1. **Start small** - Run rate-limit test first, it's the quickest
2. **Watch Grafana live** - Open dashboard before starting tests
3. **Run tests individually** - Easier to identify bottlenecks
4. **Check logs** - If metrics look weird, check app logs
5. **Resource limits** - Monitor Docker Desktop resource usage

## 🎉 You're Ready!

Your complete load testing harness is set up. Start with:

```bash
docker compose -f docker-compose.loadtest.yml up -d
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit
```

Then open [http://localhost:3000](http://localhost:3000) and watch the magic! 🚀