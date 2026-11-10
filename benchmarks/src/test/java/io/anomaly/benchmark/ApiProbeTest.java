package io.anomaly.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.core.algorithm.IqrDetector;
import io.anomaly.core.algorithm.ZScoreDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiProbeTest {
  @TempDir Path directory;

  @Test
  void generatedRequestsHaveExactFullInputAndOmittedThreshold() throws Exception {
    var data = FixtureData.create("MIXED", 100, 20261110);
    Path request = directory.resolve("request.json");
    ApiProbe.writeRequest(request, "IQR", data);
    var tree = new ObjectMapper().readTree(request.toFile());
    assertEquals("IQR", tree.get("algorithm").asText());
    assertEquals(new ObjectMapper().valueToTree(data), tree.get("data"));
    assertEquals(2, tree.size());
  }

  @Test
  void validatorRejectsTruncatedAndReorderedFullResponses() throws Exception {
    var data = FixtureData.create("MIXED", 100, 20261110);
    var mapper = new ObjectMapper();
    for (var detector :
        new io.anomaly.core.algorithm.AnomalyDetector[] {new IqrDetector(), new ZScoreDetector()}) {
      var result = detector.detect(data, detector.defaultThreshold());
      var response = mapper.createObjectNode();
      response.put("algorithm", detector.algorithm());
      response.put("threshold", detector.defaultThreshold());
      response.put("status", "COMPLETED");
      response.putArray("warnings");
      response.set("anomalies", mapper.valueToTree(result.summary().anomalies()));
      response.set("normalValues", mapper.valueToTree(result.summary().normalValues()));
      FixtureChecks.statistics(detector.algorithm(), "MIXED").forEach(response::put);
      Path file = directory.resolve(detector.algorithm() + ".json");
      mapper.writeValue(file.toFile(), response);
      ApiProbe.validate(file, detector.algorithm(), data);
      ((com.fasterxml.jackson.databind.node.ArrayNode) response.get("normalValues")).remove(0);
      mapper.writeValue(file.toFile(), response);
      assertThrows(
          IllegalStateException.class, () -> ApiProbe.validate(file, detector.algorithm(), data));
      Files.writeString(file, "{");
      assertThrows(Exception.class, () -> ApiProbe.validate(file, detector.algorithm(), data));
    }
  }
}
