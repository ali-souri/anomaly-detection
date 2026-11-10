package io.anomaly.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RequestLimitsTest {
  private final ApplicationContextRunner context =
      new ApplicationContextRunner().withUserConfiguration(Binding.class);

  @Test
  void defaultsAndEnvironmentStyleOverridesBind() {
    context.run(
        app ->
            assertThat(app.getBean(RequestLimits.class))
                .isEqualTo(new RequestLimits(1, 2_000_000, 67_108_864)));
    context
        .withPropertyValues(
            "detection.limits.max-active=4",
            "detection.limits.max-numbers=100",
            "detection.limits.max-body-bytes=4096")
        .run(
            app ->
                assertThat(app.getBean(RequestLimits.class))
                    .isEqualTo(new RequestLimits(4, 100, 4096)));
  }

  @ParameterizedTest
  @CsvSource({"0,1,1", "-1,1,1", "1,0,1", "1,-1,1", "1,1,0", "1,1,-1", "1,1,2147483647"})
  void invalidPoliciesAreRejected(int active, int numbers, int bytes) {
    assertThatThrownBy(() -> new RequestLimits(active, numbers, bytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void invalidBoundConfigurationPreventsStartup() {
    context
        .withPropertyValues("detection.limits.max-active=0")
        .run(app -> assertThat(app).hasFailed());
    context
        .withPropertyValues("detection.limits.max-body-bytes=not-a-number")
        .run(app -> assertThat(app).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(RequestLimits.class)
  static class Binding {}
}
