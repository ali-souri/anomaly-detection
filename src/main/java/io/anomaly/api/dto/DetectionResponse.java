package io.anomaly.api.dto;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

/** Marker for documented algorithm-specific flat HTTP success responses. */
@Schema(
    oneOf = {IqrResponse.class, ZScoreResponse.class},
    discriminatorProperty = "algorithm",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "IQR", schema = IqrResponse.class),
      @DiscriminatorMapping(value = "Z_SCORE", schema = ZScoreResponse.class)
    })
public interface DetectionResponse {}
