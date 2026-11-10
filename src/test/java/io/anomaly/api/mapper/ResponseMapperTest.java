package io.anomaly.api.mapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.anomaly.core.model.DetectionResult;
import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.DetectionSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseMapperTest {
  @Test
  void aCoreExtensionNeedsAnExplicitApiRepresentation() {
    var summary =
        new DetectionSummary(
            "TEST_ONLY", 1, DetectionStatus.COMPLETED, List.of(), List.of(), List.of(7.));
    DetectionResult additionalCoreResult = () -> summary;
    assertThatThrownBy(() -> new ResponseMapper().toResponse(additionalCoreResult))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No API representation configured for this result type.");
  }
}
