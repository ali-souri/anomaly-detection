package io.anomaly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Positive, per-instance demonstration policies, not measured capacity promises.
 *
 * @param maxActive maximum simultaneous synchronous detection requests
 * @param maxNumbers inclusive number count limit
 * @param maxBodyBytes inclusive raw body byte limit, below Integer.MAX_VALUE for a bounded read
 */
@ConfigurationProperties("detection.limits")
public record RequestLimits(
    @DefaultValue("1") int maxActive,
    @DefaultValue("2000000") int maxNumbers,
    @DefaultValue("67108864") int maxBodyBytes) {
  /**
   * Rejects invalid policies during application startup.
   *
   * @throws IllegalArgumentException if a limit is nonpositive or the byte limit cannot be buffered
   */
  public RequestLimits {
    if (maxActive <= 0
        || maxNumbers <= 0
        || maxBodyBytes <= 0
        || maxBodyBytes == Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Request limits must be positive; max-body-bytes must be below Integer.MAX_VALUE.");
    }
  }
}
