# Load Testing Structure

Complete file organization for the load testing harness.

## Directory Structure

```
sportsbetting/
├── docker-compose.loadtest.yml          ← Main orchestration file
├── Dockerfile                            ← Multi-stage app build
├── LOADTEST_QUICKSTART.md               ← Quick start guide (start here!)
│
├── loadtest/
│   ├── README.md                         ← Detailed documentation
│   ├── STRUCTURE.md                      ← This file
│   │
│   ├── prometheus.yml                    ← Prometheus config
│   │
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/
│   │   │   │   └── prometheus.yml        ← Auto-configure Prometheus datasource
│   │   │   └── dashboards/
│   │   │       └── dashboard.yml         ← Auto-load dashboards
│   │   └── dashboards/
│   │       └── sportsbetting-loadtest.json  ← Pre-built dashboard
│   │
│   ├── k6/
│   │   ├── rate-limit-test.js            ← Test 1: Rate limiting
│   │   ├── bet-placement-test.js         ← Test 2: Bet placement
│   │   ├── settlement-test.js            ← Test 3: Settlement contention
│   │   ├── odds-burst-test.js            ← Test 4: Odds burst
│   │   └── results/                      ← Test results (gitignored)
│   │       └── .gitkeep
│   │
│   ├── run-all-tests.sh                  ← Linux/Mac test runner
│   ├── run-all-tests.ps1                 ← Windows test runner
│   └── check-status.ps1                  ← Status checker
│
└── src/main/resources/
    └── application-loadtest.yaml         ← Load test Spring profile
```

## File Purposes

### Core Files


| File                          | Purpose                                                       |
| ----------------------------- | ------------------------------------------------------------- |
| `docker-compose.loadtest.yml` | Orchestrates all services (app, DB, Redis, Kafka, monitoring) |
| `Dockerfile`                  | Multi-stage build for Spring Boot app                         |
| `LOADTEST_QUICKSTART.md`      | Quick start guide - read this first!                          |


### Configuration


| File                                                       | Purpose                                         |
| ---------------------------------------------------------- | ----------------------------------------------- |
| `loadtest/prometheus.yml`                                  | Configures Prometheus to scrape app metrics     |
| `loadtest/grafana/provisioning/datasources/prometheus.yml` | Auto-connects Grafana to Prometheus             |
| `loadtest/grafana/provisioning/dashboards/dashboard.yml`   | Auto-loads dashboards on Grafana startup        |
| `src/main/resources/application-loadtest.yaml`             | Optimized Spring Boot settings for load testing |


### Dashboards


| File                                                      | Purpose                                   |
| --------------------------------------------------------- | ----------------------------------------- |
| `loadtest/grafana/dashboards/sportsbetting-loadtest.json` | Pre-built Grafana dashboard with 9 panels |


### Load Tests


| Test Script             | Duration | What It Tests                                 |
| ----------------------- | -------- | --------------------------------------------- |
| `rate-limit-test.js`    | 3.5 min  | Rate limiting under high concurrent load      |
| `bet-placement-test.js` | 5 min    | Bet creation with DB contention & idempotency |
| `settlement-test.js`    | 2 min    | Pessimistic locking on hot events             |
| `odds-burst-test.js`    | 3.5 min  | High-frequency odds updates                   |


### Helper Scripts


| Script              | Platform  | Purpose                                  |
| ------------------- | --------- | ---------------------------------------- |
| `run-all-tests.sh`  | Linux/Mac | Runs all 4 tests sequentially            |
| `run-all-tests.ps1` | Windows   | Runs all 4 tests sequentially            |
| `check-status.ps1`  | Windows   | Checks service health and resource usage |


## Docker Services

### Infrastructure Services


| Service     | Port | Resource Limits    | Purpose                 |
| ----------- | ---- | ------------------ | ----------------------- |
| `postgres`  | 5432 | 2 CPU, 2GB RAM     | PostgreSQL database     |
| `redis`     | 6379 | 1 CPU, 512MB RAM   | Rate limiting + pub/sub |
| `zookeeper` | 2181 | 0.5 CPU, 512MB RAM | Kafka coordination      |
| `kafka`     | 9092 | 1 CPU, 1GB RAM     | Optional odds buffering |


### Application Services


| Service | Port | Resource Limits | Purpose                 |
| ------- | ---- | --------------- | ----------------------- |
| `app`   | 8080 | 2 CPU, 3GB RAM  | Spring Boot application |


### Monitoring Services


| Service      | Port | Resource Limits    | Purpose               |
| ------------ | ---- | ------------------ | --------------------- |
| `prometheus` | 9090 | 1 CPU, 1GB RAM     | Metrics aggregation   |
| `grafana`    | 3000 | 0.5 CPU, 512MB RAM | Metrics visualization |


### Load Testing Services


| Service         | Purpose                   | Run Mode           |
| --------------- | ------------------------- | ------------------ |
| `k6-rate-limit` | Rate limit test runner    | Manual (on-demand) |
| `k6-bets`       | Bet placement test runner | Manual (on-demand) |
| `k6-settlement` | Settlement test runner    | Manual (on-demand) |
| `k6-odds`       | Odds burst test runner    | Manual (on-demand) |


## Metrics Flow

```
┌─────────────────────────────────────────────────┐
│  Spring Boot App                                │
│  ─────────────────                              │
│  • MeterRegistry (Micrometer)                   │
│  • Custom metrics (counters, timers, gauges)    │
│  • JVM metrics (heap, GC, threads)              │
│  • HTTP metrics (request rate, latency)         │
│                                                  │
│  Exposes: /actuator/prometheus                  │
└─────────────────────┬───────────────────────────┘
                      │
                      │ HTTP scrape every 5s
                      ▼
┌─────────────────────────────────────────────────┐
│  Prometheus                                     │
│  ──────────                                     │
│  • Time-series database                         │
│  • Stores 24h of metrics                        │
│  • PromQL query engine                          │
│                                                  │
│  URL: http://localhost:9090                     │
└─────────────────────┬───────────────────────────┘
                      │
                      │ Prometheus datasource
                      ▼
┌─────────────────────────────────────────────────┐
│  Grafana Dashboard                              │
│  ─────────────────                              │
│  • 9 pre-configured panels                      │
│  • Auto-refresh every 5s                        │
│  • PromQL queries for aggregations              │
│                                                  │
│  URL: http://localhost:3000                     │
│  Login: admin/admin                             │
└─────────────────────────────────────────────────┘
```

## Key Metrics

### Business Metrics


| Metric Name                                     | Type    | Source                     | What It Measures                |
| ----------------------------------------------- | ------- | -------------------------- | ------------------------------- |
| `bets_placed_total`                             | Counter | DefaultBetPlacementService | Total bets placed               |
| `bets_placed_idempotent_replay`                 | Counter | DefaultBetPlacementService | Duplicate requests caught early |
| `bets_placed_idempotent_replay_after_race`      | Counter | DefaultBetPlacementService | Race condition recoveries       |
| `bets_placed_idempotency_constraint_violations` | Counter | DefaultBetPlacementService | DB constraint violations        |
| `events_settled_total`                          | Counter | DefaultSettlementService   | Events settled                  |
| `events_settled_duration`                       | Timer   | DefaultSettlementService   | Settlement latency              |
| `events_settlement_lock_wait`                   | Timer   | DefaultSettlementService   | Lock wait time                  |
| `events_settlement_lock_failures`               | Counter | DefaultSettlementService   | Lock timeout failures           |
| `events_settled_conflicts`                      | Counter | DefaultSettlementService   | Settlement conflicts            |
| `bets_exposure_total`                           | Gauge   | DefaultExposureService     | Current total exposure          |


### JVM Metrics (Auto-generated)


| Metric Name                    | Type  | What It Measures     |
| ------------------------------ | ----- | -------------------- |
| `jvm_memory_used_bytes`        | Gauge | JVM heap usage       |
| `jvm_memory_max_bytes`         | Gauge | JVM max heap         |
| `process_cpu_usage`            | Gauge | Process CPU %        |
| `system_cpu_usage`             | Gauge | System CPU %         |
| `http_server_requests_seconds` | Timer | HTTP request latency |


### k6 Metrics (Test Results)


| Metric Name             | Type    | What It Measures             |
| ----------------------- | ------- | ---------------------------- |
| `http_req_duration`     | Trend   | Request latency distribution |
| `http_req_failed`       | Rate    | Request failure rate         |
| `http_reqs`             | Counter | Total HTTP requests          |
| `vus`                   | Gauge   | Current virtual users        |
| `rate_limit_errors`     | Rate    | Rate limiting frequency      |
| `bets_placed`           | Counter | Test: bets created           |
| `settlements_succeeded` | Counter | Test: successful settlements |


## Typical Workflow

### First Time Setup

```bash
# 1. Build Docker image
docker compose -f docker-compose.loadtest.yml build

# 2. Start all services
docker compose -f docker-compose.loadtest.yml up -d

# 3. Wait for healthy
sleep 30

# 4. Check status
.\loadtest\check-status.ps1

# 5. Open Grafana
start http://localhost:3000
```

### Running Tests

```bash
# Option A: Run individual test
docker compose -f docker-compose.loadtest.yml run --rm k6-bets

# Option B: Run all tests
.\loadtest\run-all-tests.ps1
```

### Monitoring

```bash
# Watch live metrics
start http://localhost:3000

# View Prometheus data
start http://localhost:9090

# Check app logs
docker compose -f docker-compose.loadtest.yml logs -f app
```

### Cleanup

```bash
# Stop services
docker compose -f docker-compose.loadtest.yml down

# Remove data volumes
docker compose -f docker-compose.loadtest.yml down -v
```

## Customization Points

### Adjust Load Test Intensity

Edit k6 test files:

- `options.stages` - VU ramp profile
- `options.thresholds` - Pass/fail criteria
- Sleep durations - Think time between requests

### Tune Application Performance

Edit `application-loadtest.yaml`:

- `hikari.maximum-pool-size` - DB connection pool
- Increase in `docker-compose.loadtest.yml` → `JAVA_OPTS`

### Change Resource Limits

Edit `docker-compose.loadtest.yml`:

- Under each service → `deploy.resources.limits`

### Enable/Disable Features

Edit `docker-compose.loadtest.yml` environment variables:

- `APP_REDIS_ENABLED` - Redis rate limiting + pub/sub
- `APP_KAFKA_ODDS_ENABLED` - Kafka odds buffering

## Ports Reference


| Port | Service     | URL                                            |
| ---- | ----------- | ---------------------------------------------- |
| 3000 | Grafana     | [http://localhost:3000](http://localhost:3000) |
| 5432 | PostgreSQL  | jdbc:postgresql://localhost:5432/sportsbetting |
| 6379 | Redis       | redis://localhost:6379                         |
| 8080 | Application | [http://localhost:8080](http://localhost:8080) |
| 9090 | Prometheus  | [http://localhost:9090](http://localhost:9090) |
| 9092 | Kafka       | localhost:9092                                 |


## Next Steps

1. **Read** `LOADTEST_QUICKSTART.md` for getting started
2. **Read** `loadtest/README.md` for detailed documentation
3. **Run** your first test with `k6-rate-limit`
4. **Monitor** in Grafana dashboard
5. **Analyze** results and tune configuration

