# Experiment 008 — declaration

Start: 2026-11-11 01:40:53 UTC. Preserve 007 publication
33d4e2f3936723cbc4ec28cd03e4428944cecc60 as the primary before comparator, not v0.2.1.
No production code change before actual stack evidence. Six diagnostic runs: IQR/Z_SCORE ×
1/2/3 active requests on one 3.75 CPU / 3 GiB / 2 GiB G1 instance. Same seed 20261110,
2M MIXED, thresholds and full eager response. Controlled closed clients equal active permits,
100 ms think time, 30-second load after serial warmups. These are diagnostic runs, not target passes.

JFR settings=profile, stackdepth=128, delay=20s after JVM startup, duration=15s, maxsize=128m.
Record to /app/tmp then copy to ignored F: project storage; extract stacks at depth 128. Verify
timestamps overlap warmed load. Keep hashes, settings, counts, CPU/allocation/GC summaries in Git.
CPU grouping must distinguish exclusive leaf/first-match estimates from inclusive ancestor counts;
preserve unclassified/native/blocked work and separate sampled allocation weights from exact B/op.

Starting Docker VM has 15.46 GiB, about 13.50 GiB MemAvailable. Windows free memory is lower than
007: initial 9.98 GiB, next capture 9.31 GiB. Begin one instance (max app+generator 5 GiB); inspect
headroom again before scaling and monitor available host memory. No other services are stopped.

Final unchanged ceiling: 3 apps × 3.75 CPUs/3 GiB/2 GiB heap, active=3; nginx 1 CPU/128 MiB;
k6 2 CPUs/2 GiB/GOMAXPROCS=2/max20 VUs. Total 14.25 CPUs/11.125 GiB. No added resources,
compression, lower target or smaller output. Java21.0.11 containers and Java21.0.10 native results
are separate. Final timed runs have no active JFR recording or Java agent and no concurrent builds.

After stack analysis, declare at most one small optimization and its measurable prediction/risks.
Preserve existing GREEN contracts; representation-only work needs no fabricated functional RED.
Then exact clean verification, paired two-fork/multiple-iteration 2M core, full API and three
60-second ten-RPS open-arrival cases through nginx. Record every failed/unexecuted attempt.
