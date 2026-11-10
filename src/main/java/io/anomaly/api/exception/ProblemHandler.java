package io.anomaly.api.exception;

import io.anomaly.core.exception.NumericalRangeException;
import io.anomaly.core.exception.UnsupportedAlgorithmException;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** RFC 9457 errors with stable codes; framework statuses and headers remain intact. */
@RestControllerAdvice
public final class ProblemHandler extends ResponseEntityExceptionHandler {
  /** Creates shared, stateless HTTP exception handling. */
  public ProblemHandler() {}

  @ExceptionHandler(InputTooLargeException.class)
  ResponseEntity<ProblemDetail> tooLarge(InputTooLargeException error, WebRequest request) {
    return response(HttpStatus.PAYLOAD_TOO_LARGE, "INPUT_TOO_LARGE", error.getMessage(), request);
  }

  @ExceptionHandler(UnsupportedAlgorithmException.class)
  ResponseEntity<ProblemDetail> unsupported(
      UnsupportedAlgorithmException error, WebRequest request) {
    return response(HttpStatus.BAD_REQUEST, "UNSUPPORTED_ALGORITHM", error.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> invalid(IllegalArgumentException error, WebRequest request) {
    return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage(), request);
  }

  @ExceptionHandler(NumericalRangeException.class)
  ResponseEntity<ProblemDetail> numerical(NumericalRangeException error, WebRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "NUMERICAL_RANGE", error.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception error, WebRequest request) {
    logger.error("Unexpected anomaly detection error", error);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected internal error occurred.",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception error,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String code =
        switch (status.value()) {
          case 400 -> "MALFORMED_JSON";
          case 405 -> "METHOD_NOT_ALLOWED";
          case 415 -> "UNSUPPORTED_MEDIA_TYPE";
          default -> "HTTP_" + status.value();
        };
    String detail =
        switch (status.value()) {
          case 400 -> "Malformed JSON request.";
          case 405 -> "This HTTP method is not supported for the resource.";
          case 415 -> "Content type must be application/json.";
          default -> "The HTTP request cannot be processed.";
        };
    return super.handleExceptionInternal(
        error,
        problem(HttpStatus.valueOf(status.value()), code, detail, request),
        headers,
        status,
        request);
  }

  private static ResponseEntity<ProblemDetail> response(
      HttpStatus status, String code, String detail, WebRequest request) {
    return ResponseEntity.status(status).body(problem(status, code, detail, request));
  }

  private static ProblemDetail problem(
      HttpStatus status, String code, String detail, WebRequest request) {
    return problem(
        status, code, detail, ((ServletWebRequest) request).getRequest().getRequestURI());
  }

  /**
   * Builds the same public error representation for filters and controller advice.
   *
   * @param status HTTP status
   * @param code stable machine-readable code
   * @param detail safe public explanation
   * @param path request URI path
   * @return an RFC 9457 problem with the stable code extension
   */
  public static ProblemDetail problem(HttpStatus status, String code, String detail, String path) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create("urn:anomaly-detection:problem:" + code.toLowerCase(Locale.ROOT)));
    problem.setTitle(status.getReasonPhrase());
    problem.setInstance(URI.create(path));
    problem.setProperty("code", code);
    return problem;
  }
}
