package io.anomaly.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;

/** Generates reusable large and ordinary request files once, outside load timing. */
public final class GenerateLoadFixtures {
  private GenerateLoadFixtures() {}

  /**
   * Writes both default algorithms with the established seeded MIXED distribution.
   *
   * @param args ignored project-local output directory
   * @throws Exception for fixture generation or file failures
   */
  public static void main(String[] args) throws Exception {
    Path directory = Path.of(args[0]);
    Files.createDirectories(directory);
    for (int size : new int[] {10_000, 2_000_000}) {
      var data = FixtureData.create("MIXED", size, 20261110);
      for (String algorithm : new String[] {"IQR", "Z_SCORE"}) {
        ApiProbe.writeRequest(directory.resolve(algorithm + "-" + size + ".json"), algorithm, data);
      }
    }
  }
}
