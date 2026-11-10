import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const size = Number(__ENV.SIZE || 2000000);
const algorithm = __ENV.ALGORITHM || 'MIXED';
const mode = __ENV.MODE || 'arrival';
const base = __ENV.BASE_URL || 'http://app:8080';
const body = {
  IQR: open(`/fixtures/IQR-${size}.json`),
  Z_SCORE: open(`/fixtures/Z_SCORE-${size}.json`),
};
const successful = new Counter('successful_requests');
const rejected = new Counter('rejected_requests');
const unexpected = new Counter('unexpected_requests');
const successLatency = new Trend('successful_latency_ms', true);
const requestBytes = new Counter('successful_request_body_bytes');

export const options = {
  discardResponseBodies: true,
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max', 'count'],
  scenarios: {
    traffic: mode === 'arrival'
      ? { executor: 'constant-arrival-rate', rate: 10, timeUnit: '1s', duration: __ENV.DURATION || '60s', preAllocatedVUs: 20, maxVUs: 20, gracefulStop: '30s' }
      : { executor: 'constant-vus', vus: Number(__ENV.CLIENTS || 1), duration: __ENV.DURATION || '10s', gracefulStop: '30s' },
  },
  thresholds: mode === 'arrival' ? {
    unexpected_requests: ['count==0'], rejected_requests: ['count==0'],
    dropped_iterations: ['count==0'], successful_requests: ['count>=600'],
  } : { unexpected_requests: ['count==0'] },
};

export function setup() {
  // Warm up outside the declared steady scenario. Full mathematical/order checks use ApiProbe.
  for (const name of ['IQR', 'Z_SCORE']) {
    const result = http.post(`${base}/api/v1/anomaly-detection`, body[name], {
      headers: { 'Content-Type': 'application/json', 'Accept-Encoding': 'identity' }, timeout: '30s',
    });
    if (result.status !== 200) throw new Error(`Warmup ${name}: ${result.status}`);
  }
}

export default function () {
  const name = algorithm === 'MIXED' ? (exec.scenario.iterationInTest % 2 === 0 ? 'IQR' : 'Z_SCORE') : algorithm;
  const started = Date.now();
  const response = http.post(`${base}/api/v1/anomaly-detection`, body[name], {
    headers: { 'Content-Type': 'application/json', 'Accept-Encoding': 'identity' }, timeout: '30s',
    tags: { algorithm: name },
  });
  const completed = Date.now();
  if (response.status === 200) {
    successful.add(1, { algorithm: name });
    successLatency.add(completed - started, { algorithm: name });
    requestBytes.add(body[name].length);
  } else if (response.status === 503) {
    rejected.add(1);
  } else {
    unexpected.add(1, { status: String(response.status), error: String(response.error_code || 0) });
  }
  // Compact timestamps establish completion-window and backlog behavior. No huge response retained.
  console.log(JSON.stringify({ iteration: exec.scenario.iterationInTest, algorithm: name, started, completed, status: response.status, errorCode: response.error_code || 0 }));
}
