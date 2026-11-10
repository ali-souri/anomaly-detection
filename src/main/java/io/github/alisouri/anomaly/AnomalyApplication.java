package io.github.alisouri.anomaly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the stateless anomaly detection HTTP application. */
@SpringBootApplication
public class AnomalyApplication {
  /**
   * Starts embedded HTTP serving.
   *
   * @param args Spring Boot command-line options
   */
  public static void main(String[] args) {
    SpringApplication.run(AnomalyApplication.class, args);
  }
}
