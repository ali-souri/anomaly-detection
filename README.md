# Anomaly detection

A small Java/Spring Boot application with an independent statistical core. It classifies a
numeric sequence using IQR or population Z-Score while retaining duplicates and original order. Read
[the architecture and class diagram](docs/architecture.md) first, then the detectors and tests.

## Quick start

Prerequisites: Java 17+ and Git; first use needs access to Maven Central. Maven is supplied by
the checksum-pinned wrapper. The verified workstation uses Java 21.0.10; source targets Java 17.

Windows PowerShell:

```powershell
./scripts/install-hooks.ps1
./mvnw.cmd verify
./mvnw.cmd spring-boot:run
```

Unix:

```sh
bash scripts/install-hooks.sh
./mvnw verify
./mvnw spring-boot:run
```

```sh
curl -X POST http://localhost:8080/api/v1/anomaly-detection \
  -H 'Content-Type: application/json' \
  -d '{"algorithm":"IQR","data":[10,12,11,13,12,14,11,13,12,100]}'
```

In PowerShell, use `curl.exe` and a JSON file to avoid native argument quoting differences:

```powershell
'{"algorithm":"IQR","data":[10,12,11,13,12,14,11,13,12,100]}' | Set-Content -Encoding utf8 request.json
curl.exe -X POST http://localhost:8080/api/v1/anomaly-detection -H 'Content-Type: application/json' --data-binary '@request.json'
```

Expect Q1=11, Q3=13, IQR=2, bounds [8,16], anomaly [100]. Empty data succeeds with `NO_DATA`;
one to three values succeed with `SMALL_DATASET`.

Z-Score uses population standard deviation (variance divisor n), default threshold 3.0. For a
hand-calculated example, override it to 1.5:

```sh
curl -X POST http://localhost:8080/api/v1/anomaly-detection \
  -H 'Content-Type: application/json' \
  -d '{"algorithm":"Z_SCORE","threshold":1.5,"data":[2,4,4,4,5,5,7,9]}'
```

This gives mean=5, standardDeviation=2, anomalies=[9]. Z-Score and IQR may disagree: the original
ten-value IQR example has no Z-Score anomaly at threshold 3.0. Zero variance succeeds with all
values normal and `ZERO_VARIANCE`.

Swagger UI and OpenAPI JSON are disabled by default. Enable both only with the dev profile
and explicit flag:

```powershell
$env:SWAGGER_ENABLED='true'
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

```sh
SWAGGER_ENABLED=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Then open `/swagger-ui/index.html` or `/v3/api-docs`. The flag alone outside dev does not enable them.

## Reading and verification

- [API contract](docs/api.md): exact JSON shapes, defaults, numerical policy and Problem Details.
- [Architecture + Mermaid class diagram](docs/architecture.md): actual layers, Strategy and complexity.
- [Development](docs/development.md): TDD, Git flow, hooks, formatting and commands.
- [TDD evidence](docs/tdd/): actual RED/GREEN runs; `verify` runs unit and real HTTP integration suites separately.
- Generated JavaDoc: `target/reports/apidocs/index.html` after `verify` (strict doclint).
- [v0.1 release](docs/releases/v0.1.md): `release/v0.1`, annotated tag `v0.1.0`.
- Corrected v0.1 patch: annotated tag `v0.1.1` closes direct Swagger static-resource access; v0.1.0 is preserved.
- [v0.2 release](docs/releases/v0.2.md): `release/v0.2`, annotated tag `v0.2.0` (current application).
- [Roadmap](docs/roadmap.md): benchmark v0.2 before deciding v0.3.

All project caches, temporary files, wrapper distributions and outputs stay under `.cache/`
and `target/`, on F: in the canonical workspace. No generated directories belong in Git.

Work request [AD-PRM-20261110-002](docs/prompts/AD-PRM-20261110-002.txt) supersedes
`AD-PRM-20261110-001`. The result is `AD-RES-20261110-002`; revisions require new IDs and
explicit PARENT/SUPERSEDES links.
