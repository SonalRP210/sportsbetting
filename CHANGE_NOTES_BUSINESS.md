# Business-Focused Change Notes

This document explains why major technical changes were made and what business risk each change addresses.

## Why `@EnableConfigurationProperties` was added

`RateLimitProperties` uses `@ConfigurationProperties(prefix = "app.rate-limit")` to bind YAML settings into a typed Java class.

We added `@EnableConfigurationProperties(RateLimitProperties.class)` in `SportsBettingApplication` so Spring Boot reliably registers and wires that properties bean at startup.

### Business value

- Allows rate-limit thresholds to be changed by config instead of code edits.
- Makes environments safer (different limits in local/test/prod).
- Reduces production incidents caused by hardcoded operational values.

## Service-layer split (SRP) and interface-based design

### What changed

- Split monolithic service into focused responsibilities:
  - bet placement
  - odds ingestion
  - settlement
  - exposure
  - query/read operations
- Introduced interfaces for each service and kept a thin orchestration facade.

### Why this was needed

- A single large service increases change risk and regression probability.
- Tight coupling makes testing and onboarding slower.
- Production incidents become harder to isolate and fix.

### Business value

- Faster and safer feature delivery.
- Lower maintenance and incident resolution time.
- Cleaner separation for team ownership and scaling.

## JPA migration and persistence model

### What changed

- Converted `Bet` to a JPA entity.
- Replaced in-memory repository with `JpaRepository`.
- Added H2 datasource for runtime/testing setup.

### Why this was needed

- In-memory state loses all bets on restart.
- No durable source of truth for settlement and audit.
- In-memory data model is not production-like and weak for compliance discussions.

### Business value

- Durable and queryable data model.
- Better foundation for auditability and regulatory requirements.
- Easier move to production databases with minimal code changes.

## Rate limiting

### What changed

- Added a request filter with configurable fixed-window throttling.
- Added dedicated exception and HTTP `429` response mapping.

### Why this was needed

- Prevents abusive clients and accidental traffic spikes from degrading service.
- Protects hot endpoints (bet placement, settlement) under load.

### Business value

- Better uptime and fairness across customers.
- Lower cost of traffic bursts and abusive patterns.
- More predictable system behavior during peak events.

## Observability baseline (logs, metrics, correlation)

### What changed

- Enabled INFO logging (instead of turning logs off).
- Added request correlation via `X-Request-Id` and MDC.
- Added actuator + metrics counters/gauge for key flows.
- Added management endpoint exposure for health/metrics.

### Why this was needed

- Without telemetry, failures are hard to detect and triage.
- No correlation ID means cross-service debugging is slow.
- No metrics means no data for SLO/SLA and capacity planning.

### Business value

- Faster incident detection and root-cause analysis.
- Better operational visibility during match-day traffic.
- Enables data-driven reliability improvements.

## Correctness and safety improvements in domain flow

### What changed

- Removed random rejection and blocking sleep from core flow.
- Removed default silent odds fallback.
- Ensured settlement processes only OPEN bets.
- Preserved historical accepted odds on placed bets.
- Improved exposure accounting consistency.

### Why this was needed

- Nondeterministic behavior creates test flakiness and production surprises.
- Silent defaults hide data issues.
- Invalid state transitions can cause financial errors.

### Business value

- More predictable outcomes for users and operations.
- Reduced risk of payout/exposure mistakes.
- Stronger trust in core betting lifecycle behavior.

## Idempotency (bet placement and settlement)

### What changed

- **Bets**: Optional HTTP header **`Idempotency-Key`** on place-bet; stored with the bet under a **unique (user_id, idempotency_key)** constraint. Duplicate requests return the same logical outcome without duplicating exposure.
- **Events**: **`EventSettlement`** table keyed by `event_id`. Replaying settlement with the same winner returns the stored outcome; a **different** winner after a successful settle is rejected as a conflict.

### Why this was needed

- Networks and clients retry; without idempotency, duplicate stakes and incorrect exposure are a material financial risk.
- Settlement must not be applied twice to the same economic outcome; operators must get a clear signal if someone attempts an incompatible second result.

### Business value

- Safer retries from gateways, mobile apps, and internal jobs.
- Clearer support and audit story (“this event already closed as X”).
- Reduced duplicate payout and dispute risk.

## Concurrency controls on settlement

### What changed

- Open bets for an event are loaded with **pessimistic write locks** before status updates.
- **`@Version`** on `Bet` for optimistic locking on concurrent updates to the same row.

### Why this was needed

- Two concurrent settlement requests could otherwise both observe `OPEN` and corrupt liability accounting.

### Business value

- Stronger correctness under peak load (e.g. final whistle + automated settlement).
- Fail-fast semantics (HTTP 409) when a concurrent write wins instead of silent corruption.

## Persistence retries (transient failures only)

### What changed

- **`RetryTemplate`** (Spring Retry) configured to retry a small number of times on **`TransientDataAccessException`** subclasses.
- Bet **persist** path uses this template so brief DB glitches can recover without the client guessing whether the bet was stored.

### Why this was needed

- Transient infrastructure faults should not immediately become user-visible hard failures if a short retry can succeed.

### Business value

- Fewer false negatives on bet acceptance during short blips.
- Explicit separation between **retryable** infra errors and **non-retryable** business errors (e.g. missing odds).

## Richer API error contract

### What changed

- Additional exception types mapped to HTTP status: e.g. settlement conflict (**409**), transient dependency (**503**), optimistic lock (**409**), duplicate / integrity issues (**409**).
- **`ApiErrorResponse`** extended with optional **`traceId`** aligned to request correlation (MDC).

### Why this was needed

- Clients and operators need stable **codes** and correlation to triage without parsing unstructured text.

### Business value

- Faster incident correlation across logs and support tickets.
- Clearer client behavior (retry vs fix payload vs escalate).

## Notes for interviews

Use this framing when asked "why these changes":

1. Protect financial correctness first.
2. Improve operability (know when things break).
3. Reduce coupling so teams can ship safely.
4. Move from toy persistence to durable domain data.
5. Make runtime controls configurable, not hardcoded.
6. Treat retries and idempotency as first-class for any money-moving API.
7. Prefer explicit conflicts (409) over silent double application of settlement.
