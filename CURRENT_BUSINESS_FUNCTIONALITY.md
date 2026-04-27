# Current Implemented Business Functionality

This document summarizes the business behavior currently implemented in the Sports Betting service.

## Core Business Flow

The service supports the end-to-end lifecycle of a sportsbook bet:

1. Odds are ingested for an event and selection.
2. A user places a bet using the latest available odds.
3. Bets can be queried and open bets can be cancelled.
4. Events are settled with a winning selection.
5. User and total exposure are available for risk visibility.

## Implemented Functional Areas

### 1) Odds Feed Ingestion

- Endpoint: `POST /api/v1/odds-feed`
- Accepts a batch of odds updates (`eventId`, `selection`, `odds`).
- Stores the latest odds per market key (`eventId + selection`) for future bet placement.
- Returns an accepted response to indicate feed processing.

### 2) Bet Placement

- Endpoint: `POST /api/v1/bets`
- Validates input and business rules (for example, stake and odds must be greater than zero).
- Rejects placement if no active odds exist for the requested event/selection.
- Creates a bet with status `OPEN`.
- Returns the created bet identifier, accepted odds, and current status.

### 3) Bet Idempotency (Duplicate Protection)

- Supports optional `Idempotency-Key` on bet placement.
- If the same user retries the same key, the service returns the previously created logical bet instead of creating a duplicate.
- Handles race conditions around duplicate submissions using data integrity checks and recovery logic.

### 4) Bet Query and Cancellation

- `GET /api/v1/bets/{betId}` returns detailed bet information.
- `POST /api/v1/bets/{betId}/cancel` cancels only bets currently in `OPEN` state.
- Cancel attempts for non-open bets are rejected as business rule violations.

### 5) Event Settlement

- Endpoint: `POST /api/v1/events/settlements`
- Settles all open bets for an event based on the provided winning selection.
- Winning bets are marked `WON`; others are marked `LOST`.
- Returns settlement summary:
  - winners count
  - losers count
  - total payout
  - current total exposure

### 6) Settlement Idempotency and Conflict Handling

- Maintains an event settlement ledger keyed by `eventId`.
- If the same event is settled again with the same winner, it is treated as idempotent replay.
- If a different winner is submitted for an already settled event, the service returns a conflict (`409`).

### 7) Exposure Tracking

- Tracks total platform exposure (risk) as bets are placed and settled.
- Provides user-level exposure by summing open bet risk for that user.
- Endpoint: `GET /api/v1/users/{userId}/exposure`

### 8) User and Event Bet Queries

- `GET /api/v1/users/{userId}/bets` lists user bets.
- `GET /api/v1/events/{eventId}/bets` lists bets for an event.
- Supports page/size query parameters for basic pagination behavior.

## Cross-Cutting Business Safety Behaviors

- Validation with structured error responses.
- Business-rule errors mapped to client-friendly response codes.
- Concurrency protections for settlement and update scenarios.
- Request rate limiting.
- Request correlation ID propagation for traceability.
- Metrics for key events (bet placement, settlement, idempotent replays, exposure gauge).

## In Short

The implemented domain functionality today is:

**odds ingestion -> bet placement -> bet query/cancellation -> event settlement -> exposure/reporting**, with idempotency and concurrency safety mechanisms already in place.
