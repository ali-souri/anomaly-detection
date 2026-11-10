# Roadmap

- v0.1: IQR with a strict, documented HTTP contract and real integration checks.
- v0.2: population Z-Score through the same strategy registry.
- v0.2.1: io.anomaly package roles and exact 100% combined LINE/BRANCH coverage.
- Next: benchmark immutable **v0.2.1** using AD-PRM-20261110-003, after reading prerequisite
  docs/results/AD-RES-20261111-005.md and confirming COMPLETE. Historical v0.2.0 is no longer
  the selected benchmark baseline. Resolve v0.2.1 to its exact commit before measurement.
  Measure throughput, latency,
  memory and numerical equivalence before choosing a v0.3 architecture.
- RabbitMQ, gRPC, authentication and per-algorithm services are candidates. Adopt them only
  when benchmark evidence and actual requirements justify their operational cost.

The v0.2.1 maintenance did not implement benchmarks or v0.3; benchmark 003 was executed subsequently below.
Timed production/JMH JVMs must have no active JaCoCo agent; clean coverage verification remains
a separate mandatory pre-push/CI gate and must not be bypassed for benchmarks.

Benchmark AD-PRM-20261110-003 has now executed on v0.2.1. See
[measured results](benchmarks/v0.2/report.md) and the
[v0.3 recommendation](decisions/v0.3-architecture.md): retain a stateless modular application,
measure allocation reduction as the primary next experiment, and establish bounded admission/load
behavior before considering independent services. No absolute SLO/concurrency target was supplied.
The next conditional prompt must read AD-RES-20261110-003; v0.3 is not automatically started.
