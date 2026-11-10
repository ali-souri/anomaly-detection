export function validSummary(result, algorithm, size) {
  if (result.algorithm !== algorithm || result.status !== 'COMPLETED'
      || result.threshold !== (algorithm === 'IQR' ? 1.5 : 3)
      || !Array.isArray(result.warnings) || result.warnings.length !== 0
      || !Array.isArray(result.anomalies) || result.anomalies.length !== size / 50
      || !Array.isArray(result.normalValues) || result.normalValues.length !== size * 49 / 50) return false;
  if (!result.anomalies.every(x => x === -1000 || x === 1000)
      || !result.normalValues.every(x => Number.isFinite(x) && x >= 0 && x <= 97)) return false;
  return algorithm === 'IQR'
    ? result.q1 === 23.5 && result.q3 === 73.5 && result.iqr === 50 && result.lowerBound === -51.5 && result.upperBound === 148.5
    : Math.abs(result.mean - 47.53) < 1e-7 && Math.abs(result.standardDeviation - Math.sqrt(20830.3491)) < 1e-7;
}
