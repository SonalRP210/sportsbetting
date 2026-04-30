/**
 * k6 Load Test: Settlement Contention
 * 
 * Purpose: Test lock-based settlement on hot events with many open bets
 * 
 * Scenario:
 * 1. Create many bets on a single "hot" event
 * 2. Trigger concurrent settlement attempts
 * 3. Measure lock contention and settlement duration
 * 4. Test idempotency (same winner) and conflicts (different winner)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// Custom metrics
const settlementsSucceeded = new Counter('settlements_succeeded');
const settlementsFailed = new Counter('settlements_failed');
const settlementConflicts = new Counter('settlement_conflicts');
const settlementDuration = new Trend('settlement_duration_ms');

export const options = {
  scenarios: {
    // Scenario 1: Create bets on hot event
    create_bets: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      exec: 'createBets',
    },
    // Scenario 2: Concurrent settlement attempts (runs after bet creation)
    settle_event: {
      executor: 'constant-vus',
      vus: 10,
      duration: '1m',
      startTime: '35s',  // Start after bets are created
      exec: 'settleEvent',
    },
  },
  thresholds: {
    'settlement_duration_ms': ['p(95)<5000'],  // Settlement should complete in <5s
    'settlement_conflicts': ['count<10'],       // Minimal conflicts expected
  },
};

const HOT_EVENT_ID = 'CHAMPIONS-FINAL-2026';

// Setup: Create odds for the hot event
export function setup() {
  const odds = [
    { eventId: HOT_EVENT_ID, selection: 'Bayern Munich', odds: 2.1 },
    { eventId: HOT_EVENT_ID, selection: 'Paris SG', odds: 1.9 },
  ];

  http.post(
    `${BASE_URL}/api/v1/odds-feed`,
    JSON.stringify(odds),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `setup-${Date.now()}`,
      },
    }
  );

  console.log(`Setup: Created odds for hot event ${HOT_EVENT_ID}`);
  return { hotEventId: HOT_EVENT_ID };
}

// Phase 1: Create many bets on the hot event
export function createBets(data) {
  const userId = `user-${Math.floor(Math.random() * 500) + 1}`;
  const selection = Math.random() < 0.5 ? 'Bayern Munich' : 'Paris SG';
  
  const betRequest = {
    userId: userId,
    eventId: data.hotEventId,
    selection: selection,
    stake: Math.floor(Math.random() * 200) + 50,  // $50-$250
  };

  const response = http.post(
    `${BASE_URL}/api/v1/bets`,
    JSON.stringify(betRequest),
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': `bet-${userId}-${Date.now()}-${randomString(8)}`,
        'X-Request-Id': `req-${Date.now()}-${randomString(6)}`,
      },
    }
  );

  check(response, {
    'bet created': (r) => r.status === 201,
  });

  sleep(0.2);
}

// Phase 2: Concurrent settlement attempts
export function settleEvent(data) {
  // 80% settle with correct winner, 20% with different winner (to test conflicts)
  const isConflict = Math.random() < 0.2;
  const winningSelection = isConflict ? 'Paris SG' : 'Bayern Munich';

  const settlementRequest = {
    winningSelection: winningSelection,
  };

  const startTime = Date.now();
  const response = http.post(
    `${BASE_URL}/api/v1/events/settlements`,
    JSON.stringify({
      eventId: data.hotEventId,
      winningSelection: winningSelection,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `settle-${Date.now()}-${randomString(6)}`,
      },
    }
  );

  const duration = Date.now() - startTime;
  settlementDuration.add(duration);

  const isSuccess = check(response, {
    'settlement succeeded or idempotent replay': (r) => r.status === 200,
    'settlement conflict detected': (r) => r.status === 409,
    'response time < 10s': (r) => duration < 10000,
  });

  if (response.status === 200) {
    settlementsSucceeded.add(1);
    const body = JSON.parse(response.body);
    console.log(`Settlement succeeded: ${body.winners} winners, ${body.losers} losers, duration: ${duration}ms`);
  } else if (response.status === 409) {
    settlementConflicts.add(1);
    console.log(`Settlement conflict detected (expected for conflict test)`);
  } else {
    settlementsFailed.add(1);
    console.log(`Settlement failed: ${response.status} - ${response.body.substring(0, 100)}`);
  }

  sleep(1);  // Wait between settlement attempts
}

export function handleSummary(data) {
  const metricValues = (name) => (data && data.metrics && data.metrics[name] && data.metrics[name].values) || {};
  const succeeded = metricValues('settlements_succeeded');
  const failed = metricValues('settlements_failed');
  const conflicts = metricValues('settlement_conflicts');
  const duration = metricValues('settlement_duration_ms');

  const summary = {
    settlements_succeeded: succeeded.count || 0,
    settlements_failed: failed.count || 0,
    settlement_conflicts: conflicts.count || 0,
    avg_settlement_duration_ms: duration.avg || 0,
    p95_settlement_duration_ms: duration['p(95)'] || 0,
    p99_settlement_duration_ms: duration['p(99)'] || 0,
    max_settlement_duration_ms: duration.max || 0,
  };

  const safeFixed = (num, decimals = 2) => {
    const n = Number(num) || 0;
    return n.toFixed(decimals);
  };

  console.log('\n=== Settlement Contention Test Summary ===');
  console.log(`Settlements Succeeded: ${summary.settlements_succeeded}`);
  console.log(`Settlements Failed: ${summary.settlements_failed}`);
  console.log(`Settlement Conflicts: ${summary.settlement_conflicts}`);
  console.log(`Avg Duration: ${safeFixed(summary.avg_settlement_duration_ms)} ms`);
  console.log(`P95 Duration: ${safeFixed(summary.p95_settlement_duration_ms)} ms`);
  console.log(`P99 Duration: ${safeFixed(summary.p99_settlement_duration_ms)} ms`);
  console.log(`Max Duration: ${safeFixed(summary.max_settlement_duration_ms)} ms`);
  console.log('=========================================\n');

  return {
    '/results/settlement-summary.json': JSON.stringify(summary, null, 2),
  };
}
