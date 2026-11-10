package io.anomaly.core.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.anomaly.core.exception.NumericalRangeException;
import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.WarningCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZScoreDetectorTest {
  @Test
  void distinctAdjacentValuesMustNotCollapseToZeroVariance() {
    assertThatThrownBy(() -> new ZScoreDetector().detect(List.of(Math.nextUp(1.0), 1.0), 3))
        .isInstanceOf(NumericalRangeException.class)
        .hasMessage("Variance cannot be represented safely.");
  }

  @Test
  void dividingATinyNonzeroSquaredDeviationMustNotUnderflowToZeroVariance() {
    double value = Math.sqrt(2 * Double.MIN_VALUE);
    assertThatThrownBy(() -> new ZScoreDetector().detect(List.of(0., 0., value), 3))
        .isInstanceOf(NumericalRangeException.class)
        .hasMessage("Variance cannot be represented safely.");
  }

  private final ZScoreDetector detector = new ZScoreDetector();

  @Test
  void knownPopulationStatisticsUseDivisorNAndCustomThreshold() {
    // Sum=40, squared deviations sum=32, n=8: mean=5, variance=4, sd=2.
    var result = detector.detect(List.of(2., 4., 4., 4., 5., 5., 7., 9.), 1.5);
    assertThat(result.mean()).isEqualTo(5);
    assertThat(result.standardDeviation()).isEqualTo(2);
    assertThat(result.summary().anomalies()).containsExactly(9.);
    assertThat(result.summary().normalValues()).containsExactly(2., 4., 4., 4., 5., 5., 7.);
  }

  @Test
  void defaultFlagsKnownOutlier() {
    var data = new ArrayList<Double>();
    for (int i = 0; i < 10; i++) {
      data.add(0.);
    }
    data.add(10.);
    var result = detector.detect(data, detector.defaultThreshold());
    assertThat(detector.algorithm()).isEqualTo("Z_SCORE");
    assertThat(detector.defaultThreshold()).isEqualTo(3);
    assertThat(result.mean()).isCloseTo(10. / 11, within(1e-14));
    assertThat(result.standardDeviation()).isCloseTo(2.874797872880345, within(1e-14));
    assertThat(result.summary().anomalies()).containsExactly(10.);
  }

  @Test
  void theOriginalIqrSampleDoesNotHaveAThreeSigmaOutlier() {
    var result = detector.detect(List.of(10., 12., 11., 13., 12., 14., 11., 13., 12., 100.), 3);
    assertThat(result.summary().anomalies()).isEmpty();
    assertThat(result.summary().normalValues()).hasSize(10);
  }

  @Test
  void exactScoreBoundaryIsNormalAndZeroThresholdOnlyKeepsTheMean() {
    var boundary = detector.detect(List.of(-1., 1.), 1);
    assertThat(boundary.mean()).isZero();
    assertThat(boundary.standardDeviation()).isEqualTo(1);
    assertThat(boundary.summary().anomalies()).isEmpty();
    var zero = detector.detect(List.of(-1., 0., 1.), 0);
    assertThat(zero.summary().anomalies()).containsExactly(-1., 1.);
    assertThat(zero.summary().normalValues()).containsExactly(0.);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0, -5, Double.MAX_VALUE})
  void zeroVarianceNeverDividesByZero(double value) {
    var result = detector.detect(List.of(value, value, value, value), 0);
    assertThat(result.mean()).isEqualTo(value);
    assertThat(result.standardDeviation()).isZero();
    assertThat(result.summary().anomalies()).isEmpty();
    assertThat(result.summary().normalValues()).containsExactly(value, value, value, value);
    assertThat(result.summary().warnings()).extracting(Enum::name).containsExactly("ZERO_VARIANCE");
  }

  @Test
  void singletonHasBothWarnings() {
    var result = detector.detect(List.of(7.), 3);
    assertThat(result.mean()).isEqualTo(7);
    assertThat(result.standardDeviation()).isZero();
    assertThat(result.summary().status()).isEqualTo(DetectionStatus.COMPLETED);
    assertThat(result.summary().warnings())
        .extracting(Enum::name)
        .containsExactly("SMALL_DATASET", "ZERO_VARIANCE");
  }

  @Test
  void twoAndThreeValuesUsePopulationStatisticsAndSmallDatasetWarning() {
    var two = detector.detect(List.of(-1., 1.), 3);
    assertThat(two.standardDeviation()).isEqualTo(1);
    assertThat(two.summary().warnings()).containsExactly(WarningCode.SMALL_DATASET);
    var three = detector.detect(List.of(-3., 0., 3.), 3);
    assertThat(three.mean()).isZero();
    assertThat(three.standardDeviation()).isCloseTo(Math.sqrt(6), within(1e-14));
    assertThat(three.summary().warnings()).containsExactly(WarningCode.SMALL_DATASET);
  }

  @Test
  void emptyHasNullStatisticsAndNoWarnings() {
    var result = detector.detect(List.of(), 3);
    assertThat(result.mean()).isNull();
    assertThat(result.standardDeviation()).isNull();
    assertThat(result.summary().status()).isEqualTo(DetectionStatus.NO_DATA);
    assertThat(result.summary().anomalies()).isEmpty();
    assertThat(result.summary().normalValues()).isEmpty();
    assertThat(result.summary().warnings()).isEmpty();
  }

  @Test
  void duplicatesOrderAndCallerDataArePreserved() {
    var data = new ArrayList<>(List.of(9., 2., 5., 9., 4., 5., 4., 4.));
    var before = new ArrayList<>(data);
    var result = detector.detect(data, 1);
    assertThat(result.mean()).isCloseTo(5.25, within(1e-14));
    assertThat(result.standardDeviation()).isCloseTo(Math.sqrt(5.4375), within(1e-14));
    assertThat(result.summary().anomalies()).containsExactly(9., 2., 9.);
    assertThat(result.summary().normalValues()).containsExactly(5., 4., 5., 4., 4.);
    assertThat(data).isEqualTo(before);
    assertThatThrownBy(() -> result.summary().anomalies().add(1.))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void largeOffsetDoesNotCauseCatastrophicVarianceCancellation() {
    var result = detector.detect(List.of(1e12 + 1, 1e12 + 2, 1e12 + 3), 3);
    assertThat(result.mean()).isEqualTo(1e12 + 2);
    assertThat(result.standardDeviation()).isCloseTo(Math.sqrt(2. / 3), within(1e-14));
  }

  @Test
  void unsafeIntermediateOverflowAndVarianceUnderflowAreRejected() {
    for (var data :
        List.of(
            List.of(-Double.MAX_VALUE, Double.MAX_VALUE),
            List.of(1e200, -1e200),
            List.of(0., Double.MIN_VALUE),
            List.of(0., 1e-200))) {
      assertThatThrownBy(() -> detector.detect(data, 3))
          .isInstanceOf(NumericalRangeException.class);
    }
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void validatesThresholdEvenWhenEmpty(double threshold) {
    assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(List.of(), threshold));
  }

  @Test
  void validatesData() {
    assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(null, 3));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> detector.detect(Arrays.asList(1., null), 3));
    assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(List.of(Double.NaN), 3));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> detector.detect(List.of(Double.NEGATIVE_INFINITY), 3));
  }
}
