package io.anomaly.api.dto;

import io.anomaly.core.model.DetectionStatus;
import io.anomaly.core.model.WarningCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Flat IQR wire representation. Statistics use input units and are explicitly null for NO_DATA.
 *
 * @param algorithm always IQR
 * @param threshold effective dimensionless IQR multiplier
 * @param status NO_DATA for empty input, otherwise COMPLETED
 * @param warnings advisory codes, including SMALL_DATASET for one to three values
 * @param anomalies ordered anomalous original values
 * @param normalValues ordered normal original values
 * @param q1 median of lower half; null for NO_DATA
 * @param q3 median of upper half; null for NO_DATA
 * @param iqr Q3 minus Q1; null for NO_DATA
 * @param lowerBound strict lower classification boundary; null for NO_DATA
 * @param upperBound strict upper classification boundary; null for NO_DATA
 */
@Schema(
    requiredProperties = {
      "algorithm",
      "threshold",
      "status",
      "warnings",
      "anomalies",
      "normalValues",
      "q1",
      "q3",
      "iqr",
      "lowerBound",
      "upperBound"
    })
public record IqrResponse(
    @Schema(allowableValues = "IQR", requiredMode = Schema.RequiredMode.REQUIRED) String algorithm,
    double threshold,
    DetectionStatus status,
    List<WarningCode> warnings,
    List<Double> anomalies,
    List<Double> normalValues,
    @Schema(nullable = true) Double q1,
    @Schema(nullable = true) Double q3,
    @Schema(nullable = true) Double iqr,
    @Schema(nullable = true) Double lowerBound,
    @Schema(nullable = true) Double upperBound)
    implements DetectionResponse {}
