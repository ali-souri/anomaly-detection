package io.anomaly.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.anomaly.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevDocsDisabledIT extends HttpTestSupport {
  @Test
  void devStillDefaultsToDisabled() throws Exception {
    assertThat(get("/v3/api-docs").statusCode()).isEqualTo(404);
    assertThat(get("/v3/api-docs.yaml").statusCode()).isEqualTo(404);
    assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(404);
  }
}
