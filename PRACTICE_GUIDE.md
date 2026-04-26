# Practice Guide

Use this document as your interview simulation worksheet.

## Round 1: Code review (20–30 min)

Read code without editing first. Capture findings under these categories:

1. Correctness (money, settlement, idempotency boundaries)
2. Reliability (retries, transactions, duplicate requests)
3. Security / input validation (auth missing, abuse, rate limits)
4. Performance (DB access patterns, hot paths)
5. Concurrency (locks, versions, exposure counter vs DB truth)
6. API design (headers, status codes, error model)
7. Testability (integration vs unit coverage)
8. Operability (logs, metrics, traces, correlation)

### Suggested output format

- Severity: Critical / High / Medium / Low
- Location: class and method
- Why it matters in production
- Concrete fix recommendation

### Hooks in the current codebase

- Tests live by layer: `unit/service` (Mockito), `integration/` (Testcontainers Postgres), `smoke/` (HTTP sanity), `controller/` (`@WebMvcTest`).
- `DefaultBetPlacementService` — idempotency, `RetryTemplate` on `save`, exposure after persist
- `DefaultSettlementService` — ledger, pessimistic lock query, conflict semantics
- `GlobalExceptionHandler` — HTTP mapping for validation, rate limit, settlement conflict, transient, optimistic lock, data integrity
- `DefaultExposureService` — in-memory `AtomicReference` (discuss durability vs DB)

## Round 2: Refactor / design discussion (15–20 min)

Pick a small slice and propose **ordered** steps:

1. Add authentication and authorization on mutating endpoints.
2. Move exposure to a **derived** or persisted model with reconciliation.
3. Replace or complement `Idempotency-Key` with signed request tokens / payment gateway patterns.
4. Add outbox or event log for settlement for audit and async projections.
5. Introduce bounded retry + DLQ for a future Kafka consumer (not wired in this HTTP-only odds path).
6. Harden OpenAPI examples for all error codes (`409`, `503`, etc.).

**Already done in-repo (do not re-propose as “missing” without nuance):** DTOs + validation on main APIs, JPA, rate limiting, correlation id, actuator metrics, placement + settlement idempotency, persistence retry on transient DB errors.

## Round 3: Hands-on API drills (10–15 min)

Use a running app (`mvn spring-boot:run`) or tests as oracle.

1. **Idempotent bet**  
   - `POST /api/v1/bets` twice with the same JSON and the same **`Idempotency-Key`** header.  
   - Expect: same `betId`, single row in `bets` for that user/key.

2. **Settlement replay**  
   - Settle once; settle again with the **same** `winningSelection`.  
   - Expect: same winner/loser counts and payout; `globalExposure` reflects **current** in-memory gauge.

3. **Settlement conflict**  
   - After a successful settle, call settle again with a **different** `winningSelection`.  
   - Expect: **409**, code `SETTLEMENT_CONFLICT`.

4. **Rate limit**  
   - Burst requests until **429** (`RATE_LIMITED`).

## Round 4: System design Q&A (20–30 min)

### Prompt A

Design a betting platform handling high bet throughput and real-time odds.

Focus on: idempotency keys, partitioning, read/write separation, risk and exposure, audit, **exactly-once vs at-least-once** for feeds.

### Prompt B

How would you process Kafka odds messages safely?

Focus on: consumer groups, rebalancing, retries, DLQ, poison messages, ordering per market, lag alerts.

## Bonus drills

- Trace one request: `X-Request-Id` → MDC → log line → `ApiErrorResponse.traceId` on a forced `422`.
- Explain why **`RetryTemplate`** wraps `save` but not `getOdds`.
- Add a failing integration test for “two threads settle same event” and justify expected behavior with the current lock + ledger design.
- Propose how you would deprecate **`/api/v1`** without breaking mobile clients.
