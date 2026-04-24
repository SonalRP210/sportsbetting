# Practice Guide

Use this document as your interview simulation worksheet.

## Round 1: Code Review (20-30 min)

Read code without editing first. Capture findings under these categories:

1. Correctness
2. Reliability
3. Security/input validation
4. Performance
5. Concurrency
6. API design
7. Testability
8. Operability (logs/metrics/traces)

### Suggested output format

- Severity: Critical / High / Medium / Low
- Location: class and method
- Why it matters in production
- Concrete fix recommendation

## Round 2: Refactor Plan (15-20 min)

Propose small safe steps:

1. Introduce DTOs + validation
2. Extract risk rules service
3. Extract odds ingestion service
4. Make repository thread-safe / introduce DB
5. Add idempotency for placement and settlement
6. Add structured logging and metrics
7. Add integration tests

## Round 3: System Design Q&A (20-30 min)

### Prompt A
Design a betting platform handling 5k bet requests/sec and real-time odds updates.

Focus on:
- Event-driven architecture
- Exactly-once vs at-least-once trade-offs
- Idempotency keys
- Partitioning strategy
- Read/write model separation
- Risk checks and exposure limits
- Audit trail and compliance

### Prompt B
How would you process Kafka odds messages safely?

Focus on:
- Consumer group strategy
- Rebalancing effects
- Retry and DLQ
- Poison message handling
- Ordering guarantees per event/market
- Lag monitoring and alerting

## Bonus drills

- Add one bug intentionally, then detect it using tests.
- Load test `/api/bet` and explain race-condition symptoms.
- Redesign API contracts with proper error codes.
