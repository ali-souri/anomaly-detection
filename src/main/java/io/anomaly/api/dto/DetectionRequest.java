package io.anomaly.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Validated API input. Only the parser may interpret omission versus explicit JSON null.
 *
 * @param algorithm case-sensitive canonical identifier
 * @param threshold null only when omitted; otherwise finite and non-negative
 * @param data required finite numbers, with duplicates and original order retained
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record DetectionRequest(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"IQR", "Z_SCORE"})
        String algorithm,
    @Schema(
            minimum = "0",
            description =
                "Finite non-negative sensitivity. IQR default 1.5; Z_SCORE default 3.0. Omit for the selected default; explicit null is invalid.")
        Double threshold,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description =
                "Finite JSON numbers only. Empty is valid; nulls and numeric strings are invalid.")
        List<Double> data) {
  /** Defensively copies validated input so a request cannot change during calculation. */
  public DetectionRequest {
    data = List.copyOf(data);
  }
}
