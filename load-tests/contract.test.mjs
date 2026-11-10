import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validSummary } from './contract.mjs';

function fixture(algorithm = 'IQR') {
  return { algorithm, status: 'COMPLETED', threshold: algorithm === 'IQR' ? 1.5 : 3, warnings: [],
    anomalies: [-1000, 1000], normalValues: Array(98).fill(1),
    q1: 23.5, q3: 73.5, iqr: 50, lowerBound: -51.5, upperBound: 148.5,
    mean: 47.53, standardDeviation: Math.sqrt(20830.3491) };
}

test('accepts both independently known complete summaries', () => {
  for (const algorithm of ['IQR', 'Z_SCORE']) assert.equal(validSummary(fixture(algorithm), algorithm, 100), true);
});
test('rejects missing full partitions and wrong values/statistics', () => {
  for (const change of [x => x.normalValues.pop(), x => x.anomalies[0] = 0, x => x.normalValues[0] = 100,
    x => x.q1 = 0, x => x.threshold = 99, x => x.warnings.push('unexpected')]) {
    const result = fixture(); change(result); assert.equal(validSummary(result, 'IQR', 100), false);
  }
  const result = fixture('Z_SCORE'); result.mean = 0;
  assert.equal(validSummary(result, 'Z_SCORE', 100), false);
});
