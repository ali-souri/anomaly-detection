# Executed v0.2-line benchmark

**BENCHMARK_EXECUTED / COMPLETE.** Baseline is v0.2.1, not historical v0.2.0. The full predeclared 24-case core matrix and six large real-API samples ran successfully. No algorithm behavior or released ref changed.

## Identity and method

- Baseline: **0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7** (annotated v0.2.1).
- Measured harness: **7dfb173a0d1cef3a30a4ae6e24998614da282fe0**. Later analysis/report commits are separate.
- Baseline and measured core Git trees both equal **db8f6f19d1be30346ed73d9d8d1c5f7797daf97b**. All production source/resources matched the release before/after measurement.
- Actual executed JAR hashes are in [identity](raw/identity.json). Rebuild ZIP timestamps can change JAR hashes without changing source; those hashes identify the executed bytes.
- Core benchmark uses real released AnomalyDetector implementations from the plain baseline JAR, following [OpenJDK JMH separate-project guidance](https://github.com/openjdk/jmh). No replacement calculation is timed.
- Intel i7-12700K, 12 physical / 20 logical processors; 34,028,830,720 usable RAM bytes, about 15.38 GiB free at declaration; Windows 11 Enterprise build 26200; Temurin 21.0.10+7, Java 17 target.
- G1, Xms512m/Xmx2g, one benchmark thread, two forks; each fork has 2 Ãƒâ€” 1 s warmup and 3 Ãƒâ€” 1 s measurement. There are six measured iteration means per case, 144 total. Active Java agents are rejected; actual VM options show no JaCoCo.
- No builds or API load ran during JMH. No CPU affinity, CPU quota, global tuning or host isolation was applied. This hybrid-core desktop can migrate work between cores.

Generation/setup and analytical checks are excluded. Each detect operation includes the baseline input validation/copies, IQR copy-sort or Welford passes, boxing, ordered partition construction and immutable result copying. JMH consumes the returned result. Trial input is immutable and reused in original order; IQR always sorts its own fresh copy.

## Correctness before accepting time

The seeded histogram design and full matrix were [declared before timing](experiment.md). MIXED repeats 0..97 and -1000,+1000 in each 100 values, shuffled with seed 20261110. DUPLICATES replaces the 98 ordinary values with zero. DESCENDING orders the MIXED histogram; it is a favorable regular arrangement, not a claimed worst case.

Independent expectations: MIXED/DESCENDING Q1=23.5, Q3=73.5, IQR=50, bounds [-51.5,148.5], mean=47.53, population variance=20830.3491. DUPLICATES Q1=Q3=IQR=bounds=mean=0 and population variance=20000. Each default flags exactly the two extremes per 100 (2%). This designed agreement is not a claim that the algorithms generally agree.

All 24 preflight rows passed exact count/value/order checks and before/after fingerprints; the immutable input also prevents accidental in-place sorting. JMH repeats these checks per trial. IQR statistics are exact for these fixtures; Z-Score tolerance is 1e-8 absolute + 1e-10 Ãƒâ€” abs(expected). The full API responses were streamed and every partition value/order/count, statistic, default, status, warning and field set checked. Each 2M response retained 40,000 anomalies and 1,960,000 normalValues. All normalValues were transmitted.

## Core results: MIXED scaling

JMH average ms/op Ã‚Â± its reported 99.9% confidence half-width. Allocation is profiler B/op converted to MiB/op (1 MiB=1,048,576 bytes). The six iteration means are not six request-tail samples.

| Numbers | IQR ms/op Ã‚Â± error | Z_SCORE ms/op Ã‚Â± error | IQR MiB/op | Z_SCORE MiB/op |
| ---: | ---: | ---: | ---: | ---: |
| 10,000 | 1.316 Ã‚Â± 0.057 | 0.142 Ã‚Â± 0.012 | 0.596 | 0.508 |
| 100,000 | 15.147 Ã‚Â± 1.207 | 1.533 Ã‚Â± 0.261 | 5.509 | 4.687 |
| 1,000,000 | 232.204 Ã‚Â± 27.428 | 51.154 Ã‚Â± 8.378 | 56.208 | 48.485 |
| 2,000,000 | 548.925 Ã‚Â± 70.886 | 155.754 Ã‚Â± 28.444 | 115.954 | 100.507 |

## Two-million-value arrangement sensitivity

| Fixture | IQR ms/op Ã‚Â± error | Z_SCORE ms/op Ã‚Â± error | IQR / Z_SCORE MiB/op |
| --- | ---: | ---: | ---: |
| MIXED | 548.925 Ã‚Â± 70.886 | 155.754 Ã‚Â± 28.444 | 115.954 / 100.507 |
| DUPLICATES | 182.139 Ã‚Â± 74.872 | 164.514 Ã‚Â± 51.820 | 115.951 / 100.507 |
| DESCENDING | 111.388 Ã‚Â± 13.234 | 109.509 Ã‚Â± 16.265 | 115.828 / 100.507 |

MIXED IQR was about 3.52Ãƒâ€” Z-Score in this run. DESCENDING times were similar and confidence intervals overlap; do not claim a winner there or from the smaller DUPLICATES difference. IQR changed strongly with arrangement while allocated bytes stayed similar. This supports sorting work as a material MIXED IQR cost, without fitting an asymptotic model to timings.

Both algorithms allocate about 100Ã¢â‚¬â€œ116 MiB per 2M operation even when sorting becomes favorable. This excludes initial fixture generation but includes full result allocation. Source review shows the retained boxed input plus multiple O(n) reference arrays and new boxed partition values. Input/output memory must be budgeted together; B/op is total churn, not peak live heap.

For 2M MIXED the GC profiler recorded summed collection time of 234 ms (IQR) and 731 ms (Z-Score) over the six measured iterations, with 19 and 49 collections. These sums are not per-operation GC time. Full raw per-iteration values are retained; fast cases execute more operations and can trigger more collections in the same nominal interval.

## Real HTTP FULL responses

One 10k untimed warmup per algorithm preceded three serial 2M samples. IQR ran before Z-Score. Times include request-file read, loopback HTTP, server parse/calculate/serialize, complete receive and response-file write; they exclude fixture/JSON generation and post-receipt validation. File-system caching and client work matter.

| Algorithm | Samples | Median ms | MinÃ¢â‚¬â€œmax ms | Request bytes | Response bytes |
| --- | ---: | ---: | ---: | ---: | ---: |
| IQR | 3 | 730.644 | 707.636Ã¢â‚¬â€œ828.071 | 9,900,028 | 9,900,170 |
| Z_SCORE | 3 | 405.794 | 362.315Ã¢â‚¬â€œ424.416 | 9,900,032 | 9,900,170 |

Byte counts are JSON body bytes from files, excluding HTTP headers/framing. Every sample returned 200 and passed complete validation. The individual samples (especially declining Z-Score times) show warmup/variability; three observations do not establish p99 or steady-state production latency. The API/core gap supports additional JSON/transport/allocation cost as an inference. Separate JVMs/input lifecycles and limited warmup mean the gap is not a precise per-phase attribution; no CPU-stack profile was taken.

The project API process peaked at **1,179.594 MiB working set**, with 1,159.395 MiB private bytes observed after the run. The post-run heap snapshot showed **487.449 MiB used / 942 MiB committed**, max heap 2 GiB. Working set includes native/code/mappings and is not peak Java heap. These observations cover startup, warmups and all samples together, not one request. G1 logs include humongous allocation and young/mixed collections. The process was stopped after completion; actual VM command-line and heap evidence are retained.

## Complexity, limits and decision

The real IQR implementation validates/copies, sorts a copy using Collections.sort, then classifies all original values: general O(n log n) time and O(n) extra memory. Ordered runs can be cheaper. Z-Score uses validation/copy, Welford population statistics, classification and result copies: O(n) time and O(n) total extra memory. JSON input/output adds O(n) bytes/work and multiple live objects. The observed non-linear timing ratios do not change those source-based complexity claims.

No latency SLO or concurrency target was supplied. Only one dataset at a time was measured, not capacity under simultaneous datasets. Single-seed discrete histograms are not representative of every production distribution, million-distinct-value sort or adversarial TimSort case. Short warmups/iterations, six iteration means, only three API samples, hybrid desktop scheduling, GC, OS file caches, loopback rather than remote networking and a single JDK/heap limit external validity. No production-readiness claim is made.

**Recommendation:** retain a stateless modular application; reduce per-dataset allocation as the primary measurable v0.3 improvement, and bound admitted work before expensive parsing. Independent algorithm services are not justified by this matrix. See [the architecture decision](../../decisions/v0.3-architecture.md) for required versus conditional work. Microservices do not reduce sorting complexity. Never average chunk quartiles to derive global quartiles or classify chunks using unrelated local statistics.

## Reproduction and evidence

[Run guide and exact commands](run.md). [Declared machine](raw/machine.json); [source/JAR identity](raw/identity.json); [24-case preflight](raw/correctness.csv); [complete JMH JSON](raw/jmh.json); [derived JMH CSV](raw/jmh-summary.csv); [individual API samples](raw/api.json); [API summary](raw/api-summary.csv); [process observations](raw/api-process.json); [actual API VM arguments](raw/api-vm-command-line.txt); [post-run heap](raw/api-heap-after.txt); [API GC log](raw/api-gc.txt). Compact raw evidence is below 200 KB; multi-million-element bodies remain ignored on F:.

Run scripts/summarize-benchmarks.py to regenerate CSVs. It rejects incomplete/duplicate case matrices, missing forks/iterations, active agents and unvalidated/missing API samples. Root regression gate remains 46 unit / 47 real HTTP integration tests with exact 100% LINE/BRANCH coverage; the separate authored harness has seven meaningful unit tests, format/lint and public JavaDoc. These correctness gates are separate from timing. Final verification/publication identities are in AD-RES-20261110-003; this report does not retrospectively identify later documentation commits as measured code.

Prompt AD-PRM-20261110-003; prerequisite AD-RES-20261111-005. No frozen release/tag or algorithm behavior was modified.
