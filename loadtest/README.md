# Sports Betting Load Testing Harness

Complete load testing setup with Docker Compose, k6, Prometheus, and Grafana.

## Architecture

```
┌─────────────────┐
│   k6 Tests      │  ← Load generators (4 test types)
└────────┬────────┘
         │ HTTP
         ▼
┌─────────────────┐
│  Sports Betting │  ← Spring Boot app (your code)
│  Application    │
├─────────────────┤
│  PostgreSQL     │  ← Persistent storage
│  Redis          │  ← Rate limiting + pub/sub
│  Kafka          │  ← Optional odds buffering
└────────┬────────┘
         │ Metrics
         ▼
┌─────────────────┐
│  Prometheus     │  ← Metrics aggregation
│  Grafana        │  ← Visualization
└─────────────────┘
```

## Prerequisites

- Docker Desktop with Docker Compose
- At least 8GB RAM available for Docker
- Ports available: 8080, 5432, 6379, 9092, 9090, 3000

## Quick Start

### 1. Build the Application

```bash
# Build the Docker image
docker compose -f docker-compose.loadtest.yml build
```

### 2. Start the Infrastructure

```bash
# Start all services (app, postgres, redis, kafka, prometheus, grafana)
docker compose -f docker-compose.loadtest.yml up -d

# Wait for services to be healthy (~30 seconds)
docker compose -f docker-compose.loadtest.yml ps
```

### 3. Access Dashboards

- **Grafana**: [http://localhost:3000](http://localhost:3000) (admin/admin)
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Application**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Metrics**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)

### 4. Run Load Tests

Run each test individually:

```bash
# Test 1: Rate Limiting (3.5 minutes)
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit

# Test 2: Bet Placement (5 minutes)
docker compose -f docker-compose.loadtest.yml run --rm k6-bets

# Test 3: Settlement Contention (2 minutes)
docker compose -f docker-compose.loadtest.yml run --rm k6-settlement

# Test 4: Odds Burst (3.5 minutes)
docker compose -f docker-compose.loadtest.yml run --rm k6-odds
```

### 5. View Results in Grafana

1. Open Grafana: [http://localhost:3000](http://localhost:3000)
2. Navigate to **Dashboards** → **Load Testing** → **Sports Betting Load Test Dashboard**
3. Watch metrics update in real-time as tests run

## Load Test Details

### Test 1: Rate Limiting

**Purpose**: Verify rate limiting works under high concurrent load

**Test Profile**:

- Ramps from 50 → 200 virtual users over 4 minutes
- Targets `/api/v1/users/{userId}/exposure` endpoint
- Expected: ~30% requests rate-limited (429 responses)

**Key Metrics**:

- `rate_limit_errors` - Percentage of 429 responses
- `request_duration_ms` - Response time distribution
- `total_requests` - Throughput

**Success Criteria**:

- P95 response time < 500ms
- Rate limit kicks in as expected
- No 500 errors

---

### Test 2: Bet Placement

**Purpose**: Test concurrent bet creation with DB contention

**Test Profile**:

- Ramps from 20 → 150 virtual users over 5 minutes
- Creates bets on multiple events simultaneously
- 10% intentional duplicates to test idempotency

**Key Metrics**:

- `bets_placed_total` - Successful bet placements
- `bets_placed_idempotent_replay` - Duplicate requests caught
- `bets_placed_idempotency_constraint_violations` - DB constraint hits
- `bet_placement_duration_ms` - Latency distribution

**Success Criteria**:

- P95 response time < 2s
- < 5% failure rate
- Idempotency works (replays return original bet)

**Contention Points**:

- PostgreSQL `bet` table INSERT concurrency
- Unique constraint `(user_id, idempotency_key)`
- Outbox table writes
- Exposure projection updates

---

### Test 3: Settlement Contention

**Purpose**: Test pessimistic locking on hot events

**Test Profile**:

- Phase 1: 50 VUs create ~500 bets on ONE event (30 seconds)
- Phase 2: 10 VUs attempt concurrent settlement (1 minute)
- Tests worst-case lock contention scenario

**Key Metrics**:

- `events_settlement_lock_wait` - Time waiting for locks
- `events_settlement_lock_failures` - Lock timeout failures
- `events_settled_duration` - Total settlement time
- `events_settled_conflicts` - Duplicate settlements with different winners

**Success Criteria**:

- P95 settlement time < 5s
- Lock timeout < 2s (configured value)
- Conflicts properly detected (409 response)

**Contention Points**:

- PostgreSQL row-level locks (`PESSIMISTIC_WRITE`)
- Locking 500+ bet rows simultaneously
- Settlement retry logic

---

### Test 4: Odds Burst

**Purpose**: Test high-frequency odds ingestion

**Test Profile**:

- Ramps from 10 → 100 requests/sec over 3 minutes
- Each request sends 5-25 odds updates
- Peak: ~2000 odds updates/second

**Two Modes**:

1. **Direct Path** (`APP_KAFKA_ODDS_ENABLED=false`): Synchronous processing
2. **Kafka Buffered** (`APP_KAFKA_ODDS_ENABLED=true`): Async with backpressure

**Key Metrics**:

- `odds_updates_succeeded` - Total odds processed
- `odds_batch_duration_ms` - Batch processing time
- `odds_per_second` - Throughput

**Success Criteria**:

- P95 batch processing < 3s
- < 5% failure rate
- System doesn't fall over under burst

**Contention Points**:

- PostgreSQL `latest_odds` table UPSERT operations
- Redis pub/sub message rate
- Outbox event writes

---

## Monitoring & Observability

### Grafana Dashboards

The pre-configured dashboard shows:

1. **Request Throughput** - Bets/sec, settlements/sec
2. **Total Bets Placed** - Cumulative counter
3. **Current Exposure** - Live exposure gauge
4. **Settlement Latency** - P95/P99 timings
5. **Lock Contention** - Settlement lock wait times
6. **Idempotency Events** - Replay/race/constraint violations
7. **Settlement Failures** - Lock timeouts and conflicts
8. **JVM Heap** - Memory usage
9. **CPU Usage** - Process and system CPU

### Prometheus Queries

Useful PromQL queries:

```promql
# Request rate
rate(bets_placed_total[1m])

# Error rate
rate(events_settlement_lock_failures[1m])

# P95 settlement latency
histogram_quantile(0.95, sum(rate(events_settled_duration_bucket[1m])) by (le))

# Lock wait time
histogram_quantile(0.95, sum(rate(events_settlement_lock_wait_bucket[1m])) by (le))

# Idempotency replay rate
rate(bets_placed_idempotent_replay[1m])
```

### Application Logs

View live logs:

```bash
# Application logs
docker compose -f docker-compose.loadtest.yml logs -f app

# PostgreSQL logs
docker compose -f docker-compose.loadtest.yml logs -f postgres

# Kafka logs
docker compose -f docker-compose.loadtest.yml logs -f kafka
```

## Advanced Usage

### Running Tests with Different Configurations

#### Test with Redis Enabled (Global Rate Limiting)

```bash
# Edit docker-compose.loadtest.yml
# Set: APP_REDIS_ENABLED: "true"

docker compose -f docker-compose.loadtest.yml up -d app
docker compose -f docker-compose.loadtest.yml run --rm k6-rate-limit
```

#### Test with Kafka Odds Buffering

```bash
# Edit docker-compose.loadtest.yml
# Set: APP_KAFKA_ODDS_ENABLED: "true"

docker compose -f docker-compose.loadtest.yml restart app
docker compose -f docker-compose.loadtest.yml run --rm k6-odds
```

### Custom k6 Test Parameters

Run k6 with custom VUs and duration:

```bash
# Custom rate limit test: 300 VUs for 5 minutes
docker compose -f docker-compose.loadtest.yml run --rm \
  -e K6_VUS=300 \
  -e K6_DURATION=5m \
  k6-rate-limit run --vus 300 --duration 5m /scripts/rate-limit-test.js
```

### Database Performance Monitoring

Connect to PostgreSQL and run:

```bash
# Connect to database
docker compose -f docker-compose.loadtest.yml exec postgres psql -U sportsbetting

# See active locks
SELECT 
  pid,
  usename,
  pg_blocking_pids(pid) as blocked_by,
  wait_event_type,
  query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock';

# See slow queries (requires pg_stat_statements extension)
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

SELECT 
  calls,
  mean_exec_time,
  max_exec_time,
  query
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

# See table sizes
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Resource Limits

Current Docker resource limits:


| Service    | CPU | Memory |
| ---------- | --- | ------ |
| app        | 2.0 | 3GB    |
| postgres   | 2.0 | 2GB    |
| redis      | 1.0 | 512MB  |
| kafka      | 1.0 | 1GB    |
| prometheus | 1.0 | 1GB    |
| grafana    | 0.5 | 512MB  |


Adjust in `docker-compose.loadtest.yml` under `deploy.resources`.

## Interpreting Results

### Good Performance Indicators

✅ **Rate Limiting Test**:

- P95 < 500ms
- ~30% rate limit errors (expected)
- No 500 errors

✅ **Bet Placement Test**:

- P95 < 2s
- < 5% failures
- Idempotent replays work correctly

✅ **Settlement Test**:

- P95 settlement < 5s
- Lock wait < 2s
- Proper conflict detection

✅ **Odds Burst Test**:

- P95 batch < 3s
- Sustained throughput > 500 updates/sec
- No timeouts

### Warning Signs

⚠️ **High lock contention**:

- `events_settlement_lock_wait` P95 > 1.5s
- `events_settlement_lock_failures` increasing
- **Fix**: Tune lock timeout, add retries, or reduce concurrent settlements

⚠️ **High idempotency constraint violations**:

- `bets_placed_idempotency_constraint_violations` > 10% of total
- **Cause**: Client retries, network issues
- **Expected**: Some violations are normal; recovery should work

⚠️ **Memory growth**:

- JVM heap continuously increasing
- **Fix**: Check for memory leaks, tune GC settings

⚠️ **Database connection exhaustion**:

- Errors like "connection pool exhausted"
- **Fix**: Increase `spring.datasource.hikari.maximum-pool-size`

## Cleanup

```bash
# Stop all services
docker compose -f docker-compose.loadtest.yml down

# Remove volumes (clears database)
docker compose -f docker-compose.loadtest.yml down -v

# Remove everything including images
docker compose -f docker-compose.loadtest.yml down -v --rmi all
```

## Troubleshooting

### Services won't start

```bash
# Check service health
docker compose -f docker-compose.loadtest.yml ps

# View logs for specific service
docker compose -f docker-compose.loadtest.yml logs app
docker compose -f docker-compose.loadtest.yml logs postgres
```

### k6 tests fail immediately

```bash
# Make sure app is healthy first
curl http://localhost:8080/actuator/health

# Check if app can reach database
docker compose -f docker-compose.loadtest.yml exec app curl -f postgres:5432
```

### Grafana dashboard not showing data

1. Check Prometheus is scraping: [http://localhost:9090/targets](http://localhost:9090/targets)
2. Verify app metrics endpoint: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
3. Check Prometheus datasource in Grafana (Configuration → Data Sources)

### Out of memory errors

Increase Docker Desktop memory limit:

- Docker Desktop → Settings → Resources → Memory
- Recommended: 10GB+ for full test suite

## Next Steps

1. **Run baseline tests** - Establish performance baseline numbers
2. **Tune configuration** - Adjust DB pool, JVM heap, lock timeouts
3. **Scale horizontally** - Test with multiple app instances
4. **Production testing** - Deploy to AWS/GCP for realistic load

## References

- [k6 Documentation](https://k6.io/docs/)
- [Prometheus Querying](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)

