# Reproducible local load experiment 007

Status and actual numeric results are published below and in
[result 007](../results/AD-RES-20261111-007.md). The owner requires **10 successful complete
2,000,000-number FULL requests/second total** separately for IQR, Z_SCORE and 50/50 mixed.
HTTP 503/413 never count as success. No latency SLO was supplied.

## Environment and fixed ceiling

Windows 11 / i7-12700K / 20 logical CPUs, 31.69 GiB usable host RAM. At declaration about
15.93 GiB was free. Before scaling about 12.78 GiB was free. Docker Desktop Linux 29.6.2
has 20 logical CPUs and 15.46 GiB configured RAM; six unrelated containers were left untouched.
Existing Docker storage was used without relocation or a new project data root.

Maximum: **3 × (4 CPUs / 3 GiB container / 2 GiB heap)**, nginx **0.25 CPU / 128 MiB**,
official **k6 1.8.2 (2 CPUs / 2 GiB, GOMAXPROCS=2, 20 VUs maximum)**.
Total ceiling 14.25 CPUs / 11.125 GiB, with headroom for existing services, OS and native memory.
App Linux runtime: Temurin **21.0.11+10**, G1, Xms512m/Xmx2g, Java 17 source.
This differs from native paired JMH/API Java 21.0.10; container timings are separate evidence.
No agent is active in timed application/generator processes. All generated fixtures/logs remain on F:.

Demo: 1 replica, active=1, inclusive numbers=2M and body=64 MiB. Tuned single: active=2.
Three-active concurrency was measured first (26 successes at three clients, zero rejection/error;
100 ms think time): CPU around 3.7 units, cgroup peak **2,053,492,736 bytes**, no OOM events.
Scaled experiment: **3 replicas, active=3 each**, same CPU/heap/memory and size policies, nginx entry
point. This is horizontal scaling of one application, not per-algorithm services. Per-instance
admission is not a global nine-request cluster semaphore; load balancing still affects placement.
The 3 GiB cap includes native/thread/code memory; a 2 GiB heap is not a 2 GiB process.

The quarter-CPU proxy showed sustained quota use and throttling. A budget-neutral follow-up uses
**3 × 3.75 app CPUs + 1 nginx CPU + 2 generator CPUs = the same 14.25 CPU ceiling**; active=3,
heap/memory/replica limits remain unchanged. Effective settings are in rebalanced-compose.yaml.
Reproduce with APP_CPUS=3.75 and PROXY_CPUS=1 before the scaled Compose command.

## Commands

```powershell
.\scripts\build-v03.ps1                 # separate app/harness gates; generates ignored fixtures
node --test load-tests/contract.test.mjs
python -B scripts/test_summarize_load.py
docker compose up --build -d --wait
.\scripts\api-v03.ps1 -Implementation candidate -Run container-check -BaseUrl http://localhost:8080
.\scripts\run-load.ps1 -Run demo -Mode clients -Size 10000 -Clients 2 -Duration 10s
.\scripts\run-load.ps1 -Run demo -Mode clients -Clients 1 -Duration 10s
.\scripts\run-load.ps1 -Run demo -Mode clients -Clients 2 -Duration 10s
.\scripts\run-load.ps1 -Run demo -Mode clients -Clients 4 -Duration 10s
.\scripts\run-load.ps1 -Run demo -Algorithm IQR -Duration 60s
.\scripts\run-load.ps1 -Run demo -Algorithm Z_SCORE -Duration 60s
.\scripts\run-load.ps1 -Run demo -Algorithm MIXED -Duration 60s
```

Each run name must be fresh to preserve published evidence. Actual workstation host port was 18080
(`$env:HTTP_PORT='18080'`); the container/network port remains 8080. Reproduction defaults to 8080.
The script connects k6 to the Compose network. It pins the official image; digest is in raw evidence.

```powershell
$env:MAX_ACTIVE='3'
docker compose -f compose.yaml -f compose.capacity.yaml up -d --scale app=3 --wait
.\scripts\api-v03.ps1 -Implementation candidate -Run scaled-check -BaseUrl http://localhost:8080
.\scripts\run-load.ps1 -Run scaled -Algorithm IQR -BaseUrl http://proxy:8080 -Duration 60s
.\scripts\run-load.ps1 -Run scaled -Algorithm Z_SCORE -BaseUrl http://proxy:8080 -Duration 60s
.\scripts\run-load.ps1 -Run scaled -Algorithm MIXED -BaseUrl http://proxy:8080 -Duration 60s
docker run --rm --network anomaly-v03_default --mount "type=bind,source=$PWD/load-tests,target=/suite,readonly" grafana/k6:1.8.2 run /suite/validation.js
python -B scripts/summarize_load.py
docker compose -f compose.yaml -f compose.capacity.yaml down
```

For scaled validation set BASE_URL=http://proxy:8080. `sample-load-resources.ps1` samples only
existing project app/proxy/generator containers. Compose config snapshots record every effective
setting. k6 metrics/huge fixtures/bodies/full logs stay ignored; compact summaries, CSV timestamps,
resource/GC observations and selected correctness evidence are published under raw/.

## Measurement semantics

The arrival executor schedules ten starts/second for **60 seconds** after two serial FULL warmups,
with no retries, maximum 20 in flight and 30-second graceful drain. A closed 1/2/4-client run has
**100 ms think time** and answers a different concurrency/overload question; it cannot verify ten
RPS. Initial unpaced failed-overload runs are retained separately and are not capacity results.

Both algorithms use the same 20261110-seeded MIXED fixture. Request bodies are generated once,
opened outside per-request timing and sent unchanged; no huge input arrays are regenerated or
JSON-parsed by the generator per iteration. Responses are always fully received. Most are discarded
after reception to save generator memory; every 101st scheduled request retains/parses the body
for full partition count/value-membership and
independent statistic checks. Detailed original-order checks run outside timing through ApiProbe
on all six selected FULL responses per deployment entry point. These checks are different scopes.

Successful latency is measured from HTTP send to complete reception, before sampled JSON checks;
generator checks/think time still occupy VUs and affect possible scheduling. Only validated 200s
enter success counters/latencies. Empirical average/p50/p95/p99 include sample counts; these short
runs do not establish stable production p99 or acceptable tail latency.

IQR request body **9,900,028 bytes**, Z_SCORE **9,900,032**; full response **9,900,170 bytes** each.
Selected during-run bodies confirm actual received length. k6 data_sent/data_received report actual
HTTP traffic including warmups/headers and any rejected partial uploads; these totals are not
misrepresented as successful payload only. Ten successes/second require approximately **99 MB/s
each direction**, before overhead. The bounded generator shares the host; CPU/GC/bridge bandwidth
and sampled JSON validation can limit it. Resource snapshots and dropped iterations expose limits.

CSV started/completed epoch-ms timestamps establish exact completion windows and drain. k6 can
schedule a 601st request at the 60-second boundary; reports retain it. Successful scheduled count/60
and successful completions inside the exact window/60 are separately reported, along with ten-second
completion bins and in-flight counts. An all-scheduled-success flag is only eligibility for review:
it does not replace assessing growing backlog or rounded finite-window throughput.

## Validation and limitations

Actual container validation checked malformed JSON 400, numeric strings 400, numerical 422,
2,000,001 numbers 413 and subsequent successful recovery: five checks passed. Native random-port
HTTP additionally tests exact byte/count boundaries, unknown/chunked lengths, malformed/numerical/
application-failure recovery, strict schemas/empty/small results and dev-only Swagger isolation.
The occupied-slot test uses latches, includes a 10 MB rejected body and tests Retry-After.

The first unpaced two/four-client overload exposed connection-reset errors. A bounded Tomcat
discard allowance corrected the transport behavior; corrected checks had zero unexpected errors.
Closed policy checks overlapped some local quality work and do not establish capacity. Actual
60-second arrival runs ran without local builds/JMH. CI ran remotely. Cgroup memory includes more
than heap and GC logs observe collection points, not peak live per-request memory.

Initial scaled generators aborted with exit 137 under the 2 GiB cap while many sampled 2M
responses were parsed concurrently; memory exhaustion is the source-supported inference, not
a retained OOMKilled inspection (containers used --rm). No 60-second capacity result is claimed
from those attempts. One subsequent run was deliberately stopped because its identity commit
was blocked by formatting; its incomplete counter/timestamp mismatch is explicitly rejected by
the summarizer. Full aborted logs remain ignored. The committed correction reuses binary request
buffers and samples every 101st scheduled result. Completed runs keep the same 2 CPU/2 GiB cap
and all full server output; no extra RAM or reduced API shape was used.

Unexecuted: long stress/soak, remote independent generator, production network/quotas, broad random
continuous-value fixtures, profiler phase attribution and owner-approved latency SLO validation.
No paid infrastructure, broker, storage, gRPC, auth or independent algorithm service was added.

## Executed results: CAPACITY_TARGET_NOT_MET

Implementation and bounded Docker/HTTP/load verification succeeded; the owner throughput gate failed. **No case reached ten successful requests/second.** No v0.3 release/tag was created. These are observed accepted rates under offered ten-RPS load, not proven zero-rejection capacity ceilings.

| Configuration | Workload | Scheduled / successful / 503 | Success count / 60 s | Completed inside window / s |
| --- | --- | ---: | ---: | ---: |
| 1 app / active 1 | IQR | 601 / 84 / 517 | 1.400000 | 1.383333 |
| 1 app / active 1 | Z_SCORE | 601 / 120 / 481 | 2.000000 | 1.983333 |
| 1 app / active 1 | MIXED | 601 / 100 / 501 | 1.666667 | 1.650000 |
| 1 app / active 2 (IQR only) | IQR | 600 / 134 / 466 | 2.233333 | 2.216667 |
| 3 apps / active 3 / proxy 0.25 CPU | IQR | 601 / 277 / 324 | 4.616667 | 4.466667 |
| 3 apps / active 3 / proxy 0.25 CPU | Z_SCORE | 601 / 318 / 283 | 5.300000 | 5.133333 |
| 3 apps / active 3 / proxy 0.25 CPU | MIXED | 601 / 307 / 294 | 5.116667 | 4.966667 |
| 3 apps / active 3 / proxy 1 CPU | IQR | 600 / 339 / 261 | 5.650000 | 5.516667 |
| 3 apps / active 3 / proxy 1 CPU | Z_SCORE | 601 / 418 / 183 | 6.966667 | 6.833333 |
| 3 apps / active 3 / proxy 1 CPU | MIXED | 601 / 369 / 232 | 6.150000 | 6.000000 |

All ten completed 60-second arrival scenarios have **zero unexpected errors, timeouts and dropped iterations**, with sustained 503 rejection. Every scenario therefore fails the success target. Tuned-single Z_SCORE/mixed at active=2 were not run; scaling followed the IQR bottleneck.

Successful latency only; empirical milliseconds, n shown explicitly:

| Configuration | Workload | n | Average | p50 | p95 | p99 | Drain after window ms |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 app / active 1 | IQR | 84 | 667.881 | 654.000 | 763.850 | 816.700 | 232 |
| 1 app / active 1 | Z_SCORE | 120 | 457.808 | 445.000 | 566.150 | 626.580 | 445 |
| 1 app / active 1 | MIXED | 100 | 555.040 | 555.000 | 732.150 | 808.050 | 422 |
| 1 app / active 2 (IQR only) | IQR | 134 | 849.030 | 853.500 | 1049.350 | 1117.350 | 185 |
| 3 apps / active 3 / proxy 0.25 CPU | IQR | 277 | 2032.693 | 2050.000 | 2851.200 | 3191.240 | 1656 |
| 3 apps / active 3 / proxy 0.25 CPU | Z_SCORE | 318 | 1771.302 | 1796.500 | 2513.150 | 2784.510 | 1289 |
| 3 apps / active 3 / proxy 0.25 CPU | MIXED | 307 | 1807.332 | 1808.000 | 2770.900 | 3012.940 | 602 |
| 3 apps / active 3 / proxy 1 CPU | IQR | 339 | 1504.799 | 1481.000 | 1834.200 | 2143.080 | 842 |
| 3 apps / active 3 / proxy 1 CPU | Z_SCORE | 418 | 1201.957 | 1204.000 | 1453.000 | 1535.830 | 784 |
| 3 apps / active 3 / proxy 1 CPU | MIXED | 369 | 1379.780 | 1335.000 | 2093.000 | 2322.000 | 985 |

Final rebalanced traffic metrics (decimal MB; k6 totals include warmups/protocol traffic and rejected uploads; successful request-body bytes are separate):

| Workload | k6 sent MB | k6 received MB | Successful request bodies MB | Sampled full checks / failures |
| --- | ---: | ---: | ---: | ---: |
| IQR | 5959.922 | 3382.309 | 3356.109 | 4 / 0 |
| Z_SCORE | 5969.825 | 4165.838 | 4138.213 | 5 / 0 |
| MIXED | 5969.824 | 3679.852 | 3653.111 | 6 / 0 |

The successful scheduled rate differs from completions inside the window because full responses finish after the boundary. These differences are not rounded into a pass. Exact epoch-ms starts/completions, 10-second bins, in-flight counts, byte totals and samples are in [analysis.json](raw/analysis.json) and the per-run request CSVs. Rejection prevented an unbounded application queue; it also proves the target was not met.

The first proxy profile had 980 throttled periods / 1,419 observed, 64,621,263 throttled microseconds. Reallocation reduced that obvious bottleneck; final apps used roughly 3.5–3.7 CPU units each under 3.75 quotas while the proxy/generator had headroom. GC logs and cgroup peaks are separate from B/op. No CPU-stack phase attribution was captured, so sorting versus JSON/GC proportions are not asserted.

Concrete next experiment: retain FULL fixtures, use an independent generator, profile parse/sort/serialization/GC at bounded 1/2/3 in-flight requests, then repeat the three 60-second arrival cases under a newly agreed resource budget. Do not claim more replicas or lower payload/target is already approved. A lower zero-rejection operating point and longer soak have not been established.

Final sampled generator peak was 1.253 GiB with maximum sampled CPU 70.8% (under its 2 CPU/2 GiB cap). App sampled peaks were 2.221/2.170/2.206 GiB; cgroup peaks were 2,389,577,728 / 2,369,810,432 / 2,383,589,376 bytes, each with zero max/OOM/OOM-kill events. nginx peaked at 60.61% sampled CPU under its one-CPU quota. These are finite sampled/lifetime observations, not universal safe memory bounds. All project containers were stopped after post-load full-output recovery checks.
