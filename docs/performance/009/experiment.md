# 009 reproducible commands

All commands run from F:\projects\anomaly-detection. Required cached images: the pinned 008
identity, current 009 build, nginx:1.28.0-alpine3.21 and grafana/k6:1.8.2. All generated project
data is under .cache/009, .cache/tmp, target and docs/performance/009; Docker storage is unchanged.

```powershell
./mvnw.cmd -B spotless:apply clean verify
# Characterization first passed on unmodified 008; final tests include both charset regressions.
docker build --progress plain -t anomaly-detection:009 .
./scripts/experiment009.ps1
# Runs exhaustive A/B checks, matched profiles, then the six fixed-budget A/B target cases.
# No build/test/JFR export process runs alongside final measured traffic.

foreach ($version in 'A','B') {
    $run="$version-profile-IQR"
    jfr summary ".cache/009/profiles/$run.jfr" > "docs/performance/009/raw/$run-jfr-summary.txt"
    jfr print --json --stack-depth 32 --events 'jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ObjectAllocationSample,jdk.GarbageCollection,jdk.GCHeapSummary,jdk.ThreadPark,jdk.SocketRead,jdk.SocketWrite,jdk.JavaMonitorEnter,jdk.CPULoad,jdk.ContainerCPUThrottling,jdk.ActiveRecording,jdk.ActiveSetting' ".cache/009/profiles/$run.jfr" > ".cache/009/profiles/$run-events.json"
}
python -B scripts/analyze009.py
python -B scripts/report009.py
python -B scripts/resources009.py
```

Recordings retain depth128. Both exports use the same nearest32 frames to keep verbose JFR
metadata bounded in volume; this choice is made before examining either final stack result.
Nearest recognized categories and unclassified/native counts remain visible; deep inclusive
transport ancestry can be truncated and is not used as an exclusive CPU hotspot.

Bounded streaming analysis reuses analyze008's decoder (1MiB chunks / 16MiB single-event cap).
The new namespace never runs predecessor report mains against accepted predecessor outputs.
The report preserves timestamps, successful latency n, p50/p95/p99, six 10-second completion
bins, in-flight boundaries, drain, sent/received bytes, sampled checks and rejection/error/drop
counts. A finite all-success eligibility flag alone is not a capacity conclusion.

Exact image/JAR/runtime and configured environment are in raw/A-identity.json and B-identity.json;
sourceCommit in execution metadata denotes the experiment script commit. A's application source
is preserved 008 publication; B's is final ebf432f4a72325aa0504a183b571dabf782d47c0. Both raw image
inspects retain immutable layers; runtime-match.json verifies the same base layers. Effective
Compose configurations retain all CPU/memory/permit/body/count/JVM/proxy limits. Snapshot deltas
come from actual cgroup cpu.stat/memory.* and periodic docker stats; guard observations retain host
availability. Profile metadata stores actual recording hash/bytes/start and JVM settings.

The earlier 416ea7c prototype was stopped on charset review. Its completed and interrupted data
is retained under preliminary/raw, never substituted into the final six-case comparison. Native
verification/pre-push finished before the fresh matched diagnostics and final capacity cases.
No optional final A-MIXED drift repeat was added after the full rerun; baseline pairs remain
contemporary and host-headroom observations are retained, without a claim of statistical replication.
