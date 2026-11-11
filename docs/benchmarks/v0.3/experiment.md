# Run 007 — declared experiment

Declared before capacity measurement on 2026-11-11. Immutable baseline: v0.2.1,
0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7. Prior harness: 7dfb173a0d1cef3a30a4ae6e24998614da282fe0.
Preserve all v0.2 raw evidence. New run identity: **007-local-20261111**.

## Fixed budget

Host: i7-12700K, 12 physical / 20 logical processors; Windows reports 33,231,280 KiB
usable RAM and 16,700,788 KiB free at declaration. Docker Desktop Linux engine 29.6.2,
20 logical CPUs, 16,605,085,696 bytes RAM, existing storage /var/lib/docker; no relocation.
Six unrelated containers use about 0.85 GiB; leave them untouched.

Maximum application budget: **three replicas**, each **4 CPUs, 3 GiB memory, Xms512m/Xmx2g G1**.
Load balancing, if needed: nginx, 0.25 CPU / 128 MiB. Generator: official k6 1.8.2,
2 CPUs / 2 GiB maximum, 20 VUs. Total new maximum: 14.25 CPUs / 11.125 GiB;
remaining Docker RAM and logical CPUs reserve room for existing services, native overhead,
OS and scheduling. Stop escalating replicas at this budget; do not change global Docker settings.
Native paired comparisons run separately, not alongside capacity tests: JDK 21.0.10+7,
Xms512m/Xmx2g G1, one thread, no agent, same seeded immutable FULL fixtures.

## Ordered measurements

1. Two forks, two 1-second warmups and three 1-second measurements, GC profiler, per
   implementation/algorithm at 2M MIXED, seed 20261110. Additional matrix only if time permits.
   Validate full mathematical/order/immutability contracts outside timing.
2. Three real native FULL API samples/algorithm/implementation with streaming full-output checks.
3. Compose demo: one replica, active limit 1, inclusive 2M numbers and 64 MiB raw body.
   Validate both full 2M outputs, ordinary traffic, clients 1/2/4, overload/recovery and invalid/oversize.
4. Open arrival: separate IQR, Z_SCORE, 50/50 mixed at **10 total requests/second**, 60 seconds
   after warmup each. Maximum 20 in flight; no retries. Count drops, 503, 413, errors and timeouts
   separately. Fully receive bodies; discard timed bodies after reception, retain byte evidence.
5. If one replica misses: inspect measured concurrency/memory/CPU, try small admission tuning,
   then up to three copies of the same application through nginx. Publish effective settings.

No latency SLO was supplied. Successful request latency only: average/p50/p95/p99 with counts;
three correctness/API samples do not establish p99. Report timestamps and window/drain boundaries,
backlog and actual rates. No sustained rejections, timeouts, drops or backlog growth qualifies
as a target pass. If this bounded host misses, report CAPACITY_TARGET_NOT_MET and keep the feature
branch without a release. Docker evidence is a changed Linux/cgroup environment, not an algorithm
comparison against the Windows baseline.

## Budget-neutral adjustment declared after first scaled measurements

At 2026-11-11 00:04 UTC the 0.25-CPU nginx profile showed sustained approximately 24-25% CPU,
980 throttled periods out of 1,419 and 64,621,263 throttled microseconds, while application CPUs
were below their quotas. The planned next small configuration experiment reallocates existing
CPU: **3 × 3.75 application CPUs + 1 nginx CPU + 2 generator CPUs = the same 14.25 CPU ceiling**.
Heap, memory, replica count, active=3, fixtures, output and 60-second 10-RPS scenarios stay the
same. This is a measured proxy configuration adjustment, not additional/unbounded infrastructure.
Its actual execution and outcomes must be reported separately.
