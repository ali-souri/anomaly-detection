# 008 — profile-led IQR scratch-workspace optimization

**PARTIAL: PROFILING_COMPLETE / OPTIMIZATION_VERIFIED / CAPACITY_TARGET_NOT_MET.**
The starting comparator is the verified 007 increment at
33d4e2f3936723cbc4ec28cd03e4428944cecc60, not historical v0.2.1. The sole production change
is [IqrDetector](../../../src/main/java/io/anomaly/core/algorithm/IqrDetector.java): a local double[]
scratch copy replaces the boxed sort workspace. Original Double references still populate eager,
immutable, ordered FULL partitions. No parsing, statistical convention, model, admission, output,
threshold, resource ceiling or service boundary changed. Candidate source: 0ba38032861ffa36f69cf5caf434abb471665161.

## Actual profiles before selecting the change

Six unchanged-007 container diagnostics: IQR/Z_SCORE × 1/2/3 active requests, one instance at
3.75 CPUs / 3 GiB / 2 GiB G1 heap. k6 retains its 2 CPU/2 GiB cap; controlled clients equal the
active limit, 100 ms think time, 30 seconds after two serial FULL warmups. Each JFR recording is
**15 seconds**, settings=profile, delay=20s, maxsize=128m, recording/export stack depth=128.
Recorded sample timestamps overlap warmed traffic; exact offsets, settings, file sizes/hashes,
cgroup deltas and memory are in [profile-overview.json](raw/profile-overview.json).

JDK Flight Recorder runs in Java21.0.11 Linux containers. Host JDK21.0.10 jfr exports the recordings.
Recordings stay ignored in .cache/008/profiles on F:. Verbose JSON exports are streamed one event
at a time with a bounded buffer; they are not loaded wholesale or committed. Compact extracted
stacks and recording hashes are retained. The initial numeric recording-name startup failure
captured no profile and is preserved separately. A project-local optional JSON-parser installation
failed; the delivered analysis uses only Python's standard library and added no global dependency.

ExecutionSample and NativeMethodSample are **separate populations**. Exclusive grouping assigns
the nearest recognized frame from the leaf; inclusive grouping counts every matched ancestor and
can overlap. Transport's inclusive Tomcat-worker ancestry appears in nearly every request, so it
does not establish transport CPU dominance. Native samples mostly contain EPoll.wait/Net.accept,
with some socket reads/writes; waiting native samples are not added to Java CPU samples. Unknown
work remains unclassified. Sampling percentages estimate stack frequency, not wall-clock phases.
Allocation weights are sampled estimates, not exact object counts, B/op or live heap.

IQR boxed sorting accounts for **42.73–45.96%** of Java samples across all three controlled
concurrencies. Leaf evidence includes Double.compare and ComparableTimSort.mergeLo/mergeHi.
Z_SCORE instead assigns about 32–34% to JSON tree creation, 26–28% to serialization and 14–15%
to numeric-list conversion. Raw-body copies, detector validation/copy, result construction,
statistics/classification, native/transport and unclassified shares remain in each stack JSON and
the complete phase CSV. Low raw-buffer sample frequency does not imply free allocation/copying.

The [declared hypothesis](hypothesis.md) selected only primitive IQR scratch sorting and predicted
at least a 20% lower paired core mean. Signed zero, median indices, overflow/subnormal arithmetic,
original ordering/duplicates and immutable snapshots are preserved. The new two unit and one real
HTTP regressions ran GREEN against old production before the refactor; no functional RED is
invented for an intentionally unchanged representation contract. [TDD ordering](../../tdd/10-profile-workspace.md).

## Matched unprofiled measurements

Explicit 007 and candidate JARs in separate native Java21.0.10 JVMs; Xms512m/Xmx2g G1, one thread,
2M MIXED seed20261110, JMH1.37 GC profiler. Two forks, **3×1s warmup and 3×1s measurements/fork**,
six iteration means/case. Full validation/copy/sort/statistics/eager partitions remain timed.
All 24 analytical/order/nonmutation fixture preflights pass for both artifacts. No agent, active
JFR recording, build or concurrent load ran alongside final measurements. Actual class origins,
JAR hashes, source/harness commits and arguments are preserved in raw identities and origin logs.

| Algorithm | 007 ms/op ± reported 99.9% CI | Candidate ms/op ± CI | 007 / candidate B/op |
| --- | ---: | ---: | ---: |
| IQR | 657.289 ± 193.413 | 201.915 ± 51.454 | 73,586,136 / 73,389,950.044 |
| Z_SCORE control | 206.996 ± 26.685 | 214.633 ± 87.757 | 57,389,887.644 / 57,389,958.644 |

The IQR point mean is **69.28% lower**, with non-overlapping reported intervals. Allocation falls
only about **0.267%**: this is primarily a sort CPU improvement and preserves the earlier 007
Double-reference allocation benefit. Z_SCORE code is unchanged and its intervals overlap.
This short discrete-histogram experiment does not prove the gain for every data distribution.

Native FULL API medians over three serial samples: IQR **935.488 → 645.518 ms**, Z_SCORE
**522.712 → 451.903 ms**. All twelve responses were received fully and every value/order/count/
statistic/field was streaming-validated outside timing. These are exploratory observations, not
established universal end-to-end gains or p99. Raw rows/process memory/GC snapshots remain separate
from allocation B/op. Complete container output/recovery checks also passed before and after load.

## Fixed-budget target outcome

Three replicas, **3.75 CPUs / 3 GiB container / 2 GiB heap / active=3 each**; nginx 1 CPU/128 MiB;
k6 2 CPUs/2 GiB, GOMAXPROCS=2, max20 VUs. Total ceiling **14.25 CPUs / 11.125 GiB**, unchanged.
Demo defaults remain active=1, inclusive 2M numbers and 64 MiB body. No compression, smaller output,
extra replica/resources or lower target was used. Current host availability is recorded; a scoped
sampler preserves at least 1.5 GiB host headroom. Other projects/services are untouched.

All target runs use unprofiled Java21.0.11 Linux containers through the real nginx entry point,
after warmup, at **10 total open arrivals/second for 60 seconds**, separately IQR/Z_SCORE/mixed.
Every successful request receives the complete 40k anomalies and 1.96M normal values. Most bodies
are discarded only after full reception; every 101st scheduled result receives semantic/count/
statistics checks. Exhaustive original order/value checks occur outside timing through ApiProbe.
Fixtures are generated once and binary request buffers reused. Body bytes remain IQR9,900,028,
Z_SCORE9,900,032 and response9,900,170. k6 traffic metrics include warmups/protocol/partial rejected
uploads; successful payload bytes are recorded separately. Ten successes/second requires about
99 MB/s body payload in each direction before overhead.

| Workload | Scheduled / success / 503 | Success count ÷ 60s | Exact-window successful completion/s |
| --- | ---: | ---: | ---: |
| IQR | 601 / 339 / 262 | 5.650000 | 5.500000 |
| Z_SCORE | 600 / 348 / 252 | 5.800000 | 5.683333 |
| 50/50 mixed | 601 / 335 / 266 | 5.583333 | 5.433333 |

Zero unexpected errors, timeouts and dropped iterations, but sustained 503s: **all cases fail**.
Fast rejections never enter successful latency. Finite boundary starts and drain are not rounded
into a pass. Timestamps, ten-second bins/backlog, actual bytes and resource observations are saved.
007's historical counts were 339/418/369; host state/time differs, and the 008 unchanged Z_SCORE
control is slower than that historical run. No controlled deployment-capacity gain is established.

| Workload | Successful n | Average ms | p50 | p95 | empirical p99 | Drain ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| IQR | 339 | 1526.245 | 1519 | 1788.200 | 1915.120 | 992 |
| Z_SCORE | 348 | 1465.144 | 1464 | 1711.600 | 1871.530 | 786 |
| Mixed | 335 | 1534.501 | 1516 | 1866.300 | 2131.960 | 853 |

No owner latency SLO exists. These finite empirical percentiles are not production tail guarantees.
No lower zero-rejection capacity ceiling or long soak was established.

## Representative scaled diagnostic after the change

Separate from the unprofiled target runs, a **35-second diagnostic IQR arrival run** recorded
one of three candidate replicas: profile settings, delay=25s, duration=15s, maxsize=128m/depth128.
It completed 194 successes/156 rejections; its short-run threshold failure is retained and is not
a capacity pass. The profiled pod has **1,211 Java / 413 native samples**: primitive sorting 15.77%,
tree creation26.18%, numeric-list conversion13.71%, serialization25.85%, statistics/classification
11.48%, result3.06%, validation/copy1.73%, transport1.98%, raw-buffer0.17%, unclassified0.08%.
This is a scaled post-change observation, not a matched single-instance percentage comparison.
It supports the next experiment: a narrowly bounded direct request parser to reduce tree/list
materialization, with the complete strict HTTP/type/order/limit/error contract tested first.
Serialization remains material too. Do not promise that parsing, more permits or microservices
will automatically meet ten RPS. No second production optimization was attempted.

## Reproduce and verification

```powershell
.\scripts\profile008.ps1
python -B scripts/analyze008.py
python -B scripts/report008.py
.\scripts\benchmark008.ps1 -Implementation baseline
.\scripts\benchmark008.ps1 -Implementation candidate
.\scripts\api008.ps1 -Implementation baseline
.\scripts\api008.ps1 -Implementation candidate
$env:HTTP_PORT='18080'; $env:MAX_ACTIVE='3'; $env:APP_CPUS='3.75'; $env:PROXY_CPUS='1'
$env:PERFORMANCE_IMAGE='anomaly-detection:008'
docker compose -p anomaly-008 -f compose.yaml -f compose.capacity.yaml -f compose.008.yaml up -d --scale app=3 --wait
.\scripts\run008-load.ps1 -Run fresh-IQR -Algorithm IQR -BaseUrl http://proxy:8080 -Network anomaly-008_default
docker compose -p anomaly-008 -f compose.yaml -f compose.capacity.yaml -f compose.008.yaml down
```

Fresh run labels/artifact directories are required; never overwrite accepted 007/008 evidence.
See [result008](../../results/AD-RES-20261111-008.md) for exact publication/CI/artifact identities.
All 61 unit/56 real HTTP integration tests pass, exact LINE296/0 and BRANCH134/0, no handwritten
exclusions; strict formatting/lint/JavaDoc and hooks remain. Separate harness9, Node2, prior Python4,
JFR analysis3 and cgroup-counter1 checks pass; root JaCoCo does not cover these separate tools.
Container build executed clean verify. All task-owned containers are stopped after diagnostics.
Longer warmups/matrices, independent generator, remote network, exact phase wall times and
production endurance remain outside this bounded experiment.

Actual GC/CPU/memory observations (CPU deltas span the whole diagnostic load call including warmup/drain; heap is observed at GC events, cgroup peak spans container lifetime):

| Run | GC count | Sum observed GC pauses ms | CPU usage delta s | Throttled periods / periods | GC-observed heap max MiB | Cgroup peak MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| before-IQR-active1 | 69 | 3736.509 | 58.701 | 11 / 346 | 918.976 | 1202.938 |
| before-IQR-active2 | 71 | 4916.647 | 88.376 | 55 / 345 | 1651.280 | 1954.664 |
| before-IQR-active3 | 75 | 5965.611 | 112.425 | 168 / 349 | 1974.476 | 2209.934 |
| before-Z_SCORE-active1 | 91 | 4188.322 | 59.952 | 13 / 336 | 943.976 | 1534.820 |
| before-Z_SCORE-active2 | 98 | 6000.884 | 91.903 | 45 / 343 | 1372.476 | 1726.551 |
| before-Z_SCORE-active3 | 98 | 7086.696 | 111.101 | 187 / 352 | 1632.976 | 1987.094 |
| diagnostic-scaled-IQR | 61 | 6533.057 | 131.212 | 258 / 392 | 2047.476 | 2234.488 |

GC pauses can overlap other threads and sampled allocation weights are not heap usage. Native API working-set/post-heap measurements are in the separate api-*-process.json / heap files; no B/op-to-live-heap conversion is claimed.
