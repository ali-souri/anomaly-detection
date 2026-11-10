package io.github.alisouri.anomaly.core;

/**
 * Population Z-Score statistics; both are null for empty input.
 *
 * @param summary shared immutable result behavior
 * @param mean arithmetic mean in input units; null for NO_DATA
 * @param standardDeviation population standard deviation (variance divisor n), in input units
 */
public record ZScoreResult(DetectionSummary summary, Double mean, Double standardDeviation)
    implements DetectionResult {}
