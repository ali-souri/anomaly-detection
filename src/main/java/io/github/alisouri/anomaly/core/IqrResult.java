package io.github.alisouri.anomaly.core;

/**
 * IQR statistics in the same units as the data. All statistics are null for empty input.
 *
 * @param summary common immutable result
 * @param q1 median of the lower half, excluding the overall middle for odd sample sizes
 * @param q3 median of the upper half under the same convention
 * @param iqr Q3 minus Q1
 * @param lowerBound Q1 minus threshold times IQR (equality is normal)
 * @param upperBound Q3 plus threshold times IQR (equality is normal)
 */
public record IqrResult(
    DetectionSummary summary,
    Double q1,
    Double q3,
    Double iqr,
    Double lowerBound,
    Double upperBound)
    implements DetectionResult {}
