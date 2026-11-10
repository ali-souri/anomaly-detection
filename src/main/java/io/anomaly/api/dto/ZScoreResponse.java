package io.anomaly.api.dto;

import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.WarningCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Flat population Z-Score wire result, without IQR-only fields.
 *
 * @param algorithm always Z_SCORE
 * @param threshold effective dimensionless population standard-deviation multiplier
 * @param status NO_DATA for empty input, otherwise COMPLETED
 * @param warnings SMALL_DATASET for 1..3 values; ZERO_VARIANCE when applicable
 * @param anomalies ordered anomalous original values
 * @param normalValues ordered normal original values
 * @param mean arithmetic mean in input units, explicitly null for NO_DATA
 * @param standardDeviation population standard deviation in input units, null for NO_DATA
 */
@Schema(
    requiredProperties = {
      "algorithm",
      "threshold",
      "status",
      "warnings",
      "anomalies",
      "normalValues",
      "mean",
      "standardDeviation"
    })
public record ZScoreResponse(
    @Schema(allowableValues = "Z_SCORE", requiredMode = Schema.RequiredMode.REQUIRED)
        String algorithm,
    double threshold,
    DetectionStatus status,
    List<WarningCode> warnings,
    List<Double> anomalies,
    List<Double> normalValues,
    @Schema(nullable = true) Double mean,
    @Schema(nullable = true) Double standardDeviation)
    implements DetectionResponse {}
