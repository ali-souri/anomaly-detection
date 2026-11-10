package io.github.alisouri.anomaly.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionServiceTest {
  @Test
  void omissionUsesSelectedDefaultAndZeroIsAnOverride() {
    var service = new DetectionService(new AlgorithmRegistry(List.of(new IqrDetector())));
    assertThat(service.detect("IQR", List.of(1.), null).summary().threshold()).isEqualTo(1.5);
    assertThat(service.detect("IQR", List.of(), 0.).summary().threshold()).isZero();
  }

  @Test
  void unknownAndNonCanonicalNamesFailEvenForEmptyInput() {
    var service = new DetectionService(new AlgorithmRegistry(List.of(new IqrDetector())));
    for (String name : new String[] {"MISSING", "iqr", " IQR ", null}) {
      assertThatThrownBy(() -> service.detect(name, List.of(), null))
          .isInstanceOf(UnsupportedAlgorithmException.class);
    }
  }

  @Test
  void aNewTestStrategyWorksWithoutChangingSelectionLogic() {
    AnomalyDetector additional =
        new AnomalyDetector() {
          @Override
          public String algorithm() {
            return "TEST_ONLY";
          }

          @Override
          public double defaultThreshold() {
            return 7;
          }

          @Override
          public DetectionResult detect(List<Double> data, double threshold) {
            var summary =
                new DetectionSummary(
                    algorithm(), threshold, DetectionStatus.COMPLETED, List.of(), List.of(), data);
            return () -> summary;
          }
        };
    var service =
        new DetectionService(new AlgorithmRegistry(List.of(new IqrDetector(), additional)));
    var result = service.detect("TEST_ONLY", List.of(3., 3., 1.), null);
    assertThat(result.summary().algorithm()).isEqualTo("TEST_ONLY");
    assertThat(result.summary().threshold()).isEqualTo(7);
    assertThat(result.summary().normalValues()).containsExactly(3., 3., 1.);
  }

  @Test
  void duplicateIdentifiersFailAtStartup() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AlgorithmRegistry(List.of(new IqrDetector(), new IqrDetector())));
  }
}
