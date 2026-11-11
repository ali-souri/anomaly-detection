# Final interview / voice-report handoff

Optimization is **closed after 010**, regardless of PARTIAL capacity status. The next activity is
the owner's voice-report discussion. No further performance prompt, grid or optimization is proposed.
No audio was generated and no interviewer was contacted.

## Versions, access and exact identities

| Item | Verified state |
| --- | --- |
| Stable shipped source | [v0.2.1](https://github.com/ali-souri/anomaly-detection/tree/v0.2.1), commit 0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7; annotated tag preserved |
| Current candidate | [feature/v0.3-jvm-tuning](https://github.com/ali-souri/anomaly-detection/tree/feature/v0.3-jvm-tuning); tested feature, **unreleased**, no v0.3.0 |
| Preserved application publication | bc18d635bcf8359794b0f75aaedbf63d57f481fa (009), same application source throughout010 |
| 009 exact-code CI | [ebf432f / run42958128656](https://github.com/ali-souri/anomaly-detection/actions/runs/42958128656) |
| 010 runner/configuration and actual CI | 0fc287b648f52f8c269d891ea6ae987a0fa28f53; [run42964647730](https://github.com/ali-souri/anomaly-detection/actions/runs/42964647730), successful; downloaded reports independently checked |
| Executed image / runtime | sha256:e886eab5769e756e3cd5b72d31453be51a9ee5e9ada6b9a215031dd1d3404a4e; Temurin 21.0.11+10, source target 17 |
| Same executed JAR for A/B/C | de69e4871d3b94605bb436aca49ff10dfd42684f110316ed21ad0aab2f082aac |

The later handoff/result commit contains documentation/evidence and retained-baseline Compose
commands; it is not relabeled as timed application code. Resolve its exact publication using
`git log -1 --format=%H -- docs/results/AD-RES-20261111-010.md`.
The repository is **PRIVATE**, verified through the owner's connected account. Links require
existing repository permission. These are source URLs; no public production API is hosted or
claimed. Access information is for owner review only; visibility, invitations and reviewer contact
were not changed. Stable release readiness and unreleased candidate capacity are distinct.

Current candidate: **67 unit /101 real random-port HTTP tests**, zero failures/errors/skips;
LINE364/0, BRANCH172/0, INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0.
Thirty handwritten production Java files,32XML entries,29executable classes; no handwritten
exclusions or reduced/fake coverage guards. Separate harness 9, Node 2 and Python16 checks pass.
Strict JavaDoc, formatting/lint, fork agents/tmpdirs, pre-commit and pre-push clean verify remain.
Coverage exercises code; it does not prove correctness or production readiness.

## Architecture and short reading order

One synchronous, stateless modular Spring application. The statistical core has no Spring/Jackson
imports. Strategy/registry and constructor injection keep algorithm selection separate from HTTP.
The [actual class diagram](../architecture.md) remains unchanged by010.

1. [AnomalyDetector](../../src/main/java/io/anomaly/core/algorithm/AnomalyDetector.java),
   [IqrDetector](../../src/main/java/io/anomaly/core/algorithm/IqrDetector.java),
   [ZScoreDetector](../../src/main/java/io/anomaly/core/algorithm/ZScoreDetector.java).
2. AlgorithmRegistry and DetectionService: exact identifiers and defaults; Inputs, DetectionSummary
   and typed results: validation, immutable snapshots and ordered partitions.
3. RequestParser, BoundedRequestBodyAdvice and DetectionAdmissionFilter: strict tokens, bounded
   bytes/counts and immediate admission retained through synchronous response writing.
4. DetectionController → ResponseMapper; ProblemHandler; DetectionConfiguration / RequestLimits.
5. Matching core tests, integration/api tests, [test chronology](../tdd/11-direct-json.md),
   verification workflow and [final runtime evidence](../performance/010/README.md).

IQR uses median-of-halves quartiles (excluding the central value for odd n), default multiplier1.5,
strict outside bounds, O(n log n) time/O(n) scratch. Z_SCORE uses Welford population variance
(divisor n), default3, O(n) time. Both preserve order/duplicates, defensive immutable snapshots
and full eager results. Empty data succeeds; small data warns; zero variance returns all normal
with a warning. Unsafe computed overflow/underflow returns422. Signed zero and numeric boundary
policies remain tested.

HTTP rejects coercive strings/nulls/booleans, wrong shapes and unknown fields. Missing threshold
selects default; explicit null fails. Canonical algorithms, syntax/semantic error categories,
last-wins duplicate and first-root/trailing compatibility are retained. Defaults per instance:
active1, inclusive2M numbers and64MiB raw bytes. Unknown/chunked bytes are actually bounded;
busy503 includes Retry-After:1, oversize413. No decoded waiting queue. Unexpected500 is safe.
Input lists/raw buffers remain O(n); the byte[] bridge adds a raw copy. Swagger needs dev **plus**
SWAGGER_ENABLED=true. See [API](../api.md) for exact schemas, fields and error contracts.

## Improvements, comparators and final capacity

| Increment | Own comparator and supported result | Limit |
| --- | --- | --- |
| 007 immutable Double-reference reuse | v0.2.1 /0bc6e1d versus paired331cb861 candidate: IQR39.48%, Z_SCORE45.55% less core B/op on2M MIXED | Timing intervals overlap; no general speed/capacity claim. [Evidence](../benchmarks/v0.3/report.md) |
| 008 primitive IQR sort workspace | Preserved007 /33d4e2f versus0ba3803 candidate: matched core mean657.289→201.915ms/op | Same fixture/local microbenchmark, not whole API; capacity failed. [Evidence](../performance/008/README.md) |
| 009 direct JSON token reader | Contemporary pinned008 versus009: input Java sample share38.82→31.57%; controlled mean735.617→678.609ms | Samples are not phase timers; total allocation sample weight near-unchanged; capacity failed. [Evidence](../performance/009/README.md) |
| 010 heap/G1 settings | Same009 image: A81 requests, B79, C80 in25s three-client mixed screens; means827.2/845.0/843.2ms | Both safe briefly, neither useful; **TUNING_NOT_ACCEPTED**, retainA. No scaled final comparison. [Evidence](../performance/010/README.md) |

Retain G1/Xms512m/Xmx2048m, default200ms soft pause goal and ergonomic1MiB regions. Larger heaps
selected2MiB regions automatically; no manual region/thread tuning. G1 goals are not response-time
limits. The GC audit reproduces009's distinct pause totals; concurrent-cycle elapsed is separate,
and cross-replica sums may overlap. Do not describe them as one request's delay or add JFR GC
cycle duration again. Allocation samples are not exact B/op/live heap.

The last actual open-arrival capacity evidence is009: exact-window successful rates 6.70 IQR,
7.133333 Z_SCORE and6.966667 mixed, sustained503s, no unexpected/timeouts/drops. **Ten RPS did not
pass.** 010 capacity is **NOT_EXECUTED** because neither screen qualified; it supplies no new
capacity rate. No owner latency SLO exists. One short local screen/pair does not prove endurance,
remote networking, safe maximum-heap occupancy or universal tail latency.

Retained experimental budget: three apps ×3.75CPU/3GiB/2GiB heap/active3; nginx1CPU/128MiB;
k6 2CPU/2GiB/GOMAXPROCS2/max20VUs =14.25CPU/11.125GiB. This is three-replica aggregate throughput,
not one-instance capacity. Single-instance demo defaults remain separate. Microservices were
 deferred because distributing global statistics does not remove sort/JSON/GC costs and adds
transport/operations complexity. Brokers/persistence/auth need requirements absent from this task.

TDD chronology: genuine algorithm/HTTP/limit/admission REDs are documented; representation changes
008/009 first characterized existing behavior GREEN, without fabricated RED. Actual constraint/
charset regressions found during009 were corrected. Runtime010 adds no functional behavior and
claims no functional RED. Frozen tags/history and accepted evidence remain intact.

## Short commands for the owner

```powershell
./mvnw.cmd clean verify
Start-Process ./target/site/jacoco/index.html
./mvnw.cmd javadoc:javadoc
./mvnw.cmd spring-boot:run
# In a separate invocation, enable interactive docs instead:
$env:SWAGGER_ENABLED='true'
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

API: `POST http://localhost:8080/api/v1/anomaly-detection`.
[Swagger UI](http://localhost:8080/swagger-ui/index.html) · [OpenAPI JSON](http://localhost:8080/v3/api-docs).
The flag alone outside dev does not enable Swagger. There is no authentication in these releases.

Retained capacity configuration, requiring the already cached pinned image, without rebuild/pull:

```powershell
docker compose -p anomaly-review -f compose.yaml -f compose.capacity.yaml -f compose.010.yaml -f compose.010.capacity.yaml up -d --no-build --pull never --scale app=3 --wait
./scripts/run010-load.ps1 -Run owner-review-IQR -Algorithm IQR -Duration 60s -BaseUrl http://proxy:8080 -Network anomaly-review_default
docker compose -p anomaly-review -f compose.yaml -f compose.capacity.yaml -f compose.010.yaml -f compose.010.capacity.yaml down
```

Use a new run identity for any owner-initiated reproduction; do not overwrite accepted evidence.
These are reproduction instructions, not authorization to resume optimization automatically.
The [Persian script](voice-script-fa.md) is a coherent outline for roughly 4–5 minutes of the owner's
own speech. No dense SHAs need reading aloud. Review the diagrams/tests/result first; the next
conversation is the voice report.

[Issued010 result](../results/AD-RES-20261111-010.md) records the independent experiment, tuning,
capacity and voice-handoff statuses. Optimization remains closed.
