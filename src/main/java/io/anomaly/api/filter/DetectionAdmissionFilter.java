package io.anomaly.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.api.exception.ProblemHandler;
import io.anomaly.config.RequestLimits;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Immediate admission before body conversion; the permit covers the entire synchronous response.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class DetectionAdmissionFilter extends OncePerRequestFilter {
  private final RequestLimits limits;
  private final ObjectMapper mapper;
  private final Semaphore permits;

  /**
   * Creates one bounded admission policy per application instance.
   *
   * @param limits validated startup settings
   * @param mapper the application's Problem Details aware JSON mapper
   */
  public DetectionAdmissionFilter(RequestLimits limits, ObjectMapper mapper) {
    this.limits = limits;
    this.mapper = mapper;
    permits = new Semaphore(limits.maxActive());
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"POST".equals(request.getMethod())
        || !"/api/v1/anomaly-detection".equals(request.getServletPath());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > limits.maxBodyBytes()) {
      reject(
          request,
          response,
          HttpStatus.PAYLOAD_TOO_LARGE,
          "INPUT_TOO_LARGE",
          "Request body exceeds the configured byte limit.");
      return;
    }
    if (!permits.tryAcquire()) {
      response.setHeader("Retry-After", "1");
      reject(
          request,
          response,
          HttpStatus.SERVICE_UNAVAILABLE,
          "ADMISSION_FULL",
          "Detection capacity is occupied. Retry after one second.");
      return;
    }
    try {
      chain.doFilter(request, response);
    } finally {
      permits.release();
    }
  }

  private void reject(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    mapper.writeValue(
        response.getOutputStream(),
        ProblemHandler.problem(status, code, detail, request.getRequestURI()));
  }
}
