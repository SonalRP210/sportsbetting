# Sports Betting Interview Sandbox (Intentionally Flawed)

This repository is intentionally designed to be messy and error-prone for technical interview preparation.

## Why this project exists

Use this codebase to practice:
- Code review comments
- Refactoring strategy
- System design Q&A tied to real code
- Risk analysis and production-readiness discussion

## Domain

Minimal sports betting backend:
- Place bet
- Ingest odds updates
- Settle event
- Fetch user summary

## Intentionally planted issues

### Code quality and design
- Very large `BettingService` with too many responsibilities
- No interfaces between controller/service/repository
- Tight coupling to concrete classes
- Primitive obsession (`Map<String, Object>` request/response)
- Inconsistent naming (`user`, `id`, `winner`, `selection`)

### API design
- Poor endpoint modeling and request contracts
- No versioning strategy
- Inconsistent response formats
- Returns HTTP 200 on hidden failures

### Performance
- N+1-like repeated full scans over in-memory storage
- Event processing does nested loops
- Artificial blocking call in request path (`Thread.sleep`)

### Concurrency and thread safety
- Shared mutable state with non-thread-safe collections
- Global exposure and odds map updated without synchronization
- Time-based bet IDs can collide under load

### Error handling and resilience
- Catches generic exceptions and swallows root cause
- Success messages returned for failed operations
- No retry strategy, no dead-letter handling concept

### Observability
- Root logging disabled
- No structured logs, metrics, tracing, or correlation IDs

## Suggested practice checklist

1. Review code as if it were a PR and list top risks first.
2. Prioritize correctness and data consistency.
3. Refactor in small, safe commits.
4. Introduce DTOs and validation.
5. Add meaningful tests (happy path + edge cases + concurrency).
6. Propose observability and resilience improvements.

## Example interview prompts

- "What are the top 5 production risks in this code?"
- "How would you break this service into smaller components?"
- "How do you make odds ingestion safe under concurrency?"
- "What would you log and measure in production?"
- "How would you redesign this API for versioned public use?"
Sports Betting Project
