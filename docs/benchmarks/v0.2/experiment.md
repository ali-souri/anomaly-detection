# Declared experiment (before timings)

Prompt AD-PRM-20261110-003; prerequisite AD-RES-20261111-005 selects **v0.2.1**, exact source
0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7. Production source/resources are unchanged.
Machine/resource declaration is raw/machine.json. No owner latency SLO/concurrency target exists.

Core matrix: IQR and Z_SCORE × 10,000 / 100,000 / 1,000,000 / 2,000,000 numbers PER DATASET ×
MIXED / DUPLICATES / DESCENDING. Seed 20261110. One thread, two separate forks, G1, Xms512m,
Xmx2g, project-local tmpdir. JMH 1.37: two 1-second warmup iterations and three 1-second
measurement iterations per fork (six measured iteration means/case); GC profiler; average ms/op.
This is a bounded exploratory experiment, not established production tail latency or capacity.

Each 100-element histogram has either 0..97 once plus -1000,+1000 (MIXED), or 98 zeros plus
-1000,+1000 (DUPLICATES). Repeat to size; seeded shuffle. DESCENDING has the MIXED histogram in
descending order: an ordered TimSort-friendly arrangement, not a claimed worst case. This gives
independent analytical quartiles, population moments and exact 2% outlier counts for both defaults.
Generation and correctness checks occur outside timing. A trial reuses an immutable fixture;
each real detector invocation includes its own validation, copies, sort/statistics and full result
partition allocation. The JMH return value is consumed by the harness.

API matrix: one untimed 10,000-value MIXED warmup per algorithm, then three serial 2,000,000-value
MIXED requests per algorithm, IQR then Z_SCORE, against the real release application. Full response
includes normalValues. Timed client interval includes request-file reading, HTTP and response-file
writing; JSON generation and post-receipt correctness validation are outside the interval. Record
payload byte counts, every duration, median/range, project-server working-set observations. No p99.

Timed JMH fork and API JVM command lines must contain no JaCoCo agent. Root clean verify remains
a separate required gate. Only project-owned application processes are started/stopped. Massive
request/response/fixture files stay under ignored .cache; compact JSON/CSV results are committed.
No other builds or API load run concurrently with JMH measurements. No CPU affinity/OS tuning or
global settings changes are planned; desktop background activity and short iterations limit inference.
