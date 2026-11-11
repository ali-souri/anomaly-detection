package io.anomaly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.api.mapper.ResponseMapper;
import io.anomaly.api.parser.RequestParser;
import io.anomaly.core.algorithm.AnomalyDetector;
import io.anomaly.core.algorithm.IqrDetector;
import io.anomaly.core.algorithm.ZScoreDetector;
import io.anomaly.core.registry.AlgorithmRegistry;
import io.anomaly.core.service.DetectionService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring composition root; the core has no framework annotations or HTTP dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RequestLimits.class)
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
  RequestParser requestParser(RequestLimits limits, ObjectMapper mapper) {
    return new RequestParser(limits.maxNumbers(), mapper.getFactory());
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
                .version("0.3.0-SNAPSHOT")
                .description(
                    "Finite JSON numbers; ordered partitions; IQR median-of-halves or population Z-Score. See docs/api.md for numerical conventions and stable Problem Details codes."));
  }
}
