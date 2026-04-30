/**
 * k6 Load Test: Rate Limiting
 * 
 * Purpose: Test rate limiting behavior under high concurrent load
 * 
 * Scenario:
 * - Multiple users hammering the API simultaneously
 * - Should see 429 responses when rate limit is exceeded
 * - Tests both in-memory and Redis rate limiting
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// Custom metrics
const rateLimitErrors = new Rate('rate_limit_errors');
const requestDuration = new Trend('request_duration_ms');
const totalRequests = new Counter('total_requests');

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp up to 50 users
    { duration: '1m', target: 100 },   // Ramp up to 100 users
    { duration: '2m', target: 200 },   // Ramp up to 200 users (should trigger rate limits)
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],   // 95% of requests should be below 500ms
    // All k6 VUs share a single source IP in Docker, so the shared rate-limit
    // bucket fills up quickly. Accept up to 90% rate-limited; the important
    // assertion is that they return 429 (not 500), verified by the checks below.
    'rate_limit_errors': ['rate<0.9'],
  },
};

export default function () {
  const userId = `user-${Math.floor(Math.random() * 100) + 1}`;
  
  // Try to get user exposure (lightweight endpoint for rate limit testing)
  const response = http.get(
    `${BASE_URL}/api/v1/users/${userId}/exposure`,
    {
      headers: {
        'X-Request-Id': `req-${Date.now()}-${Math.random()}`,
      },
    }
  );

  totalRequests.add(1);
  requestDuration.add(response.timings.duration);

  const isRateLimited = response.status === 429;
  rateLimitErrors.add(isRateLimited ? 1 : 0);

  check(response, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'response time < 1s': (r) => r.timings.duration < 1000,
  });

  if (isRateLimited) {
    console.log(`Rate limited for user ${userId} at ${new Date().toISOString()}`);
  }

  sleep(0.1);  // 10 requests per second per virtual user
}

export function handleSummary(data) {
  return {
    '/results/rate-limit-summary.json': JSON.stringify(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = (options && options.indent) || '';

  // Safe accessor: returns 'N/A' for any missing/null metric value
  function f(val, decimals) {
    return (val !== null && val !== undefined && !isNaN(val))
      ? Number(val).toFixed(decimals !== undefined ? decimals : 2)
      : 'N/A';
  }

  function metricVal(path) {
    var parts = path.split('.');
    var cur = data.metrics;
    for (var i = 0; i < parts.length; i++) {
      if (cur === null || cur === undefined) return undefined;
      cur = cur[parts[i]];
    }
    return cur;
  }

  var totalReqs   = metricVal('total_requests.values.count') || 0;
  var rateLimitPct = f((metricVal('rate_limit_errors.values.rate') || 0) * 100);
  var nonLimitPct  = f((1 - (metricVal('rate_limit_errors.values.rate') || 0)) * 100);
  var avg  = f(metricVal('request_duration_ms.values.avg'));
  var p95  = f(metricVal('request_duration_ms.values.p(95)'));
  var max  = f(metricVal('request_duration_ms.values.max'));
  var rps  = f(metricVal('http_reqs.values.rate'));

  return '\n' +
    indent + 'Rate Limiting Load Test Summary\n' +
    indent + '================================\n' +
    indent + '\n' +
    indent + 'Total Requests : ' + totalReqs + '\n' +
    indent + 'Rate Limited   : ' + rateLimitPct + '%\n' +
    indent + 'Passed Through : ' + nonLimitPct + '%\n' +
    indent + '\n' +
    indent + 'Request Duration:\n' +
    indent + '  Average : ' + avg + ' ms\n' +
    indent + '  P95     : ' + p95 + ' ms\n' +
    indent + '  Max     : ' + max + ' ms\n' +
    indent + '\n' +
    indent + 'Requests/sec   : ' + rps + '\n';
}
