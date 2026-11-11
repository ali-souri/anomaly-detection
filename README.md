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

Then open **http://localhost:8080/swagger-ui/index.html** and **http://localhost:8080/v3/api-docs**.
Both the dev profile **and** `SWAGGER_ENABLED=true` are required. The default HTTP port is 8080;
override with `PORT` or `--server.port=9090` (then use that port in both URLs).

## v0.3 candidate: operational policies

One stateless application still serves both strategies and always returns the full ordered
`anomalies` and `normalValues` arrays. Classification reuses immutable values from its validated
snapshot. Positive per-instance settings:

| Property | Environment | Default |
| --- | --- | ---: |
| `detection.limits.max-active` | `DETECTION_LIMITS_MAX_ACTIVE` | 1 |
| `detection.limits.max-numbers` | `DETECTION_LIMITS_MAX_NUMBERS` | 2,000,000 inclusive |
| `detection.limits.max-body-bytes` | `DETECTION_LIMITS_MAX_BODY_BYTES` | 67,108,864 (64 MiB) inclusive |

The byte setting must also be below Integer.MAX_VALUE. Admission happens before JSON conversion,
has no waiting queue and retains a permit through synchronous response serialization. A full slot
returns **503 ADMISSION_FULL**, `Retry-After: 1` second. Count/body limits return **413 INPUT_TOO_LARGE**;
unknown/chunked lengths are bounded too. These deliberately replace historically unlimited input
with provisional demo policies; they are not proven safe capacity or a latency SLO. Limits apply
per instance. [API details](docs/api.md), [candidate release record](docs/releases/v0.3.md).

## Containers and capacity

```powershell
docker compose up --build -d --wait
curl.exe http://localhost:8080/actuator/health/readiness
docker compose down
.\scripts\build-v03.ps1
.\scripts\benchmark-v03.ps1 -Implementation baseline
.\scripts\benchmark-v03.ps1 -Implementation candidate
.\scripts\api-v03.ps1 -Implementation baseline
.\scripts\api-v03.ps1 -Implementation candidate
```

The multi-stage build runs clean verify. Java 17 source runs on Temurin Java 21, matching the
native measurement major runtime. Compose defaults to one 4-CPU/3-GiB container and a 2-GiB G1 heap,
with readiness checks and graceful shutdown. `HTTP_PORT` overrides the host port; `APP_PROFILE`,
`SWAGGER_ENABLED`, `MAX_ACTIVE`, `MAX_NUMBERS`, `MAX_BODY_BYTES` configure Compose settings.
Use `docker compose -f compose.yaml -f compose.capacity.yaml up -d --scale app=3 --wait` for
the bounded three-copy experiment through nginx; this is horizontal scaling of the same app.

The owner's target is **10 successful complete 2M FULL requests/second total**, separately IQR,
Z_SCORE and 50/50 mixed. No latency SLO was supplied. The declared ceiling is three application
containers (12 CPUs/9 GiB), nginx (0.25 CPU/128 MiB) and k6 (2 CPUs/2 GiB). Read the
[actual load results and run commands](docs/load-tests/README.md) before claiming capacity, and
the [paired measurements](docs/benchmarks/v0.3/report.md) before claiming allocation/speed gains.
The candidate is not a published v0.3.0 release unless every required capacity/release gate passes.
Actual outcome: **CAPACITY_TARGET_NOT_MET**. The best tested configuration used three replicas,
active=3 each, 3.75 CPUs/3 GiB/2 GiB heap each and a 1-CPU nginx proxy. Successful counts over
60 seconds were IQR **339 (5.65/s)**, Z_SCORE **418 (6.966667/s)** and mixed **369 (6.15/s)**,
with sustained 503s and no unexpected errors/timeouts/dropped iterations. Completion-window
rates and latency sample counts are in the load report. Work remains on the feature branch;
no v0.3.0 release is published.

Continuation 008 profiles that increment and changes only IQR's temporary sorting workspace.
Matched 2M core means are **657.289 → 201.915 ms/op**; the full deployment still misses ten RPS
(339 IQR / 348 Z_SCORE / 335 mixed successes in separate 60-second runs). Current verification
is **61 unit / 56 HTTP**, LINE296/0 and BRANCH134/0. See [008 profile, measurements and commands](docs/performance/008/README.md).
The 007 evidence above is historical and preserved; no general end-to-end or capacity gain is claimed.

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
- [Roadmap](docs/roadmap.md): current candidate gates and conditional follow-ups.
- [Initial execution result](docs/results/AD-RES-20261110-002.md): preserved v0.1/v0.2 evidence.
- [Maintenance result](docs/results/AD-RES-20261111-005.md): measured coverage, exact v0.2.1 source and CI artifacts.
- [Executed benchmark](docs/benchmarks/v0.2/report.md): measured v0.2.1 core/API results, raw evidence and run commands.
- [v0.3 architecture decision](docs/decisions/v0.3-architecture.md): delivered scope and measured limitations.

All project caches, temporary files, wrapper distributions and outputs stay under `.cache/`
and `target/`, on F: in the canonical workspace. No generated directories belong in Git.
Container images/build cache use the already configured Docker storage; it is not relocated.

Initial work request [AD-PRM-20261110-002](docs/prompts/AD-PRM-20261110-002.txt) supersedes
`AD-PRM-20261110-001`. The result is `AD-RES-20261110-002`; revisions require new IDs and
explicit PARENT/SUPERSEDES links.

Maintenance [AD-PRM-20261111-005](docs/prompts/AD-PRM-20261111-005.txt) has parent 002 and
supersedes none. Its result is AD-RES-20261111-005. The selected benchmark baseline is v0.2.1;
see [continuing project rules](AGENTS.md) before prompt 003.
