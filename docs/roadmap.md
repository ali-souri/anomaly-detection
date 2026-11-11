# Roadmap

- v0.1: IQR and strict real HTTP contract.
- v0.2: population Z_SCORE through the same Strategy registry.
- v0.2.1: io.anomaly roles, exact combined coverage and immutable benchmark baseline.
- Benchmark 003: executed native JMH/full API evidence on v0.2.1; immutable result/raw data retained.
- v0.3 candidate, prompt 007: measured reference-reuse allocation gain, early bounded admission,
  Docker and actual open-arrival evaluation of **10 successful 2M FULL requests/second total**
  separately IQR, Z_SCORE and 50/50 mixed. No owner latency SLO exists.
  See [release gates](releases/v0.3.md), [measurements](benchmarks/v0.3/report.md) and
  [capacity status](load-tests/README.md). Prompt 007 supersedes unexecuted 006; 004/006 need not run.
- Conditional later work: profile/repeat the measured capacity bottleneck, longer stress/soak,
  independent generator, production resource/latency requirements. Brokers, gRPC, persistence,
  auth and independent algorithm services need separate evidence and owner requirements.

Timed JVMs have no active Java agent. Coverage remains a mandatory separate clean verify gate in
pre-push/CI. Frozen releases and issued prompts/results stay intact. No next task starts automatically.
