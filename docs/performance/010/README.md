# 010 — final bounded heap/G1 experiment

**Experiment complete. TUNING_NOT_ACCEPTED. CAPACITY_NOT_EXECUTED. Retain009 runtimeA.**
Optimization is closed; the next activity is the interview voice report. See
[experiment declared before trials](experiment.md), [final handoff](../../interview/final-handoff.md)
and [coherent Persian script](../../interview/voice-script-fa.md).

Same pinned009 image/JAR for every variant, verified by digest and actual startup flags. No
rebuild/pull or application-source change. Application publication bc18d635bcf8359794b0f75aaedbf63d57f481fa;
executed image sha256:e886eab5769e756e3cd5b72d31453be51a9ee5e9ada6b9a215031dd1d3404a4e,
JAR de69e4871d3b94605bb436aca49ff10dfd42684f110316ed21ad0aab2f082aac. Runner/configuration source
0fc287b648f52f8c269d891ea6ae987a0fa28f53; later documentation is not relabeled measured application code.
JDK21.0.11+10, source17, G1. Only maximum heap, then pause goal varied; no GC-thread/region sweep.

## GC audit and interpretation

New tested accounting matches all six 009 counts/sums exactly. Distinct [gc] stop-the-world
summary events are counted once; start/phase/duplicate summaries are excluded. Young, mixed,
remark, cleanup and full pauses are retained per JVM. Concurrent Mark/Undo cycle elapsed is a
separate quantity, never pause time. Cross-replica sums overlap and are not one JVM/request's
stall. Event sums use summary completion timestamps inside the declared window, without clipping
an individual boundary event's duration. Quota throttling is separate cgroup data.

No numerical erratum is required. The interpretation is explicit: neither those sums nor sampled
stack weights establish GC as the sole limiting cause. The actual 009 JFR G1Old events demonstrate
the distinction: baseline18events total 7.083s elapsed but 0.053s sumOfPauses; candidate22events
4.413s elapsed but 0.063s pauses. G1New durations matched their pause sums. Do not add JFR events
again to matching log pauses. Historical records/evidence are untouched.

## Actual matched screens — not capacity passes

One app3.75CPU/3GiB, active3; nginx1CPU/128MiB; k6 2CPU/2GiB/GOMAXPROCS2, three constant clients,
25s MIXED with100ms think time, same two FULL warmups. Each variant first passed exhaustive FULL
validation for both2M algorithms (three samples each), every value/order/field/count/statistic.
Seed20261110;9.9MB bodies received completely and usually discarded locally after reception;
every101st iteration sampled semantics. Timestamp CSVs prove three actual active requests overlap.
Post-load small-request recovery is200 for all variants. No new010 overload deployment was run;
occupied-admission/recovery remains covered by the existing deterministic HTTP suite. Closed-client work is not ten-RPS proof.

| Variant | Xmx / goal / ergonomic region | Successful (incl. drain) | Mean / p95 / p99 ms | STW count / sum ms / max ms | Cgroup peak GiB |
| --- | --- | ---: | --- | --- | ---: |
| A | 2048MiB / 200ms / 1MiB | 81 | 827.185 / 1118.000 / 1239.800 | 178 / 11420.117 / 496.002 | 1.948 |
| B | 2560MiB / 200ms / 2MiB | 79 | 845.038 / 1068.000 / 1186.100 | 175 / 11531.241 / 319.891 | 2.107 |
| C | 2560MiB / 500ms / 2MiB | 80 | 843.237 / 996.650 / 1127.410 | 168 / 12077.757 / 360.902 | 1.958 |

All three have0rejections/unexpected/timeouts/drops/semantic failures; safety guards never stop,
max/OOM/kill counters stay0, and each screen proves overlap3. Initial heap512MiB throughout;
ParallelGCThreads4/ConcGCThreads1 remain ergonomic/default. Increasing Xmx automatically changes
regions1→2MiB, so B is not an identical-heap/region comparison. C adds500ms to B's2.5GiB heap;
its extra effect is79→80 completions and845.0→843.2ms mean, not the whole heap change.

B/C perform fewer completions and have worse mean thanA. Lower isolated p95/p99 and C's fewer
collections do not outweigh that: C's total STW sum is larger. The qualitative useful-work rule
was declared before all trials; a5% work/no-worse-mean refinement was fixed after A but before
B/C results. Neither qualifies under either criterion. **No promising candidate; scaled final
A/selected pairs and a hardest-case repeat are SKIPPED**, as authorized, not claimed passing.
No extra configuration or search begins. RetainedA is G1/Xms512m/Xmx2048m/default200ms goal.

All three had more than1.5GiB host headroom (minimum7.887GiB) and stayed below 95%container use;
B/C short safety is not proof for an eventual full2.5GiB live/committed heap. The nominal0.5GiB
native/thread/code budget is a risk bound, not measured native-reserve proof. Cgroup/process RSS,
maxima and GC heap meanings remain distinct; no NMT/exact native-memory attribution was added.
Raw per-JVM events, actual flags/origins, process/cgroup samples, counters and recovery are in
raw/screen-analysis.json. Per-call CPU/throttling includes outside validation and warmup, unlike
windowed pause sums. Generator docker stats/state remain ignored on F: and in compact run metadata.
The guarded runner preserves inspect/state before any removal and uses no --rm for applications.
The identity-only first launch failed from PowerShell quoting, retained separately; no timed
candidate crashed or exited137. All original six unrelated services remain; task containers/network
are removed. No builds/JMH/coverage/JFR/agent accompanied measured screens. No core JMH rerun.

## Quality, provenance and reproduction

Application67unit/101HTTP and separate harness 9 pass; Node 2, existing Python 12 and new Python 4
pass. Strict JavaDoc/format/lint and hooks/CI remain mandatory. Exact zero-missed counters:
LINE364/0, BRANCH172/0, INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0.
No application classes/tests were added or changed. GC tests exercise duplicate/phase/concurrent
separation and per-JVM boundaries; safety tests exercise overlap and unsafe memory/events.
No functional RED is fabricated for runtime-only tuning. Source src/main/java is unchanged.

Actual successful [CI42964647730](https://github.com/ali-souri/anomaly-detection/actions/runs/42964647730)
on 0fc287b648f52f8c269d891ea6ae987a0fa28f53 was independently inspected after downloading artifact
13831625233 (960,096 bytes), digest sha256:fecab3cd47ac3300e7677331523fe55c6a14c24d17383dd5518e363389413d84.
The artifact confirms exact test/counter/nonempty-exec evidence and all quality steps. No source,
coverage exclusions, ignored cases, agents/tmpdirs or thresholds changed. Source/CI versus later
report publication are separate. No develop/master merge or v0.3 release is made.

The retained experimental three-app capacity configuration is in compose.010.capacity.yaml;
use with compose.yaml + compose.capacity.yaml + compose.010.yaml, --no-build --pull never. Its
3×3.75CPU/3GiB/active3 + nginx1CPU/128MiB + generator2CPU/2GiB/max20VUs remain14.25CPU/11.125GiB.
This unchanged budget is not a newly executed010 capacity pass. Last009 rates 6.70/7.133333/6.966667
remain historical; ten RPS is unmet. Single-instance defaults active1 /2M/64MiB remain separate.
See the handoff for short test/API/Swagger/reproduction commands. All project data stays on F:;
existing Docker storage and installed tools are unchanged. Repo access stays PRIVATE, owner review
only. No invitations/audio/interviewer messages. **Stop optimization; voice handoff is ready.**
