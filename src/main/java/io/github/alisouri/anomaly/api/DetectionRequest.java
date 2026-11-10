package io.github.alisouri.anomaly.api;

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
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "IQR") String algorithm,
    @Schema(
            minimum = "0",
            defaultValue = "1.5",
            description =
                "Finite non-negative IQR multiplier. Omit for default; explicit null is invalid.")
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
