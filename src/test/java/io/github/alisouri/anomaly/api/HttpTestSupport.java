package io.github.alisouri.anomaly.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.boot.test.web.server.LocalServerPort;

abstract class HttpTestSupport {
  @LocalServerPort int port;
  final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  final ObjectMapper mapper = new ObjectMapper();

  URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  HttpResponse<String> post(String json) throws Exception {
    return send("POST", "/api/v1/anomaly-detection", "application/json", json);
  }

  HttpResponse<String> send(String method, String path, String contentType, String body)
      throws Exception {
    var builder = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15));
    if (contentType != null) {
      builder.header("Content-Type", contentType);
    }
    return client.send(
        builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  HttpResponse<String> get(String path) throws Exception {
    return send("GET", path, null, "");
  }

  JsonNode json(HttpResponse<String> response) throws Exception {
    return mapper.readTree(response.body());
  }
}
