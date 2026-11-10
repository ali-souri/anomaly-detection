package io.anomaly.benchmark;

import io.anomaly.core.algorithm.AnomalyDetector;
import io.anomaly.core.algorithm.IqrDetector;
import io.anomaly.core.algorithm.ZScoreDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Executes a correctness preflight of the entire declared matrix, without accepting timings. */
public final class CheckFixtures {
  private CheckFixtures() {}

  /**
   * Validates every matrix case and writes compact lifecycle/count evidence.
   *
   * @param args one CSV output path
   * @throws Exception if validation or output fails
   */
  public static void main(String[] args) throws Exception {
    HarnessEnvironment.requireUninstrumented();
    var lines = new ArrayList<String>();
    lines.add("algorithm,kind,size,seed,anomalies,normalValues,fingerprint,validated");
    for (String kind : new String[] {"MIXED", "DUPLICATES", "DESCENDING"}) {
      for (int size : new int[] {10000, 100000, 1000000, 2000000}) {
        var data = FixtureData.create(kind, size, 20261110);
        for (AnomalyDetector detector :
            new AnomalyDetector[] {new IqrDetector(), new ZScoreDetector()}) {
          long before = FixtureChecks.fingerprint(data);
          var result = detector.detect(data, detector.defaultThreshold());
          FixtureChecks.validate(data, kind, result);
          if (before != FixtureChecks.fingerprint(data)) {
            throw new IllegalStateException("Input mutated.");
          }
          lines.add(
              detector.algorithm()
                  + ","
                  + kind
                  + ","
                  + size
                  + ",20261110,"
                  + result.summary().anomalies().size()
                  + ","
                  + result.summary().normalValues().size()
                  + ","
                  + before
                  + ",true");
        }
      }
    }
    Files.write(Path.of(args[0]), lines);
    System.out.println(
        "Validated all 24 declared cases, including exact order, counts, input lifecycle and analytical statistics.");
  }
}
