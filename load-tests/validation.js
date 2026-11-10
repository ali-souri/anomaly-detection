import http from 'k6/http';
import { check } from 'k6';

const base = __ENV.BASE_URL || 'http://app:8080';
// Generation outside the single validation iteration; no 2M array parsed in the generator.
const tooMany = '{"algorithm":"IQR","data":[' + '0,'.repeat(2000000) + '0]}';
export const options = { vus: 1, iterations: 1, thresholds: { checks: ['rate==1'] } };

export default function () {
  const requests = [
    ['{', 400, 'MALFORMED_JSON'],
    ['{"algorithm":"IQR","data":["1"]}', 400, 'INVALID_REQUEST'],
    ['{"algorithm":"IQR","data":[-1.7976931348623157e308,1.7976931348623157e308]}', 422, 'NUMERICAL_RANGE'],
    [tooMany, 413, 'INPUT_TOO_LARGE'],
  ];
  for (const [body, status, code] of requests) {
    const result = http.post(`${base}/api/v1/anomaly-detection`, body, { headers: { 'Content-Type': 'application/json' }, timeout: '30s' });
    check(result, { [`${code}: status and stable code`]: r => r.status === status && r.json('code') === code });
  }
  const recovery = http.post(`${base}/api/v1/anomaly-detection`, '{"algorithm":"Z_SCORE","data":[7]}', { headers: { 'Content-Type': 'application/json' } });
  check(recovery, { 'recovered complete small output': r => r.status === 200 && r.json('normalValues')[0] === 7 });
}
