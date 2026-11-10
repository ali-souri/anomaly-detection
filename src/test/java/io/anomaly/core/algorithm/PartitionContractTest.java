package io.anomaly.core.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PartitionContractTest {
  @ParameterizedTest
  @ValueSource(strings = {"IQR", "Z_SCORE"})
  void fullOrderedDuplicatePartitionsAreImmutableSnapshots(String algorithm) {
    AnomalyDetector detector = algorithm.equals("IQR") ? new IqrDetector() : new ZScoreDetector();
    var input = new ArrayList<>(Collections.nCopies(98, -0.0));
    input.add(0, 100.);
    input.add(-100.);
    var before = List.copyOf(input);
    var result = detector.detect(input, detector.defaultThreshold());
    assertThat(input).isEqualTo(before);
    assertThat(result.summary().anomalies()).containsExactly(100., -100.);
    assertThat(result.summary().normalValues()).hasSize(98).containsOnly(-0.0);
    input.clear();
    assertThat(result.summary().normalValues()).hasSize(98).containsOnly(-0.0);
    assertThat(result.summary().anomalies()).containsExactly(100., -100.);
    assertThatThrownBy(() -> result.summary().anomalies().set(0, 2.))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.summary().normalValues().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
