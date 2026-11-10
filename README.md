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
./mvnw.cmd clean verify
./mvnw.cmd spring-boot:run
```

Unix:

```sh
bash scripts/install-hooks.sh
./mvnw clean verify
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

## Testing and Coverage

Unit tests exercise small contracts; `*IT` tests start real random-port HTTP servers. The release
gate combines both suites and enforces **100% LINE and 100% BRANCH** coverage for all handwritten
production code. No handwritten exclusions are allowed. Missing coverage data or any failed check
returns a nonzero exit code. Full coverage does not prove the absence of defects.

Windows PowerShell:

```powershell
.\mvnw.cmd test                                      # unit tests only
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify  # integration only; no Surefire
.\mvnw.cmd clean verify                              # full combined release gate
Start-Process .\target\site\jacoco\index.html           # fresh combined HTML report
.\mvnw.cmd javadoc:javadoc                            # explicit JavaDoc
.\mvnw.cmd spotless:apply                             # review changes, then stage yourself
```

Unix:

```sh
./mvnw test
./mvnw test-compile failsafe:integration-test failsafe:verify
./mvnw clean verify
./mvnw javadoc:javadoc
./mvnw spotless:apply
# Optional desktop report opener: macOS uses open; Linux desktops often use xdg-open.
# open target/site/jacoco/index.html
# xdg-open target/site/jacoco/index.html
```

Unit-only, integration-only and selected runs are conveniences, not the combined 100% gate.
Run **clean verify** before interpreting coverage. Reports: `target/surefire-reports`,
`target/failsafe-reports`, `target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml`
(also CSV), and `target/reports/apidocs/index.html`. See [coverage details](docs/coverage.md)
for instrumentation, exact counters, filtering and failure-fixture evidence.

## Reading and verification

- [API contract](docs/api.md): exact JSON shapes, defaults, numerical policy and Problem Details.
- [Architecture + Mermaid class diagram](docs/architecture.md): actual layers, Strategy and complexity.
- [Development](docs/development.md): TDD, Git flow, hooks, formatting and commands.
- [TDD evidence](docs/tdd/): actual RED/GREEN runs; `verify` runs unit and real HTTP integration suites separately.
- Generated JavaDoc: `target/reports/apidocs/index.html` after `verify` (strict doclint).
- [v0.1 release](docs/releases/v0.1.md): `release/v0.1`, annotated tag `v0.1.0`.
- Corrected v0.1 patch: annotated tag `v0.1.1` closes direct Swagger static-resource access; v0.1.0 is preserved.
- [v0.2 release](docs/releases/v0.2.md): frozen `release/v0.2`, annotated tag `v0.2.0`.
- [v0.2.1 maintenance](docs/releases/v0.2.1.md): io.anomaly packages, exact coverage gate and benchmark baseline.
- [Roadmap](docs/roadmap.md): benchmark v0.2 before deciding v0.3.
- [Initial execution result](docs/results/AD-RES-20261110-002.md): preserved v0.1/v0.2 evidence.
- [Maintenance result](docs/results/AD-RES-20261111-005.md): measured coverage, exact v0.2.1 source and CI artifacts.
- [Executed benchmark](docs/benchmarks/v0.2/report.md): measured v0.2.1 core/API results, raw evidence and run commands.
- [v0.3 recommendation](docs/decisions/v0.3-architecture.md): minimum complexity supported by those measurements.

All project caches, temporary files, wrapper distributions and outputs stay under `.cache/`
and `target/`, on F: in the canonical workspace. No generated directories belong in Git.

Initial work request [AD-PRM-20261110-002](docs/prompts/AD-PRM-20261110-002.txt) supersedes
`AD-PRM-20261110-001`. The result is `AD-RES-20261110-002`; revisions require new IDs and
explicit PARENT/SUPERSEDES links.

Maintenance [AD-PRM-20261111-005](docs/prompts/AD-PRM-20261111-005.txt) has parent 002 and
supersedes none. Its result is AD-RES-20261111-005. The selected benchmark baseline is v0.2.1;
see [continuing project rules](AGENTS.md) before prompt 003.
