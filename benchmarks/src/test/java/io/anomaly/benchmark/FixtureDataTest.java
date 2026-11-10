package io.anomaly.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.anomaly.core.algorithm.IqrDetector;
import io.anomaly.core.algorithm.ZScoreDetector;
import io.anomaly.core.model.DetectionSummary;
import io.anomaly.core.model.IqrResult;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class FixtureDataTest {
  @Test
  void mixedHasExactHistogramAndARepeatableSeed() {
    var data = FixtureData.create("MIXED", 200, 123);
    assertEquals(200, data.size());
    for (double value = 0; value < 98; value++) {
      assertEquals(2, Collections.frequency(data, value));
    }
    assertEquals(2, Collections.frequency(data, -1000.));
    assertEquals(2, Collections.frequency(data, 1000.));
    assertEquals(data, FixtureData.create("MIXED", 200, 123));
    assertNotEquals(data, FixtureData.create("MIXED", 200, 124));
    assertThrows(UnsupportedOperationException.class, () -> data.set(0, 42.));
  }

  @Test
  void duplicatesRetainTheExactExpectedMultiplicity() {
    var data = FixtureData.create("DUPLICATES", 100, 123);
    assertEquals(98, Collections.frequency(data, 0.));
    assertEquals(1, Collections.frequency(data, -1000.));
    assertEquals(1, Collections.frequency(data, 1000.));
  }

  @Test
  void orderedFixtureHasTheSameHistogramInDescendingOrder() {
    var data = FixtureData.create("DESCENDING", 100, 123);
    assertEquals(1000., data.get(0));
    assertEquals(-1000., data.get(99));
    for (int i = 1; i < data.size(); i++) {
      assertTrue(data.get(i - 1) >= data.get(i));
    }
  }

  @Test
  void invalidFixtureDeclarationsFailClearly() {
    assertThrows(IllegalArgumentException.class, () -> FixtureData.create("MIXED", 0, 1));
    assertThrows(IllegalArgumentException.class, () -> FixtureData.create("MIXED", 101, 1));
    assertThrows(IllegalArgumentException.class, () -> FixtureData.create("UNKNOWN", 100, 1));
  }

  @Test
  void analyticalChecksAcceptBothRealAlgorithmsForEveryDistribution() {
    for (String kind : new String[] {"MIXED", "DUPLICATES", "DESCENDING"}) {
      var data = FixtureData.create(kind, 1000, 123);
      FixtureChecks.validate(data, kind, new IqrDetector().detect(data, 1.5));
      FixtureChecks.validate(data, kind, new ZScoreDetector().detect(data, 3));
      assertEquals(1000, data.size());
    }
  }

  @Test
  void validationRejectsWrongStatisticsRatherThanMerelyCountingResults() {
    var data = FixtureData.create("MIXED", 100, 123);
    var actual = new IqrDetector().detect(data, 1.5);
    var wrong =
        new IqrResult(
            actual.summary(),
            24.5,
            actual.q3(),
            actual.iqr(),
            actual.lowerBound(),
            actual.upperBound());
    assertThrows(IllegalStateException.class, () -> FixtureChecks.validate(data, "MIXED", wrong));
  }

  @Test
  void validationRejectsWrongOrderWithUnchangedCounts() {
    var data = FixtureData.create("MIXED", 100, 123);
    var actual = new IqrDetector().detect(data, 1.5);
    var normal = new ArrayList<>(actual.summary().normalValues());
    Collections.reverse(normal);
    var old = actual.summary();
    var wrong =
        new IqrResult(
            new DetectionSummary(
                old.algorithm(),
                old.threshold(),
                old.status(),
                old.warnings(),
                old.anomalies(),
                normal),
            actual.q1(),
            actual.q3(),
            actual.iqr(),
            actual.lowerBound(),
            actual.upperBound());
    assertThrows(IllegalStateException.class, () -> FixtureChecks.validate(data, "MIXED", wrong));
  }
}
