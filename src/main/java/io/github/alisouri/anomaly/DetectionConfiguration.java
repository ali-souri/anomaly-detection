package io.github.alisouri.anomaly;

import io.github.alisouri.anomaly.api.RequestParser;
import io.github.alisouri.anomaly.api.ResponseMapper;
import io.github.alisouri.anomaly.core.AlgorithmRegistry;
import io.github.alisouri.anomaly.core.AnomalyDetector;
import io.github.alisouri.anomaly.core.DetectionService;
import io.github.alisouri.anomaly.core.IqrDetector;
import io.github.alisouri.anomaly.core.ZScoreDetector;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring composition root; the core has no framework annotations or HTTP dependencies. */
@Configuration(proxyBeanMethods = false)
public class DetectionConfiguration {
  /** Creates the composition root discovered by Spring. */
  public DetectionConfiguration() {}

  @Bean
  IqrDetector iqrDetector() {
    return new IqrDetector();
  }

  @Bean
  ZScoreDetector zScoreDetector() {
    return new ZScoreDetector();
  }

  @Bean
  AlgorithmRegistry algorithmRegistry(List<AnomalyDetector> detectors) {
    return new AlgorithmRegistry(detectors);
  }

  @Bean
  DetectionService detectionService(AlgorithmRegistry registry) {
    return new DetectionService(registry);
  }

  @Bean
  RequestParser requestParser() {
    return new RequestParser();
  }

  @Bean
  ResponseMapper responseMapper() {
    return new ResponseMapper();
  }

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Anomaly detection")
                .version("0.2.0")
                .description(
                    "Finite JSON numbers; ordered partitions; IQR median-of-halves or population Z-Score. See docs/api.md for numerical conventions and stable Problem Details codes."));
  }
}
