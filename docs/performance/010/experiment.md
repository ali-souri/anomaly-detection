# 010 experiment — declared before tuning trials

Preserve009 publication bc18d635bcf8359794b0f75aaedbf63d57f481fa / exact-code CI ebf432f.
Every variant uses the SAME pinned image sha256:e886eab5769e756e3cd5b72d31453be51a9ee5e9ada6b9a215031dd1d3404a4e
and JAR de69e4871d3b94605bb436aca49ff10dfd42684f110316ed21ad0aab2f082aac. No build/pull/source change.
The identity-only first JVM launch failed from PowerShell argument quoting, not application logic;
its state is retained and the correctly quoted launch verifies actual Java21.0.11 settings.

GC audit first: distinct-event accounting reproduces all six009 pause counts/sums. Concurrent
Mark/Undo cycle elapsed is separately recorded, never added to pauses. Young/mixed/remark/cleanup/
full remain separate per JVM; duplicates/start/phase lines are excluded. Cross-replica sums overlap
in time and are not one request/JVM's delay. GC alone is not established as the capacity cause.
No numerical erratum; historical files remain unchanged. JFR GC duration can include concurrent
work; sumOfPauses is distinct and is never added again to the same log-derived pauses.

Hypothesis: more heap, then (only if safe) a relaxed G1 goal may reduce GC overhead and improve
successful throughput. Unproven; no ten-RPS promise. Oracle's [JDK21 G1 guidance](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-garbage-collector-tuning.html)
supports heap/pause-goal tests for throughput. Pause goals are soft, not response-time guarantees.

Only A/B/C, no sweep/retry grid:
- A: -Xms512m -Xmx2048m -XX:+UseG1GC, actual default MaxGCPauseMillis=200, region1MiB.
- B: ONLY maximum heap changes to -Xmx2560m, same200ms goal.
- C: only after safe B, same2560m heap plus -XX:MaxGCPauseMillis=500; compare added effect with B.
Capture actual ergonomic region (may change with heap), collector/flags/initial/max heap and cgroups.
Common app-only /app/tmp, startup PrintFlagsFinal and gc+init/gc info logs are identical; no active
JFR/JaCoCo or verbose per-phase final diagnostics. No global JAVA_TOOL_OPTIONS contamination.

Safety/screens: A, B, then C only if B safe. Single app, active3, three mixed clients for25s,
matched two serial FULL warmups. First validate complete2M FULL responses for both algorithms and
all configs outside timing; prove three actual requests overlap. Before any scaled larger heap,
monitor cgroup/process/native headroom and max/OOM/kill/transport/readiness. Stop promptly at98%
current container use,95% on two successive samples, any max/OOM event or host free<1.5GiB.
Capture failure state/logs before removal (no --rm apps, preserve exit137). Short safety is not soak.
Choose at most ONE safe promising candidate using completed work/GC/CPU/memory/latency together;
otherwise retain A, skip final capacity and stop searching. Screens are not capacity passes.

If promising, fresh interleaved A/selected IQR, A/selected Z_SCORE, A/selected MIXED. Same resources:
three3.75CPU/3GiB apps, active3 each; nginx1CPU/128MiB; k6 2CPU/2GiB/GOMAXPROCS2/max20VUs.
Outer14.25CPU/11.125GiB; unchanged PID/readiness/shutdown/swap limits. Only JVM heap/goal vary.
Two serial FULL warmups,10total arrivals/s for60s,30s drain. Window is first actual request start
through exactly60s, declared before results. Count/60 including drain and exact-window completions
remain distinct; retain timestamp/10s/in-flight/drain/latency n/traffic/error/drop data. No shifted
windows or rounded passes. If all pass, repeat hardest case once within budget before readiness.

Seed20261110, identical bytes/defaults/FULL40,000 anomalies+1,960,000 normal values. Receive all
bytes then discard locally except every101st sampled semantic check; exhaustive validation stays
outside timing. Post-overload recovery required. No builds/JMH/coverage alongside traffic, no
unchanged core JMH. No fabricated functional RED for this configuration-only task.

STOP optimization after010 regardless of result; final-handoff.md and a coherent4–5minute Persian
voice script are required. No audio/contact or further performance prompt. Keep007/008/009 immutable.

Quantitative refinement fixed after A completed81 requests and before B/C results: a promising candidate completes at least5% more
work thanA with no errors/safety issue and no worse mean successful latency. If both qualify,
prefer more completed work while inspecting GC/memory/tails; compare C's extra effect withB.
Final retention requires safe useful paired results, rather than merely a larger heap or one p99.
