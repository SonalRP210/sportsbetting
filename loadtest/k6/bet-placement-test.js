/**
 * k6 Load Test: Bet Placement Throughput
 * 
 * Purpose: Test concurrent bet creation with idempotency and DB contention
 * 
 * Scenario:
 * - Multiple users placing bets on the same events simultaneously
 * - Tests idempotency (duplicate requests with same key)
 * - Tests DB write contention and constraint violations
 * - Tests exposure projection updates
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// Custom metrics
const betsPlaced = new Counter('bets_placed');
const betsFailed = new Counter('bets_failed');
const idempotentReplays = new Counter('idempotent_replays');
const betPlacementDuration = new Trend('bet_placement_duration_ms');

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // Warm up
    { duration: '1m', target: 50 },    // Moderate load
    { duration: '2m', target: 100 },   // High load - test contention
    { duration: '1m', target: 150 },   // Peak load
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000'],  // 95% of requests under 2s
    'http_req_failed': ['rate<0.05'],     // Less than 5% failures
  },
};

// Setup: Create some odds first
export function setup() {
  const events = [
    { eventId: 'LIV-ARS-2026', selection: 'Liverpool', odds: 2.5 },
    { eventId: 'LIV-ARS-2026', selection: 'Arsenal', odds: 1.8 },
    { eventId: 'MAN-CHE-2026', selection: 'Manchester United', odds: 2.1 },
    { eventId: 'MAN-CHE-2026', selection: 'Chelsea', odds: 1.9 },
    { eventId: 'BAR-MAD-2026', selection: 'Barcelona', odds: 1.7 },
    { eventId: 'BAR-MAD-2026', selection: 'Real Madrid', odds: 2.3 },
  ];

  const response = http.post(
    `${BASE_URL}/api/v1/odds-feed`,
    JSON.stringify(events),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `setup-${Date.now()}`,
      },
    }
  );

  console.log(`Setup: Created odds (status ${response.status})`);
  return { events };
}

export default function (data) {
  const userId = `user-${Math.floor(Math.random() * 1000) + 1}`;
  const eventIndex = Math.floor(Math.random() * data.events.length);
  const event = data.events[eventIndex];
  
  // 10% chance of intentional duplicate request (test idempotency)
  const isIntentionalDuplicate = Math.random() < 0.1;
  const idempotencyKey = isIntentionalDuplicate
    ? `test-duplicate-${Math.floor(Math.random() * 10)}`
    : `bet-${userId}-${Date.now()}-${randomString(8)}`;

  const betRequest = {
    userId: userId,
    eventId: event.eventId,
    selection: event.selection,
    stake: Math.floor(Math.random() * 100) + 10,  // $10-$110
  };

  const response = http.post(
    `${BASE_URL}/api/v1/bets`,
    JSON.stringify(betRequest),
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
        'X-Request-Id': `req-${Date.now()}-${randomString(6)}`,
      },
    }
  );

  betPlacementDuration.add(response.timings.duration);

  const success = check(response, {
    'bet placed successfully': (r) => r.status === 201,
    'response has betId': (r) => {
      if (r.status === 201) {
        const body = JSON.parse(r.body);
        return body.betId && body.betId.startsWith('BET-');
      }
      return false;
    },
    'response time < 3s': (r) => r.timings.duration < 3000,
  });

  if (response.status === 201) {
    betsPlaced.add(1);
    
    // Check if it was an idempotent replay (you'd see this in logs)
    if (isIntentionalDuplicate) {
      idempotentReplays.add(1);
    }
  } else {
    betsFailed.add(1);
    console.log(`Bet failed: ${response.status} - ${response.body.substring(0, 100)}`);
  }

  sleep(Math.random() * 0.5);  // 0-500ms random think time
}

export function handleSummary(data) {
  const placed = (data.metrics.bets_placed && data.metrics.bets_placed.values.count) || 0;
  const failed = (data.metrics.bets_failed && data.metrics.bets_failed.values.count) || 0;
  const replays = (data.metrics.idempotent_replays && data.metrics.idempotent_replays.values.count) || 0;
  const durationValues = data.metrics.bet_placement_duration_ms && data.metrics.bet_placement_duration_ms.values;

  const summary = {
    total_bets_placed: placed,
    total_bets_failed: failed,
    idempotent_replays: replays,
    avg_duration_ms: durationValues ? durationValues.avg : 0,
    p95_duration_ms: durationValues ? durationValues['p(95)'] : 0,
    p99_duration_ms: durationValues ? durationValues['p(99)'] : 0,
    success_rate_percent: (placed + failed) > 0
      ? ((placed / (placed + failed)) * 100).toFixed(2)
      : '100.00',
  };

  console.log('\n=== Bet Placement Test Summary ===');
  console.log(`Total Bets Placed: ${summary.total_bets_placed}`);
  console.log(`Total Bets Failed: ${summary.total_bets_failed}`);
  console.log(`Success Rate: ${summary.success_rate_percent}%`);
  console.log(`Idempotent Replays: ${summary.idempotent_replays}`);
  console.log(`Avg Duration: ${summary.avg_duration_ms.toFixed(2)} ms`);
  console.log(`P95 Duration: ${summary.p95_duration_ms.toFixed(2)} ms`);
  console.log('===================================\n');

  return {
    '/results/bet-placement-summary.json': JSON.stringify(summary, null, 2),
  };
}
