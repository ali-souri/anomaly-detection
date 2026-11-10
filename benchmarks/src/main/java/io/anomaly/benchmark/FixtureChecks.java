package io.anomaly.benchmark;

import io.anomaly.core.model.DetectionResult;
import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.IqrResult;
import io.anomaly.core.model.ZScoreResult;
import java.util.List;
import java.util.Map;

/** Analytical fixture expectations, separate from the released calculation implementation. */
public final class FixtureChecks {
  private FixtureChecks() {}

  /**
   * Checks exact ordered partitions and analytical statistics before accepting timings.
   *
   * @param data original immutable fixture
   * @param kind declared fixture distribution
   * @param result real released detector output
   * @throws IllegalStateException if any contract or expected statistic disagrees
   */
  public static void validate(List<Double> data, String kind, DetectionResult result) {
    validateHistogram(data, kind);
    var summary = result.summary();
    require(summary.status() == DetectionStatus.COMPLETED, "Expected COMPLETED.");
    require(summary.warnings().isEmpty(), "Unexpected fixture warning.");
    require(summary.anomalies().size() == data.size() / 50, "Wrong outlier count.");
    require(
        summary.normalValues().size() + summary.anomalies().size() == data.size(),
        "Partition count.");
    validatePartition(data, summary.anomalies(), true);
    validatePartition(data, summary.normalValues(), false);
    var expected = statistics(summary.algorithm(), kind);
    if (result instanceof IqrResult iqr) {
      require(summary.threshold() == 1.5, "IQR default threshold.");
      require(iqr.q1().equals(expected.get("q1")), "q1 mismatch.");
      require(iqr.q3().equals(expected.get("q3")), "q3 mismatch.");
      require(iqr.iqr().equals(expected.get("iqr")), "iqr mismatch.");
      require(iqr.lowerBound().equals(expected.get("lowerBound")), "lower bound mismatch.");
      require(iqr.upperBound().equals(expected.get("upperBound")), "upper bound mismatch.");
    } else if (result instanceof ZScoreResult zScore) {
      require(summary.threshold() == 3, "Z_SCORE default threshold.");
      close(zScore.mean(), expected.get("mean"));
      close(zScore.standardDeviation(), expected.get("standardDeviation"));
    } else {
      throw new IllegalStateException("Unexpected result type.");
    }
  }

  /**
   * Supplies analytical histogram statistics, independently of either released algorithm.
   *
   * @param algorithm IQR or Z_SCORE
   * @param kind declared distribution
   * @return expected flat statistic names and values
   */
  public static Map<String, Double> statistics(String algorithm, String kind) {
    boolean duplicates = kind.equals("DUPLICATES");
    if (algorithm.equals("IQR")) {
      return duplicates
          ? Map.of("q1", 0., "q3", 0., "iqr", 0., "lowerBound", 0., "upperBound", 0.)
          : Map.of("q1", 23.5, "q3", 73.5, "iqr", 50., "lowerBound", -51.5, "upperBound", 148.5);
    }
    if (!algorithm.equals("Z_SCORE")) {
      throw new IllegalArgumentException("Unknown algorithm.");
    }
    // Sum(0..97)=4753; Sum(squares)=308945; two extremes add 2,000,000 to squares.
    return duplicates
        ? Map.of("mean", 0., "standardDeviation", Math.sqrt(20000))
        : Map.of("mean", 47.53, "standardDeviation", Math.sqrt(23089.45 - 47.53 * 47.53));
  }

  /**
   * Tests a fixture value against the analytical outlier labels at both defaults.
   *
   * @param value fixture value
   * @return true only for the two extreme values
   */
  public static boolean outlier(double value) {
    return value == -1000 || value == 1000;
  }

  /**
   * Requires population statistics to agree within a stated floating-point tolerance.
   *
   * @param actual computed statistic
   * @param expected independently derived statistic
   * @throws IllegalStateException outside 1e-8 absolute plus 1e-10 relative tolerance
   */
  public static void close(double actual, double expected) {
    require(
        Double.isFinite(actual) && Math.abs(actual - expected) <= 1e-8 + 1e-10 * Math.abs(expected),
        "Statistic mismatch: " + actual + " versus " + expected);
  }

  /**
   * Fingerprints value bits in original order for before/after lifecycle checks.
   *
   * @param data fixture values
   * @return deterministic non-cryptographic order-sensitive hash
   */
  public static long fingerprint(List<Double> data) {
    long hash = 1;
    for (double value : data) {
      hash = 31 * hash + Double.doubleToLongBits(value);
    }
    return hash;
  }

  private static void validateHistogram(List<Double> data, String kind) {
    require(!data.isEmpty() && data.size() % 100 == 0, "Fixture size.");
    int[] counts = new int[100];
    for (double value : data) {
      int slot = value == -1000 ? 98 : value == 1000 ? 99 : (int) value;
      require(
          value == -1000 || value == 1000 || (slot >= 0 && slot < 98 && value == slot),
          "Fixture value.");
      counts[slot]++;
    }
    int repeat = data.size() / 100;
    for (int i = 0; i < 100; i++) {
      int expected = kind.equals("DUPLICATES") && i < 98 ? (i == 0 ? 98 * repeat : 0) : repeat;
      require(counts[i] == expected, "Histogram mismatch.");
    }
  }

  private static void validatePartition(List<Double> data, List<Double> actual, boolean anomalies) {
    int cursor = 0;
    for (double value : data) {
      if (outlier(value) == anomalies) {
        require(
            cursor < actual.size() && actual.get(cursor).doubleValue() == value,
            "Partition order/value mismatch.");
        cursor++;
      }
    }
    require(cursor == actual.size(), "Extra partition values.");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
