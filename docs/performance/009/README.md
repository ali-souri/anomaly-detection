# 009 — direct JSON reader, contemporary A/B evidence

This feature removes the intermediate JsonNode tree from the API input path. It retains the
ordinary immutable numeric request, all strict wire/error/limit/admission policies and the entire
008 statistical core. Hypothesis was written before production edits. This is one API input
optimization; serializer, FULL output, permits, dependencies and resources did not change.

## Identities and compatibility

- Preserved baseline publication: 8d7d41c34c9e2d10e955092239fd87bb91a8acf7.
- Final measured code/tooling: ebf432f4a72325aa0504a183b571dabf782d47c0.
- Both core source trees: 85c0601c28df59dd09bc02ab4eba4881d3bb6094; source diff is empty.
- Baseline image: sha256:26a1bf88cfee5aebb907539488227f083aae105509de4c53a38bfc3a77251df5.
- Candidate image: sha256:e886eab5769e756e3cd5b72d31453be51a9ee5e9ada6b9a215031dd1d3404a4e.
- Executed baseline JAR: f23766f4d65e8764b0021f87ead2752aa2607715e985be8b4968f87250d2cfbe.
- Executed candidate JAR: de69e4871d3b94605bb436aca49ff10dfd42684f110316ed21ad0aab2f082aac.
- Shared exhaustive Java API harness source: 331cb861ee6fbbbbae77aac4e82878647cb2eec4;
  executed SHA-256 95666B36A1D36B579969D8EB70F145A39E72FDE96806F110345C02C14E6C9064.
- App runtime: identical five Temurin runtime filesystem layers, Java21.0.11+10, G1. Native
  validation client Java21.0.10+7 is identical for both versions. Java source target17 stays.

The configured JsonFactory reads allowed fields in any order, validates token types, and appends
finite Double values without preallocating the maximum capacity. Final duplicate members win,
including replacement of earlier invalid/oversized values. Syntax consumption precedes final
semantic validation; first-root/trailing behavior is retained. Integer -0 versus floating -0.0,
large integers-as-doubles, read constraints and exact Spring charset selection are characterized.
IOException/read failures deliberately become public MALFORMED_JSON. Unsupported algorithms,
numerical422, media/method errors, safe500, count/body413, busy503/Retry1 and recovery remain.

The existing readNBytes(limit+1) advice remains. Spring's byte[] converter makes an extra full
raw copy. The change removes per-number tree nodes and traversal; it does not remove buffering
or make global detection constant-memory. Lists, snapshots, sort scratch and raw buffers remain
O(n). Swagger still describes DetectionRequest as a JSON object with the same examples.

Final local verification: **67 unit / 101 genuine HTTP**, zero failures/errors/skips;
LINE364/0, BRANCH172/0, INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0.
Thirty handwritten Java files / 32 XML entries / 29 executable classes, no new exclusions or
weakened guards. Strict JavaDoc, format/lint, separate Java harness9, Node 2, old Python 8 and new
Python 4 checks pass. See [honest test order](../../tdd/11-direct-json.md).

Initial prototype runs are retained in preliminary/ and excluded from final conclusions. A
US-ASCII compatibility regression found during review caused a fresh corrected-image experiment.
The first generic-UTF16 test expectation was incorrect and is not claimed as a functional RED.
Pinned-baseline probes and the complete converter implementation confirmed byte autodetection.
All accepted 007/008 evidence, issued records and frozen refs remain unchanged.

## Experiment and interpretation

[Declared hypothesis](hypothesis.md), [commands and configuration](experiment.md),
[raw evidence](raw/). Both implementations first pass exhaustive FULL validation outside timing:
three samples per algorithm/version, every one of 2M values/order/partition/statistic/field checked.
Both algorithms return 40,000 anomalies and 1,960,000 normal values. IQR bodies are 9,900,170 bytes;
Z_SCORE bodies 9,900,183 bytes. The existing seeded MIXED fixture is unchanged (seed20261110).

Resources: 3 apps × 3.75CPU / 3GiB / 2GiB heap / active3, nginx 1CPU/128MiB, k6 2CPU/2GiB,
GOMAXPROCS2/max 20 VUs: **14.25CPU / 11.125GiB** ceiling. Default app policies remain active=1,
max2M/max64MiB. Separate A/B deployments never overlap. Unrelated services stay running.
Host guard stops only task-owned containers below 1.5GiB available physical RAM.

Final declared order is A-IQR, B-IQR, A-Z_SCORE, B-Z_SCORE, A-MIXED, B-MIXED. Every case uses a
fresh deployment, two serial FULL warmups (one per algorithm), 10 total arrivals/second, 60-second
window, at most20VUs and 30-second drain allowance. The window starts at the first actual request
start and ends exactly 60s later, as declared before results. Successes received during drain are
reported separately from exact-window completions; no window is moved to improve the result.
All bytes are received; most response bodies are discarded locally after reception. Every101st
iteration performs sampled semantic validation; full exhaustive checks precede load. 503s count
as rejection, never success/latency. No retries, compression, truncation or fixture fast paths.
Final loads have no JFR/JaCoCo/build/JMH alongside them. Core JMH is unchanged and was not rerun.

JFR diagnostics are separate: one instance, active=3/client=3, 30-second traffic after the same
serial FULL warmups, 100ms think time, 15-second profile recording delayed20s, depth 128,
maxsize 128MiB. Both analyses export the nearest 32 frames from the depth 128 recordings to bound
verbose metadata volume; deep inclusive ancestors may be truncated. Old tree+conversion and new direct-token frames share the input-parsing bucket.
Exclusive percentages choose the nearest recognized leaf-side frame; inclusive ancestors overlap.
Native/unknown/blocked work stays visible. Sample weights are estimates, not exact B/op/live heap
or wall-clock phase durations. Recordings/verbose exports stay ignored on F:; only compact
settings, hashes and analyses are committed. Cgroup memory includes buffers/heap/native/cache;
GC heap and docker working-set observations are distinct. Whole-call CPU deltas include warmups
and observation overhead; GC pauses are restricted to the declared steady window.

## Final observed results

**OPTIMIZATION_VERIFIED; CAPACITY_TARGET_NOT_MET.** Keep feature/v0.3-direct-json; no v0.3 release.

| Run | Scheduled | Success | 503 | Success/60s incl. drain | Exact-window success/s | Mean / p50 / p95 / p99 ms (n=success) | Drain ms |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| A-IQR | 601 | 395 | 206 | 6.583333 | 6.450000 | 1271.5 / 1267.0 / 1514.3 / 1593.5 | 676 |
| B-IQR | 601 | 411 | 190 | 6.850000 | 6.700000 | 1217.0 / 1205.0 / 1505.5 / 1747.1 | 790 |
| A-Z_SCORE | 601 | 413 | 188 | 6.883333 | 6.733333 | 1216.4 / 1221.0 / 1443.2 / 1547.6 | 561 |
| B-Z_SCORE | 601 | 437 | 164 | 7.283333 | 7.133333 | 1134.1 / 1126.0 / 1422.0 / 1508.7 | 653 |
| A-MIXED | 601 | 401 | 200 | 6.683333 | 6.550000 | 1257.1 / 1253.0 / 1558.0 / 1711.0 | 864 |
| B-MIXED | 601 | 427 | 174 | 7.116667 | 6.966667 | 1173.2 / 1161.0 / 1441.7 / 1592.4 | 1005 |

All six runs have **0 unexpected errors, 0 timeouts, 0 drops, 0 sampled semantic failures**.
Configured offering is10/s; k6 actually scheduled 601 iterations at the finite window boundary.
That extra boundary arrival is disclosed, not rounded away. The successful/60s column includes
responses received after the declared window; the exact-window column does not. The full
requests.csv and load-analysis.json retain all starts/completions, six10s bins, in-flight counts,
latency n, sampled checks and actual traffic bytes. No stable ten-RPS claim follows from these
6.45–7.13 exact-window rates or the bounded drain. In-flight boundaries remain bounded by rejection;
there is sustained overload rather than a hidden successful queue. Each paired case is one run,
not a confidence interval or endurance proof. Contemporary success gains are only 4.05%,5.81%,6.48%.
Historical 007/008 counts are not used as this experiment's baseline.

| Matched IQR diagnostic | A baseline | B candidate |
| --- | ---: | ---: |
| Java execution / native samples | 1275 / 463 | 1324 / 471 |
| Input-parsing exclusive Java samples | 38.8235% | 31.5710% |
| FULL serialization | 26.1176% | 27.8701% |
| Sort workspace | 18.3529% | 19.5619% |
| Statistics/classification | 5.3333% | 8.5347% |
| Validation copy | 6.6667% | 6.7976% |
| Result construction | 4.1569% | 4.9849% |
| Raw-body buffer | 0 sampled | 0.0755% |
| Unclassified Java | 0.3137% | 0.3021% |
| Native transport/wait samples | 463 | 471 |
| Input-parsing allocation sample weight | 37,621,705,288 B | 28,019,856,200 B |
| Total allocation sample weight | 69,601,993,944 B | 70,242,208,072 B |
| Maximum observed GC heap | 1,795,661,144 B | 1,456,082,536 B |
| Whole diagnostic cgroup memory peak | 2,175,778,816 B | 1,866,215,424 B |
| GC events in recording | 103 | 109 |
| 30s controlled successful requests | 107 | 115 |
| Controlled successful mean latency | 735.617 ms | 678.609 ms |

The input bucket drops 7.25 percentage points and its weighted allocation estimate drops 25.52%.
DoubleNode appears among baseline allocation samples and disappears from the candidate's leading
classes; byte/Object-array/String weights remain substantial. Total weighted allocation rises
about 0.92%, while controlled throughput rises 7.48% and mean falls 7.75%. These sampled class weights
are coarse estimates, not exact object counts, per-request B/op, or a claim that total allocation
was eliminated. Zero raw-copy Java samples in A are not zero buffering. Both raw buffers and the
extra byte[] copy remain included in container/process observations. Native samples are mostly
transport waits and remain separate from Java CPU percentages. Blocked-event counts, GC details,
recording settings/hashes and deep-stack limits stay visible in raw analyses.

The remaining measured costs are input decoding/numeric conversion, eager FULL serialization,
IQR sorting and frequent allocation/GC under concurrent load. A smaller input phase does not
remove the other work or guarantee that another parser change, extra permits or microservices
would meet the target. No further optimization begins automatically.

## Resources, GC and publication

| Run | App whole-call CPU seconds (3 replicas sum) | App maximum cgroup peak GiB | Steady-window GC pauses / cross-replica sum ms / max ms | Minimum host available GiB |
| --- | ---: | ---: | --- | ---: |
| A-IQR | 659.214 | 2.175 | 960 / 79402.409 / 345.449 | 7.187 |
| B-IQR | 652.134 | 1.993 | 910 / 78892.021 / 396.345 | 8.218 |
| A-Z_SCORE | 666.135 | 2.177 | 1005 / 85454.821 / 351.675 | 7.118 |
| B-Z_SCORE | 648.915 | 1.963 | 974 / 84029.481 / 471.759 | 7.958 |
| A-MIXED | 664.482 | 2.180 | 958 / 82298.037 / 314.641 | 7.244 |
| B-MIXED | 654.293 | 2.014 | 927 / 82063.772 / 423.366 | 8.021 |

GC pause sums aggregate three separate JVMs and may exceed 60s; they are not a single-request
latency or one process's pause time. Cgroup CPU deltas include warmup/observation and retain
original usec/period units. Actual app throttling is present (roughly13.8–23.2 cumulative throttled
seconds per replica/call); raw counters preserve each period count. No memory OOM/max event or
headroom safety stop occurred. Minimum host availability is 7.118GiB, above the 1.5GiB guard.
Observed generator peaks are73.6–132.83%CPU against its200%quota, with memory below 2GiB; it drops
no iterations. Exact observed memory strings/CPU and proxy data are in resource-analysis.json.

Mixed-run process samples, independently of cgroups/GC heap: A Java RSS1,950,632 /1,884,020 /
1,503,932KiB, high-water2,285,480 /2,243,908 /2,279,560KiB; B RSS1,293,108 /858,172 /
2,042,416KiB, high-water2,057,844 /1,973,544 /2,123,580KiB. These are one steady mixed snapshot
per replica, not all-case process peaks. process-memory.json retains UTC/argv/image identities.

Actual CI [42958128656](https://github.com/ali-souri/anomaly-detection/actions/runs/42958128656)
is successful on exact code SHA ebf432f4a72325aa0504a183b571dabf782d47c0. Downloaded artifact
13828226583 (960,057 bytes) was independently inspected: application67 unit/101HTTP and harness9,
zero failure/error/skip, the same exact coverage counters, nonempty execution data and all steps
green including strict JavaDoc/format/lint/Node/Python. Artifact digest is
sha256:a0b49312ac96db05ec08b2f337c843f02afa2316c14771753ef3068e90f01633.
The later publication adds documentation/evidence only; application/test/tool/workflow trees stay
identical to that measured, CI-verified source. develop/master and all frozen refs are unchanged.
All task-owned containers/networks were stopped; the original six unrelated services remain.
