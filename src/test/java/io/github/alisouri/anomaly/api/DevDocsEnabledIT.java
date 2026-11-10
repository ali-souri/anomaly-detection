package io.github.alisouri.anomaly.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "SWAGGER_ENABLED=true")
class DevDocsEnabledIT extends HttpTestSupport {
  @Test
  void algorithmAlternativesHaveAnUnambiguousDiscriminator() throws Exception {
    var document = json(get("/v3/api-docs"));
    var schemas = document.path("components").path("schemas");
    var response = schemas.path("DetectionResponse");
    assertThat(response.path("oneOf").size()).isEqualTo(2);
    assertThat(response.path("discriminator").path("propertyName").asText()).isEqualTo("algorithm");
    assertThat(response.path("discriminator").path("mapping").path("IQR").asText())
        .endsWith("/IqrResponse");
    assertThat(response.path("discriminator").path("mapping").path("Z_SCORE").asText())
        .endsWith("/ZScoreResponse");
    assertThat(
            schemas
                .path("IqrResponse")
                .path("properties")
                .path("algorithm")
                .path("enum")
                .toString())
        .isEqualTo("[\"IQR\"]");
    assertThat(
            schemas
                .path("ZScoreResponse")
                .path("properties")
                .path("algorithm")
                .path("enum")
                .toString())
        .isEqualTo("[\"Z_SCORE\"]");
    assertThat(schemas.path("ZScoreResponse").path("properties").has("q1")).isFalse();
    assertThat(schemas.path("IqrResponse").path("properties").has("mean")).isFalse();
  }

  @Test
  void generatedContractDocumentsRequestsAndAllResponses() throws Exception {
    var document = json(get("/v3/api-docs"));
    var operation = document.path("paths").path("/api/v1/anomaly-detection").path("post");
    assertThat(operation.path("summary").asText()).isNotBlank();
    for (String status : new String[] {"200", "400", "405", "415", "422", "500"}) {
      assertThat(operation.path("responses").has(status)).as(status).isTrue();
    }
    assertThat(
            operation
                .path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema")
                .path("$ref")
                .asText())
        .contains("DetectionRequest");
    var schema = document.path("components").path("schemas").path("DetectionRequest");
    assertThat(schema.path("additionalProperties").asBoolean(true)).isFalse();
    assertThat(schema.path("required").toString())
        .contains("algorithm", "data")
        .doesNotContain("threshold");
  }

  @Test
  void devAndExplicitFlagEnableBothDocumentationEndpoints() throws Exception {
    var response = get("/v3/api-docs");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).path("paths").has("/api/v1/anomaly-detection")).isTrue();
    assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
    assertThat(get("/swagger-ui.html").statusCode()).isIn(302, 301);
  }
}
