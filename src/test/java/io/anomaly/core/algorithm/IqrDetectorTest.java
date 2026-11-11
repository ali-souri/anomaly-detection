package io.anomaly.core.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.anomaly.core.exception.NumericalRangeException;
import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.WarningCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IqrDetectorTest {
  private final IqrDetector detector = new IqrDetector();

  @Test
  void quartileOrderingRetainsSignedZeroAndOriginalPartitionSigns() {
    var input = List.of(0.0, -0.0, 0.0, -0.0);
    var result = detector.detect(input, 1.5);
    assertThat(Double.doubleToRawLongBits(result.q1())).isEqualTo(Long.MIN_VALUE);
    assertThat(Double.doubleToRawLongBits(result.q3())).isZero();
    assertThat(result.summary().anomalies()).isEmpty();
    assertThat(result.summary().normalValues()).containsExactly(0.0, -0.0, 0.0, -0.0);
    assertThat(input).containsExactly(0.0, -0.0, 0.0, -0.0);
  }

  @Test
  void permutedDuplicateValuesUseGlobalHalvesAndStrictZeroThreshold() {
    var input = List.of(4., -4., 0., 3., -1., -4., 1., -0.0, -3., 4.);
    var result = detector.detect(input, 0);
    assertThat(result.q1()).isEqualTo(-3);
    assertThat(result.q3()).isEqualTo(3);
    assertThat(result.summary().anomalies()).containsExactly(4., -4., -4., 4.);
    assertThat(result.summary().normalValues()).containsExactly(0., 3., -1., 1., -0.0, -3.);
  }

  @Test
  void sampleUsesHandCalculatedQuartilesAndStablePartitions() {
    var data = new ArrayList<>(List.of(10., 12., 11., 13., 12., 14., 11., 13., 12., 100.));
    var before = new ArrayList<>(data);
    var result = detector.detect(data, 1.5);
    assertThat(result.q1()).isEqualTo(11);
    assertThat(result.q3()).isEqualTo(13);
    assertThat(result.iqr()).isEqualTo(2);
    assertThat(result.lowerBound()).isEqualTo(8);
    assertThat(result.upperBound()).isEqualTo(16);
    assertThat(result.summary().anomalies()).containsExactly(100.);
    assertThat(result.summary().normalValues())
        .containsExactly(10., 12., 11., 13., 12., 14., 11., 13., 12.);
    assertThat(data).isEqualTo(before);
    assertThat(result.summary().status()).isEqualTo(DetectionStatus.COMPLETED);
    assertThat(result.summary().warnings()).isEmpty();
  }

  @Test
  void emptyHasExplicitlyUnavailableStatistics() {
    var result = detector.detect(List.of(), 1.5);
    assertThat(result.summary().status()).isEqualTo(DetectionStatus.NO_DATA);
    assertThat(result.summary().anomalies()).isEmpty();
    assertThat(result.summary().normalValues()).isEmpty();
    assertThat(result.summary().warnings()).isEmpty();
    assertThat(
            Arrays.asList(
                result.q1(), result.q3(), result.iqr(), result.lowerBound(), result.upperBound()))
        .containsOnlyNulls();
  }

  @ParameterizedTest
  @CsvSource({"1,2,2", "2,2,4", "3,2,6", "4,3,7", "5,3,9", "6,4,10"})
  void handCalculatedSmallEvenAndOddQuartiles(int count, double q1, double q3) {
    var data = new ArrayList<Double>();
    for (int i = 1; i <= count; i++) {
      data.add(i * 2.);
    }
    var result = detector.detect(data, 1.5);
    assertThat(result.q1()).isEqualTo(q1);
    assertThat(result.q3()).isEqualTo(q3);
    assertThat(result.summary().normalValues()).isEqualTo(data);
    assertThat(result.summary().status()).isEqualTo(DetectionStatus.COMPLETED);
    assertThat(result.summary().warnings())
        .isEqualTo(count < 4 ? List.of(WarningCode.SMALL_DATASET) : List.of());
  }

  @Test
  void negativesDecimalsAndBothTails() {
    var result = detector.detect(List.of(-100., -2.5, -2., -1.5, -1., 0., 0.5, 100.), 1.5);
    assertThat(result.q1()).isEqualTo(-2.25);
    assertThat(result.q3()).isEqualTo(0.25);
    assertThat(result.summary().anomalies()).containsExactly(-100., 100.);
  }

  @Test
  void exactBoundariesAndZeroThresholdAreNormal() {
    var result = detector.detect(List.of(-2., 0., 0., 1., 1., 2., 2., 4.), 1.);
    assertThat(result.lowerBound()).isEqualTo(-2);
    assertThat(result.upperBound()).isEqualTo(4);
    assertThat(result.summary().anomalies()).isEmpty();
    var zero = detector.detect(List.of(0., 0., 1., 1., 2., 2.), 0);
    assertThat(zero.summary().anomalies()).isEmpty();
    var zeroWithTails = detector.detect(List.of(1., 2., 3., 4.), 0);
    assertThat(zeroWithTails.summary().anomalies()).containsExactly(1., 4.);
    assertThat(zeroWithTails.summary().normalValues()).containsExactly(2., 3.);
  }

  @Test
  void zeroIqrDoesNotInventWiderBoundsAndRetainsDuplicates() {
    var result = detector.detect(List.of(9., 2., 2., 2., 2., 2., 2., -9.), 1.5);
    assertThat(result.iqr()).isZero();
    assertThat(result.summary().anomalies()).containsExactly(9., -9.);
    assertThat(result.summary().normalValues()).containsExactly(2., 2., 2., 2., 2., 2.);
    assertThatThrownBy(() -> result.summary().normalValues().add(7.))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void defaultAndVeryLargeEqualValuesAreSafe() {
    assertThat(detector.algorithm()).isEqualTo("IQR");
    assertThat(detector.defaultThreshold()).isEqualTo(1.5);
    var result =
        detector.detect(
            List.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
            Double.MAX_VALUE);
    assertThat(result.q1()).isEqualTo(Double.MAX_VALUE);
    assertThat(result.iqr()).isZero();
    assertThat(detector.detect(List.of(Double.MIN_VALUE, Double.MIN_VALUE), 0).q1())
        .isEqualTo(Double.MIN_VALUE);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void invalidThresholdIsRejectedEvenForEmptyInput(double threshold) {
    assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(List.of(), threshold));
  }

  @Test
  void rejectsInvalidDataAndUnsafeComputedBounds() {
    assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(null, 1.5));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> detector.detect(Arrays.asList(1., null), 1.5));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> detector.detect(List.of(Double.NaN), 1.5));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> detector.detect(List.of(Double.POSITIVE_INFINITY), 1.5));
    assertThatThrownBy(() -> detector.detect(List.of(-Double.MAX_VALUE, Double.MAX_VALUE), 1.5))
        .isInstanceOf(NumericalRangeException.class);
    assertThatThrownBy(() -> detector.detect(List.of(0., 1e308), 2))
        .isInstanceOf(NumericalRangeException.class);
    assertThatThrownBy(() -> detector.detect(List.of(1e308, 1.5e308), 1.5))
        .isInstanceOf(NumericalRangeException.class);
  }
}
