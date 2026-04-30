/**
 * k6 Load Test: Odds Feed Burst
 * 
 * Purpose: Test high-frequency odds updates (direct vs Kafka-buffered paths)
 * 
 * Scenario:
 * - Send bursts of odds updates
 * - Test database write throughput to latest_odds table
 * - Test Redis pub/sub message rate
 * - Compare direct path vs Kafka-buffered path performance
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';
const KAFKA_ENABLED = __ENV.KAFKA_ENABLED === 'true';

// Custom metrics
const oddsUpdatesSucceeded = new Counter('odds_updates_succeeded');
const oddsUpdatesFailed = new Counter('odds_updates_failed');
const oddsBatchDuration = new Trend('odds_batch_duration_ms');
const oddsPerSecond = new Trend('odds_per_second');

export const options = {
  scenarios: {
    // Burst scenario: Send large batches rapidly
    burst: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 10,
      maxVUs: 50,
      stages: [
        { target: 20, duration: '30s' },   // Ramp to 20 requests/sec
        { target: 50, duration: '1m' },    // Ramp to 50 requests/sec
        { target: 100, duration: '1m' },   // Peak: 100 requests/sec
        { target: 20, duration: '30s' },   // Ramp down
      ],
    },
  },
  thresholds: {
    'odds_batch_duration_ms': ['p(95)<3000'],  // 95% of batches should complete in <3s
    'http_req_failed': ['rate<0.05'],          // Less than 5% failures
  },
};

// Generate realistic odds data
function generateOddsUpdates(batchSize = 10) {
  const events = [
    'EPL-LIV-ARS-2026',
    'EPL-MAN-CHE-2026',
    'EPL-TOT-NEW-2026',
    'LALIGA-BAR-MAD-2026',
    'LALIGA-ATL-SEV-2026',
    'BUNDESLIGA-BAY-DOR-2026',
    'SERIEA-JUV-MIL-2026',
    'SERIEA-ROM-NAP-2026',
  ];

  const selections = ['Home Win', 'Draw', 'Away Win', 'Over 2.5', 'Under 2.5'];

  const updates = [];
  for (let i = 0; i < batchSize; i++) {
    const eventId = events[Math.floor(Math.random() * events.length)];
    const selection = selections[Math.floor(Math.random() * selections.length)];
    const odds = (Math.random() * 3 + 1).toFixed(2);  // Odds between 1.00 and 4.00

    updates.push({
      eventId: eventId,
      selection: selection,
      odds: parseFloat(odds),
    });
  }

  return updates;
}

export default function () {
  const batchSize = Math.floor(Math.random() * 20) + 5;  // 5-25 odds updates per batch
  const oddsUpdates = generateOddsUpdates(batchSize);

  const startTime = Date.now();
  const response = http.post(
    `${BASE_URL}/api/v1/odds-feed`,
    JSON.stringify(oddsUpdates),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `odds-${Date.now()}-${Math.random().toString(36).substring(7)}`,
      },
      timeout: '10s',
    }
  );

  const duration = Date.now() - startTime;
  oddsBatchDuration.add(duration);
  oddsPerSecond.add(batchSize / (duration / 1000));

  const isSuccess = check(response, {
    'odds update succeeded': (r) => r.status === 200 || r.status === 202 || r.status === 204,
    'response time < 5s': (r) => duration < 5000,
  });

  if (isSuccess) {
    oddsUpdatesSucceeded.add(batchSize);
    if (__VU === 1 && __ITER % 10 === 0) {  // Log every 10th iteration for VU 1
      console.log(`Batch ${__ITER}: ${batchSize} odds updates in ${duration}ms (${(batchSize / (duration / 1000)).toFixed(2)} updates/sec)`);
    }
  } else {
    oddsUpdatesFailed.add(batchSize);
    const bodyPreview = (response.body || '').substring(0, 200);
    console.log(`Odds update failed: ${response.status} - ${bodyPreview}`);
  }

  sleep(Math.random() * 0.5);  // Random delay between batches
}

export function handleSummary(data) {
  const metricValues = (name) => (data && data.metrics && data.metrics[name] && data.metrics[name].values) || {};
  const succeeded = metricValues('odds_updates_succeeded');
  const failed = metricValues('odds_updates_failed');
  const batch = metricValues('odds_batch_duration_ms');
  const perSecond = metricValues('odds_per_second');
  const requests = metricValues('http_reqs');

  const summary = {
    kafka_enabled: KAFKA_ENABLED,
    total_odds_updates: succeeded.count || 0,
    failed_updates: failed.count || 0,
    avg_batch_duration_ms: batch.avg || 0,
    p95_batch_duration_ms: batch['p(95)'] || 0,
    p99_batch_duration_ms: batch['p(99)'] || 0,
    avg_updates_per_second: perSecond.avg || 0,
    max_updates_per_second: perSecond.max || 0,
    http_req_rate: requests.rate || 0,
  };

  console.log('\n=== Odds Burst Test Summary ===');
  console.log(`Kafka Buffering: ${summary.kafka_enabled ? 'ENABLED' : 'DISABLED (Direct Path)'}`);
  console.log(`Total Odds Updates: ${summary.total_odds_updates}`);
  console.log(`Failed Updates: ${summary.failed_updates}`);
  console.log(`Avg Batch Duration: ${summary.avg_batch_duration_ms.toFixed(2)} ms`);
  console.log(`P95 Batch Duration: ${summary.p95_batch_duration_ms.toFixed(2)} ms`);
  console.log(`Avg Updates/sec: ${summary.avg_updates_per_second.toFixed(2)}`);
  console.log(`Max Updates/sec: ${summary.max_updates_per_second.toFixed(2)}`);
  console.log(`HTTP Request Rate: ${summary.http_req_rate.toFixed(2)} req/s`);
  console.log('===============================\n');

  return {
    '/results/odds-burst-summary.json': JSON.stringify(summary, null, 2),
  };
}
