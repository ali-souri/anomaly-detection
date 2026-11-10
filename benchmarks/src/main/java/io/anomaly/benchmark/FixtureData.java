package io.anomaly.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic histogram fixtures with independently calculable statistics. */
public final class FixtureData {
  private FixtureData() {}

  /**
   * Generates data outside benchmark timing.
   *
   * @param kind MIXED, DUPLICATES or DESCENDING
   * @param size multiple of 100, between 100 and 2,000,000
   * @param seed reproducible shuffle seed
   * @return immutable original-order values with exactly 2% extreme outliers
   */
  public static List<Double> create(String kind, int size, long seed) {
    if (size < 100 || size > 2_000_000 || size % 100 != 0) {
      throw new IllegalArgumentException(
          "Size must be a multiple of 100 within the declared limit.");
    }
    if (kind == null || !List.of("MIXED", "DUPLICATES", "DESCENDING").contains(kind)) {
      throw new IllegalArgumentException("Unknown fixture kind.");
    }
    var values = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      int slot = i % 100;
      double value = slot == 98 ? -1000 : slot == 99 ? 1000 : kind.equals("DUPLICATES") ? 0 : slot;
      values.add(value);
    }
    if (kind.equals("DESCENDING")) {
      values.sort(Collections.reverseOrder());
    } else {
      Collections.shuffle(values, new Random(seed));
    }
    return List.copyOf(values);
  }
}
