# Run 007: paired allocation and complete API evidence

Run identity: **007-local-20261111**. This is exploratory local evidence; no production latency
SLO or concurrent capacity is inferred from a core microbenchmark. See the separately executed
[capacity experiments](../../load-tests/README.md) and [predeclared budget](experiment.md).

## Artifact identity and method

Immutable baseline **v0.2.1 / 0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7** was explicitly rebuilt
and passed clean verify in an isolated `git archive` on F:. Its 46 unit/47 real HTTP tests and
246/0 LINE, 112/0 BRANCH coverage remain distinct from the candidate. No baseline safeguard or
accepted v0.2 raw file was removed or overwritten.

Measured candidate/harness commit **331cb861ee6fbbbbae77aac4e82878647cb2eec4**; implementation
first committed at 4bdc5c1e51189f7bb88c7aea1c4640406fac7214. Each implementation uses an explicit
plain detector JAR before the same candidate harness on the classpath, in **separate JVMs**.
Actual fork logs print detector class origins. [Identity files](raw/) record executed SHA-256,
classpath, commit/core tree and UTC time; API process records separately hash executed Boot JARs.
Later Tomcat discarded-body configuration fixes do not change the statistical core or harness.

The original two identity files' field named coreTree records the candidate HEAD tree in both
files; it is **not** the baseline tree. Baseline core tree is
**db8f6f19d1be30346ed73d9d8d1c5f7797daf97b**, candidate core tree is
**dd25bf3771f9caff665709518d96ba9862a8ba29**. The supplement provenance.json records that distinction,
archive/JAR hashes and class origins. The reproduction script now labels both trees explicitly;
the original raw identity records remain unchanged.

Native Windows 11 / i7-12700K / 20 logical CPUs; Temurin **21.0.10+7**, Java 17 target,
Xms512m/Xmx2g G1, one thread. JMH 1.37, GC profiler, **2 forks × 2 one-second warmups ×
3 one-second measurements**, six measured iteration means/case. No active Java agent in launcher
or forks; runtime assertions reject one. No local builds/API load ran alongside JMH. Existing
desktop/background services and hybrid-core scheduling remain uncontrolled.

Completed timed matrix: **2 implementations × 2 algorithms × 2M MIXED**, seed 20261110.
Other sizes/arrangements were not timed in this paired run. Independent analytical/order/count/
input-nonmutation preflight still passed all 24 established fixtures for both implementations;
trial setup also validates each timed case. Full copies/statistics/sort/eager immutable ordered
partitions remain **inside** detect timing. Nothing is deferred to getters or omitted from output.

## Actual core measurements

Time is ms/op ± JMH's reported 99.9% confidence half-width; allocation is profiler B/op and
binary MiB/op. Confidence intervals describe these short runs, not a latency SLO.

| Algorithm | Baseline ms/op | Candidate ms/op | Baseline B/op (MiB) | Candidate B/op (MiB) |
| --- | ---: | ---: | ---: | ---: |
| IQR | 551.530 ± 58.159 | 601.730 ± 153.304 | 121,586,141.333 (115.954) | 73,586,132.000 (70.177) |
| Z_SCORE | 167.831 ± 33.622 | 139.124 ± 30.487 | 105,389,649.778 (100.507) | 57,389,461.905 (54.731) |

Measured allocation reductions: **48,000,009.333 B/op (39.48%) IQR** and
**48,000,187.873 B/op (45.55%) Z_SCORE**. Classification reuses existing immutable Double values
from the validated snapshot, while arithmetic still unboxes to primitive double. Snapshots,
sorting, order, duplicates, immutable result lists and FULL output are preserved. The clear
allocation benefit does not justify a primitive-array model migration in this increment.

The timing intervals overlap in both algorithms. The candidate IQR point estimate is slower;
neither a universal speedup nor a statistically established IQR regression is claimed. Allocation
is not live heap, retained memory, working set or a per-request container memory bound.
GC count/time raw scores aggregate measured iterations; they are not per-request pauses.

## Actual complete native API samples

Separate native uninstrumented Boot JVMs used the same heap/collector/JDK. After one 10k warmup
per algorithm, three 2M samples each ran serially. Every field, statistic, value, order and count
was streaming-validated after full reception; 40,000 anomalies and **1,960,000 normalValues** each.
Timing includes file read, loopback HTTP, server parse/compute/serialize and complete response-file
write. Fixture generation and validation are outside timing. No three-sample p99 is asserted.

| Algorithm | Baseline samples ms | Candidate samples ms | Baseline / candidate median ms |
| --- | --- | --- | ---: |
| IQR | 769.424, 725.095, 752.399 | 806.965, 711.527, 782.474 | 752.399 / 782.474 |
| Z_SCORE | 422.565, 397.644, 414.565 | 341.154, 424.781, 290.878 | 414.565 / 341.154 |

Request body bytes: IQR **9,900,028**, Z_SCORE **9,900,032**. Response body bytes: **9,900,170**
for both; headers/framing are additional. Raw API JSON preserves all individual rows.
The Z_SCORE median is lower in this small observation; these serial samples do not establish a
general end-to-end speed gain. The extra bounded raw-body buffer and JSON tree/serialization
costs also mean that a 48 MB core allocation saving is not a measured 48 MB API heap reduction.

Whole native API process lifetime (startup, warmups and all six samples): baseline peak working
set **1,255,645,184 bytes**, candidate **1,331,478,528 bytes**. Post-run heap used was respectively
**507,375 KiB** and **963,173 KiB**, at different GC points; uncollected garbage is included and
neither snapshot is peak live heap or a per-request bound. The candidate's observed working set
and post-run heap were higher. These observations must accompany the core B/op saving: admission's
raw-body buffer, JSON allocations, added readiness/management startup and GC timing remain relevant.

## Reproduce

```powershell
.\scripts\build-v03.ps1
.\scripts\benchmark-v03.ps1 -Implementation baseline
.\scripts\benchmark-v03.ps1 -Implementation candidate
.\scripts\api-v03.ps1 -Implementation baseline
.\scripts\api-v03.ps1 -Implementation candidate
```

Commit measured source/harness first. The builder uses a fresh pinned archive, separate artifact
directories and mandatory application/harness verification; prior baseline scripts retain their
v0.2.1 source guards. Set a fresh run directory before a new issued experiment; preserve this
published evidence. All bulky fixtures, bodies, binaries and complete logs remain ignored on F:.
Raw JSON/CSV and identities in this directory are the compact published evidence. Container
capacity uses Linux Java **21.0.11+10** under CPU/memory quotas, an explicitly different environment.
