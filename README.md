# Sports Betting — Interview Practice Backend

Spring Boot service for practicing **code review**, **refactors**, **API design**, and **system-design Q&A** around a small betting domain (place bet, odds feed, settlement, queries).

The repo started as an intentionally rough scaffold; it has since been **partially hardened** (JPA, validation, rate limits, idempotency, settlement ledger, retries, observability). You can still use it to discuss trade-offs, gaps, and “what we would do next.”

## Tech stack

- Java 21, Spring Boot 3.3
- Spring Web, Validation, Data JPA, Actuator (metrics/Prometheus)
- PostgreSQL (see `compose.yml`; tests use Testcontainers Postgres)
- OpenAPI JSON (`springdoc-openapi` **API** starter — `/v3/api-docs` only; no Swagger UI)
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
| `openapi.yaml` | **Static OpenAPI 3 contract** for other teams (kept aligned with controllers and error handling). Runtime spec also served at `/v3/api-docs`. |
| `PRACTICE_GUIDE.md` | Mock interview rounds and prompts |
| `CHANGE_NOTES_BUSINESS.md` | Why major changes were made (business / risk framing) |

## Run and test

Start Postgres (defaults match `application.yaml`), then run the app:

```bash
docker compose up -d
mvn spring-boot:run
```

DB-backed tests use **Testcontainers** with **one Postgres per JVM** (avoids Spring context reuse vs. per-class container teardown). Those tests are **skipped** when Docker is unavailable (`@EnabledIf`). `docker compose` here is only for **local** service dependencies when you run the app.

```bash
mvn test
```

OpenAPI when the app is running: **`/v3/api-docs`** (JSON for tooling and other services). The static contract file is `openapi.yaml`.

## Testing strategy (pyramid, smoke, Testcontainers)

**Smoke test** — A *very small* set of checks that answer: “Is the deployed or locally built service basically alive?” Typical smoke calls hit health, one read path, and sometimes one write path. They are **not** a substitute for deeper tests; they catch wiring, routing, and catastrophic config failures fast.

**Test pyramid (how to think about this repo, not a rigid formula)**  
- **Wide base — unit tests** under `src/test/java/.../unit/` (`unit.service`, `unit.support`): Mockito + no Testcontainers; most business rules and edge cases.  
- **Middle — integration tests** under `src/test/java/.../integration/`: Spring + **Testcontainers Postgres** (`AbstractPostgresSpringBootTest` / `AbstractPostgresDataJpaTest`), real JPA and service wiring.  
- **Narrow top — smoke tests** in `smoke/`: a couple of HTTP calls on the full stack (`ApplicationSmokeTest`).  
- **Controller slices** stay in `controller/` (`@WebMvcTest`); **filters/config** in `config/`.

Percentages vary by team and module; the goal is **many fast unit tests**, **some realistic integration tests**, and **very few smoke checks** so you still trust the system when containers are green.

Run only smoke: `mvn -q -Dtest=com.sonal.sportsbetting.smoke.ApplicationSmokeTest test` (Docker required like other DB-backed tests).

## Example interview prompts

- “Walk through idempotency for `POST /bets` vs settlement — what can still go wrong under load?”
- “Why pessimistic locks here instead of only optimistic versioning?”
- “How would you add Kafka for odds while keeping at-least-once semantics safe?”
- “What is still not production-grade in exposure accounting?”
- “How would you extend error responses for clients without leaking internals?”
