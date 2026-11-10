package io.anomaly.benchmark;

import io.anomaly.core.algorithm.AnomalyDetector;
import io.anomaly.core.algorithm.IqrDetector;
import io.anomaly.core.algorithm.ZScoreDetector;
import io.anomaly.core.model.DetectionResult;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/** Measures the real baseline detect call, including copies and full result allocation. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
@Fork(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class DetectionBenchmark {
  /** Canonical released algorithm. */
  @Param({"IQR", "Z_SCORE"})
  public String algorithm;

  /** Numbers per dataset. */
  @Param({"10000", "100000", "1000000", "2000000"})
  public int size;

  /** Deterministic distribution/order. */
  @Param({"MIXED", "DUPLICATES", "DESCENDING"})
  public String kind;

  /** Recorded shuffle seed. */
  @Param({"20261110"})
  public long seed;

  private List<Double> data;
  private AnomalyDetector detector;
  private double threshold;

  /** Creates one thread-local benchmark state. */
  public DetectionBenchmark() {}

  /** Generates and validates immutable input outside the measured operation. */
  @Setup(Level.Trial)
  public void setup() {
    HarnessEnvironment.requireUninstrumented();
    data = FixtureData.create(kind, size, seed);
    detector = algorithm.equals("IQR") ? new IqrDetector() : new ZScoreDetector();
    threshold = detector.defaultThreshold();
    long before = FixtureChecks.fingerprint(data);
    FixtureChecks.validate(data, kind, detector.detect(data, threshold));
    if (before != FixtureChecks.fingerprint(data)) {
      throw new IllegalStateException("Input mutated.");
    }
  }

  /**
   * Runs only released detection; JMH consumes the returned result to prevent elimination.
   *
   * @return full ordered result, without excluding copy/sort/partition allocation
   */
  @Benchmark
  public DetectionResult detect() {
    return detector.detect(data, threshold);
  }
}
