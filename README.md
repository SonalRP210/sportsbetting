# Sports Betting — Interview Practice Backend

Spring Boot service for practicing **code review**, **refactors**, **API design**, and **system-design Q&A** around a small betting domain (place bet, odds feed, settlement, queries).

The repo started as an intentionally rough scaffold; it has since been **partially hardened** (JPA, validation, rate limits, idempotency, settlement ledger, retries, observability). You can still use it to discuss trade-offs, gaps, and “what we would do next.”

## Tech stack

- Java 21, Spring Boot 3.3
- Spring Web, Validation, Data JPA, Actuator (metrics/Prometheus)
- PostgreSQL (see `compose.yml`; tests use Testcontainers Postgres)
- OpenAPI / Swagger UI (`springdoc-openapi`)
- Spring Retry (`RetryTemplate`) for transient persistence failures

## API base path

- Versioned base: **`/api/v1`** (see `application.yaml` → `api.base-path`)

Main flows:

| Action | Method | Path / notes |
|--------|--------|----------------|
| Place bet | `POST` | `/api/v1/bets` — optional header **`Idempotency-Key`** |
| Odds feed | `POST` | `/api/v1/odds-feed` |
| Settle event | `POST` | `/api/v1/events/settlements` — body `eventId`, `winningSelection` |
| User / event queries | `GET` | See controllers under `com.sonal.sportsbetting.controller` |

## Reliability and concurrency (current behavior)

- **Bet idempotency**: `Idempotency-Key` + `userId` stored on `Bet` (unique constraint). Replays return the same logical bet without double exposure.
- **Settlement idempotency**: `EventSettlement` ledger per `eventId`. Same winner → replay from ledger; **different** winner → HTTP **409** (`SETTLEMENT_CONFLICT`).
- **Concurrent settlement**: `findByEventIdAndStatusForUpdate` uses **pessimistic write** locks on open bets for the event.
- **Optimistic locking**: `Bet` has `@Version` (`opt_lock`) for concurrent update detection (mapped to **409** on conflict).
- **Retries**: `persistenceRetryTemplate` retries **`save`** on `TransientDataAccessException` (not used for “missing odds” business errors).

## Observability

- **Request correlation**: `RequestCorrelationFilter` — `X-Request-Id` (or generated), echoed in response; value in MDC as `traceId`.
- **Logging**: `application.yaml` — console pattern includes `traceId`; package `com.sonal.sportsbetting` at INFO.
- **Errors**: `ApiErrorResponse` includes optional **`traceId`** when MDC is set.
- **Metrics**: Micrometer counters (e.g. bets placed, settlements, idempotent replays); exposure gauge — see services.

## Rate limiting

- Servlet filter + `app.rate-limit` in `application.yaml` (see `RateLimitingFilter`, `RateLimitProperties`).

## Tunable configuration (`application.yaml`)

Operational and domain defaults live under `app.*` (no magic numbers in services for these):

| Prefix | Examples |
|--------|----------|
| `app.betting` | `bet-id-prefix`, `money-scale`, `money-rounding-mode`, `idempotency-key-max-length` |
| `app.pagination` | `default-page`, `default-page-size` (used by list endpoints) |
| `app.correlation` | `request-id-header`, `response-id-header`, `mdc-key` |
| `app.http` | `idempotency-key-header` |
| `app.odds` | `composite-key-separator` (in-memory odds map key) |
| `app.retry.persistence` | `max-attempts`, `backoff-millis` |

`spring.application.name` drives the **service** field on `/health`. Rate limits remain under `app.rate-limit`.

## Documentation

| File | Purpose |
|------|---------|
| `PRACTICE_GUIDE.md` | Mock interview rounds and prompts |
| `CHANGE_NOTES_BUSINESS.md` | Why major changes were made (business / risk framing) |

## Run and test

Start Postgres (defaults match `application.yaml`), then run the app:

```bash
docker compose up -d
mvn spring-boot:run
```

Tests spin up their own Postgres with **Testcontainers** (Docker required for DB-backed tests; those tests are skipped if Docker is unavailable). `docker compose` here is only for **local** service dependencies, not for the test runner itself.

```bash
mvn test
```

Swagger UI (when app is running): **`/swagger-ui.html`** (see `application.yaml` for `springdoc` paths).

## Example interview prompts

- “Walk through idempotency for `POST /bets` vs settlement — what can still go wrong under load?”
- “Why pessimistic locks here instead of only optimistic versioning?”
- “How would you add Kafka for odds while keeping at-least-once semantics safe?”
- “What is still not production-grade in exposure accounting?”
- “How would you extend error responses for clients without leaking internals?”
