# Architecture Evolution Log

This is the living architecture document for the Sports Betting service.

It has two purposes:

1. Capture the current architecture as-is.
2. Record each scalability evolution step in the same file as we implement it.

We will update this document incrementally after each change so architecture and code stay aligned.

## Current Architecture (Baseline)

### Runtime model

- Spring Boot (Spring MVC, blocking request/response).
- Request-level concurrency is provided by the servlet container thread pool.
- Business services are synchronous and transaction-based (`@Transactional`).
- No explicit async pipelines (`@Async`, `CompletableFuture`, schedulers, reactive streams) in the current implementation.

### Core components and responsibilities

- `DefaultOddsService`
  - Stores latest odds in an in-memory `ConcurrentHashMap`.
  - Reads are local to the current JVM instance.
- `DefaultBetPlacementService`
  - Places bets with optional idempotency (`Idempotency-Key`).
  - Uses DB unique constraint (`user_id`, `idempotency_key`) to prevent duplicate logical bets.
  - Uses retry template for transient persistence errors.
- `DefaultSettlementService`
  - Settles open bets for an event.
  - Uses DB row locking (`PESSIMISTIC_WRITE`) on open event bets during settlement.
  - Writes settlement ledger (`EventSettlement`) for idempotent replay and conflict detection.
- `DefaultExposureService`
  - Maintains total exposure as in-memory `AtomicReference<BigDecimal>`.
  - Provides user exposure by querying open bets and aggregating risk.
- `RateLimitingFilter`
  - Uses in-memory `ConcurrentHashMap` + atomic counters for per-client window tracking.

### Data consistency and concurrency controls

- **Idempotency for bet placement**
  - Implemented via DB uniqueness + replay/recovery on race.
- **Settlement idempotency**
  - Implemented via `EventSettlement` ledger keyed by `eventId`.
  - Same winner => replay; different winner => conflict (`409`).
- **Pessimistic locking**
  - Settlement path uses `@Lock(PESSIMISTIC_WRITE)` to serialize conflicting updates for open bets.
- **Optimistic locking**
  - `Bet` entity uses `@Version` field (`opt_lock`) for concurrent update detection.
- **Transient retry**
  - `RetryTemplate` retries transient persistence exceptions with fixed backoff.

### Current scaling characteristics

- Strong correctness for core write flows (bet idempotency, settlement serialization) because safety is DB-backed.
- In-memory components are node-local and therefore not globally consistent in multi-instance deployments:
  - odds cache
  - rate-limit counters
  - exposure accumulator

### Known architectural limits

- Global rate limiting is not enforced across instances.
- Odds can diverge between instances.
- Total exposure is not durable and not globally authoritative.

## Target Scalable Architecture (North Star)

- Shared/global rate limiting (Redis-backed token/sliding window).
- Shared odds distribution (pub/sub or Kafka + local cache convergence).
- Durable authoritative exposure source (projection/table or aggregate query model).
- Optional event-driven backbone via outbox + domain events.

## Evolution Plan (Phased)

### Phase 1 (minimal-change hardening)

1. Externalize rate limiting to Redis.
2. Make exposure reads authoritative from shared durable state.
3. Synchronize odds state across instances through shared channel/store.

### Phase 2 (consistency + boundaries)

1. Introduce domain events (`BetPlaced`, `EventSettled`, `OddsUpdated`) with outbox pattern.
2. Build exposure projection as read model.
3. Move toward explicit write/read model boundaries.

### Phase 3 (throughput + operations)

1. Add lock/settlement contention observability and tuning.
2. Add backpressure/resilience for feed bursts.
3. Tune retries/timeouts under load and failure scenarios.

## Change Log (append-only)

Use this section to record what changed, when, and why. Each entry should include:

- Date
- Step/phase
- Code/files changed
- Architecture impact
- Risks/trade-offs
- Validation performed

### Entry Template

```text
Date: YYYY-MM-DD
Phase/Step: <e.g., Phase 1 - Redis rate limiting>
Summary:
- <what changed>

Files:
- <path>
- <path>

Architecture impact:
- <behavioral/operational change>

Risks / trade-offs:
- <known limitations or migration concerns>

Validation:
- <tests, smoke checks, metrics observed>
```

### Entries

- 2026-04-27: Baseline captured in this file (no runtime behavior change).

