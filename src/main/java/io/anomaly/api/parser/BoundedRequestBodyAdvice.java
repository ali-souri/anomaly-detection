package io.anomaly.api.parser;

import io.anomaly.api.controller.DetectionController;
import io.anomaly.api.exception.InputTooLargeException;
import io.anomaly.config.RequestLimits;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/** Bounds raw bytes before Jackson creates a JSON tree, including unknown/chunked body lengths. */
@ControllerAdvice(assignableTypes = DetectionController.class)
public final class BoundedRequestBodyAdvice extends RequestBodyAdviceAdapter {
  private final int maxBytes;

  /**
   * Creates the detection controller's bounded raw-body reader.
   *
   * @param limits validated per-instance settings
   */
  public BoundedRequestBodyAdvice(RequestLimits limits) {
    maxBytes = limits.maxBodyBytes();
  }

  @Override
  public boolean supports(
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage input,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    byte[] bytes = input.getBody().readNBytes(maxBytes + 1);
    if (bytes.length > maxBytes) {
      throw new InputTooLargeException("Request body exceeds the configured byte limit.");
    }
    return new HttpInputMessage() {
      @Override
      public InputStream getBody() {
        return new ByteArrayInputStream(bytes);
      }

      @Override
      public HttpHeaders getHeaders() {
        return input.getHeaders();
      }
    };
  }
}
