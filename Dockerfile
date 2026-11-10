FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY .mvn ./.mvn
COPY config ./config
COPY src ./src
RUN --mount=type=cache,target=/workspace/.cache/repository mkdir -p .cache/tmp && mvn -B clean verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S anomaly && adduser -S -G anomaly anomaly && mkdir /app/tmp && chown anomaly:anomaly /app/tmp
COPY --from=build /workspace/target/anomaly-detection-0.3.0-SNAPSHOT.jar /app/app.jar
USER anomaly
ENV PORT=8080 JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=/app/tmp"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 CMD wget -q -O /dev/null "http://127.0.0.1:${PORT}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
