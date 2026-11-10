package io.anomaly.benchmark;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real HTTP file-to-file probe; generation and streaming correctness checks are outside timing. */
public final class ApiProbe {
  private static final JsonFactory JSON = new JsonFactory();

  private ApiProbe() {}

  /**
   * Sends full-contract requests to a separately started released application.
   *
   * @param args base URL, ignored data directory, compact result JSON path
   * @throws Exception for HTTP, validation or file failures; no failed sample is accepted
   */
  public static void main(String[] args) throws Exception {
    HarnessEnvironment.requireUninstrumented();
    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    Path directory = Path.of(args[1]);
    Files.createDirectories(directory);
    var data = FixtureData.create("MIXED", 2_000_000, 20261110);
    var rows = new ArrayList<Map<String, Object>>();
    for (String algorithm : new String[] {"IQR", "Z_SCORE"}) {
      var warmData = FixtureData.create("MIXED", 10000, 20261110);
      Path warmRequest = directory.resolve(algorithm + "-warm-request.json");
      Path warmResponse = directory.resolve(algorithm + "-warm-response.json");
      writeRequest(warmRequest, algorithm, warmData);
      send(client, args[0], warmRequest, warmResponse);
      validate(warmResponse, algorithm, warmData);
      Path request = directory.resolve(algorithm + "-request.json");
      writeRequest(request, algorithm, data);
      for (int sample = 1; sample <= 3; sample++) {
        Path response = directory.resolve(algorithm + "-response-" + sample + ".json");
        double milliseconds = send(client, args[0], request, response);
        var statistics = validate(response, algorithm, data);
        var row = new HashMap<String, Object>();
        row.put("algorithm", algorithm);
        row.put("size", data.size());
        row.put("sample", sample);
        row.put("requestBytes", Files.size(request));
        row.put("responseBytes", Files.size(response));
        row.put("durationMs", milliseconds);
        row.put("status", 200);
        row.put("validated", true);
        row.put("anomalies", data.size() / 50);
        row.put("normalValues", data.size() * 49 / 50);
        row.put("statistics", statistics);
        rows.add(row);
        System.out.println(
            algorithm
                + " sample "
                + sample
                + ": "
                + milliseconds
                + " ms; full response validated.");
      }
    }
    var output =
        Map.of(
            "rows",
            rows,
            "seed",
            20261110,
            "warmupsPerAlgorithm",
            1,
            "samplesPerAlgorithm",
            3,
            "jvmArguments",
            ManagementFactory.getRuntimeMXBean().getInputArguments(),
            "clientMaxHeapBytes",
            Runtime.getRuntime().maxMemory(),
            "timingScope",
            "request-file read + loopback HTTP + complete response-file write; generation/validation excluded");
    new ObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValue(Path.of(args[2]).toFile(), output);
  }

  private static void writeRequest(Path path, String algorithm, List<Double> data)
      throws Exception {
    try (var writer = JSON.createGenerator(path.toFile(), JsonEncoding.UTF8)) {
      writer.writeStartObject();
      writer.writeStringField("algorithm", algorithm);
      writer.writeArrayFieldStart("data");
      for (double value : data) {
        writer.writeNumber(value);
      }
      writer.writeEndArray();
      writer.writeEndObject();
    }
  }

  private static double send(HttpClient client, String base, Path request, Path response)
      throws Exception {
    var message =
        HttpRequest.newBuilder(URI.create(base + "/api/v1/anomaly-detection"))
            .timeout(Duration.ofMinutes(2))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofFile(request))
            .build();
    long start = System.nanoTime();
    var result =
        client.send(
            message,
            HttpResponse.BodyHandlers.ofFile(
                response,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE));
    double milliseconds = (System.nanoTime() - start) / 1_000_000.;
    require(result.statusCode() == 200, "HTTP status " + result.statusCode());
    return milliseconds;
  }

  private static Map<String, Double> validate(Path response, String algorithm, List<Double> data)
      throws Exception {
    var expected = FixtureChecks.statistics(algorithm, "MIXED");
    var actual = new HashMap<String, Double>();
    var fields = new HashSet<String>();
    try (var parser = JSON.createParser(response.toFile())) {
      require(parser.nextToken() == JsonToken.START_OBJECT, "Expected response object.");
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String field = parser.currentName();
        fields.add(field);
        parser.nextToken();
        switch (field) {
          case "algorithm" -> require(parser.getText().equals(algorithm), "Algorithm mismatch.");
          case "threshold" ->
              require(
                  parser.getDoubleValue() == (algorithm.equals("IQR") ? 1.5 : 3),
                  "Default threshold mismatch.");
          case "status" -> require(parser.getText().equals("COMPLETED"), "Status mismatch.");
          case "warnings" ->
              require(
                  parser.currentToken() == JsonToken.START_ARRAY
                      && parser.nextToken() == JsonToken.END_ARRAY,
                  "Unexpected warning.");
          case "anomalies" ->
              require(partition(parser, data, true) == data.size() / 50, "Anomaly count.");
          case "normalValues" ->
              require(partition(parser, data, false) == data.size() * 49 / 50, "Normal count.");
          default -> {
            require(expected.containsKey(field), "Unexpected response field.");
            double value = parser.getDoubleValue();
            actual.put(field, value);
            if (algorithm.equals("IQR")) {
              require(value == expected.get(field), "IQR statistic mismatch.");
            } else {
              FixtureChecks.close(value, expected.get(field));
            }
          }
        }
      }
      require(parser.nextToken() == null, "Trailing JSON.");
    }
    var required = new HashSet<>(expected.keySet());
    required.addAll(
        Set.of("algorithm", "threshold", "status", "warnings", "anomalies", "normalValues"));
    require(fields.equals(required), "Missing response fields.");
    return actual;
  }

  private static int partition(JsonParser parser, List<Double> data, boolean anomalies)
      throws Exception {
    require(parser.currentToken() == JsonToken.START_ARRAY, "Expected partition array.");
    int cursor = 0;
    int count = 0;
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      while (cursor < data.size() && FixtureChecks.outlier(data.get(cursor)) != anomalies) {
        cursor++;
      }
      require(
          cursor < data.size()
              && parser.currentToken().isNumeric()
              && parser.getDoubleValue() == data.get(cursor),
          "Partition value/order mismatch.");
      cursor++;
      count++;
    }
    while (cursor < data.size() && FixtureChecks.outlier(data.get(cursor)) != anomalies) {
      cursor++;
    }
    require(cursor == data.size(), "Missing partition values.");
    return count;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
