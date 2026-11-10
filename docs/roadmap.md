# Roadmap

- v0.1: IQR with a strict, documented HTTP contract and real integration checks.
- v0.2: population Z-Score through the same strategy registry.
- Next: benchmark the tested v0.2 application using prompt 003. Measure throughput, latency,
  memory and numerical equivalence before choosing a v0.3 architecture.
- RabbitMQ, gRPC, authentication and per-algorithm services are candidates. Adopt them only
  when benchmark evidence and actual requirements justify their operational cost.

This change does not implement benchmarks or v0.3.
