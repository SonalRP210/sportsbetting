# Service Level Objectives (SLO) - Sports Betting API

## Purpose

This document defines a practical SLO baseline for the Sports Betting service using the metrics and load-testing harness already present in this repository.

It is intended for internal reliability management. It is not a customer-facing SLA.

## Scope

Service:

- `sportsbetting` Spring Boot API (`/api/v1`)

Critical user journeys:

- Place bet: `POST /api/v1/bets`
- Settle event: `POST /api/v1/events/settlements`
- Exposure query: `GET /api/v1/users/{userId}/exposure`
- Odds ingestion: `POST /api/v1/odds-feed`

## Ownership

- Primary owner: Backend service owner (Sports Betting team)
- Operational owner: Platform/DevOps or SRE function (if assigned)
- Product stakeholder: Product/Business owner for betting workflows

## SLI Definitions

The following SLIs are measured from existing Micrometer/k6/Prometheus data.

1. Availability (request success rate)

- Definition: `1 - error_rate`
- Error classes included:
  - 5xx responses
  - Explicit test failure metric in k6 (`k6_http_req_failed_rate`) for load runs
- Data source:
  - Runtime: `http_server_requests_seconds_*`
  - Load tests: `k6_http_req_failed_rate`

1. Latency

- Definition: P95 and P99 response times
- Data source:
  - Runtime: `http_server_requests_seconds_*`
  - Load tests: `k6_http_req_duration_p95`, `k6_http_req_duration_p99`

1. Settlement lock contention

- Definition: P95 lock wait and lock timeout count
- Data source:
  - `events_settlement_lock_wait`
  - `events_settlement_lock_failures`

1. Functional correctness proxies

- Definition:
  - Settlement conflict handling remains explicit (`events_settled_conflicts`)
  - Idempotency replay paths remain effective
- Data source:
  - `bets_placed_idempotent_replay`
  - `bets_placed_idempotent_replay_after_race`
  - `bets_placed_idempotency_constraint_violations`

## SLO Targets (Initial Baseline)

These targets are intentionally aligned to current load-test criteria and should be revised after 2-4 weeks of production-like data.

### Global API SLOs

- Availability: >= 99.5% over rolling 30 days
- Latency P95: <= 500 ms for read-heavy endpoints over rolling 7 days
- Latency P95: <= 2 s for bet placement over rolling 7 days
- 5xx rate: <= 0.5% over rolling 7 days

### Settlement SLOs

- Settlement latency P95: <= 5 s over rolling 7 days
- Settlement lock wait P95: <= 2 s over rolling 7 days
- Settlement lock timeout failures: near-zero; investigate any sustained non-zero trend

### Odds Ingestion SLOs

- Batch processing latency P95: <= 3 s under test profile
- Failure rate: <= 5% under load-test profile

## Error Budget Policy

For the 30-day availability target of 99.5%:

- Error budget = 0.5% unavailable requests

Burn handling:

- Fast burn (critical): consume >= 10% of monthly budget in <= 24 hours -> incident and immediate mitigation
- Medium burn: consume >= 25% of monthly budget in <= 7 days -> reliability review and next-sprint action items
- Slow burn: any sustained degradation trend -> backlog hardening tasks

## Measurement Windows

- Dashboards: real-time + short windows (1m/5m) for troubleshooting
- Alert windows: 5m/30m for fast detection and noise control
- Reporting windows:
  - Weekly reliability summary
  - Monthly SLO compliance report
  - Quarterly target recalibration

## PromQL Reference (Starting Point)

Note: Exact queries may require adjustments based on label cardinality in your environment.

Availability proxy from k6:

- `(1 - avg(last_over_time(k6_http_req_failed_rate[2m]))) * 100`

Latency proxy from k6:

- `avg(last_over_time(k6_http_req_duration_p95[2m])) * 1000`
- `avg(last_over_time(k6_http_req_duration_p99[2m])) * 1000`

Settlement latency (runtime):

- `histogram_quantile(0.95, sum(rate(events_settled_duration_bucket[5m])) by (le))`

Settlement lock wait:

- `histogram_quantile(0.95, sum(rate(events_settlement_lock_wait_bucket[5m])) by (le))`

Settlement lock failures:

- `sum(rate(events_settlement_lock_failures[5m]))`

Idempotency replay rate:

- `sum(rate(bets_placed_idempotent_replay[5m])) + sum(rate(bets_placed_idempotent_replay_after_race[5m]))`

## Review and Validation Cadence

1. Per release

- Run the load-test suite (`loadtest/run-all-tests.ps1` or `.sh`)
- Verify SLO proxy panels and check threshold breaches
- Record notable regressions in release notes

1. Weekly

- Review dashboard trends (latency, failures, lock contention, resource saturation)
- Confirm no hidden degradation in dependency metrics (DB/Kafka where enabled)

1. Monthly

- Publish SLO compliance summary
- Track error budget consumption and recurring incident classes

1. Quarterly

- Revisit targets based on business criticality, traffic profile, and architecture changes

## Escalation Triggers

- Availability below target in rolling 7-day view
- Settlement lock timeout failures trending up
- Persistent P95 regression after a release
- Repeated failure spikes during known peak windows

## Related Documents

- `README.md` (service and observability overview)
- `LOADTEST_QUICKSTART.md` (quick load test and pass criteria)
- `loadtest/README.md` (detailed load test scenarios and metrics)
- `CHANGE_NOTES_BUSINESS.md` (business-risk rationale for reliability controls)

## SLA Note

No external SLA is currently defined in this repository.

If an external SLA is required, create `docs/SLA.md` with:

- contractual commitments
- exclusions and maintenance windows
- support response targets
- penalties/service credits
- legal/commercial approval workflow

