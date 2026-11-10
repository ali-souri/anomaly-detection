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

This change does not implement benchmarks or v0.3.
Timed production/JMH JVMs must have no active JaCoCo agent; clean coverage verification remains
a separate mandatory pre-push/CI gate and must not be bypassed for benchmarks.
