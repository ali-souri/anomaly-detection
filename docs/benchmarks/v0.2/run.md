# Reproducing the v0.2-line benchmark

Prerequisite: result AD-RES-20261111-005 is COMPLETE. The pinned release is **v0.2.1** at
0bc6e1d2fa2c696ce050e82035e9b3f5b6d411e7. Do not substitute historical v0.2.0 or future v0.3 code.
Read [the predeclared experiment](experiment.md). Run from the canonical project root on F:.

The standalone benchmarks/pom.xml follows the JMH-recommended separate-project approach:
it depends on the real application's uninstrumented plain classes JAR. JMH core and annotation
processor are pinned to 1.37; compiler 3.14.1, shade 3.6.1, JUnit 5.12.2 and Jackson 2.21.4.
The pinned Boot parent supplies compatible supporting versions. Production source/POM are unchanged.

```powershell
# Full application gate, local classifier installation, harness tests/lint/JavaDoc/build:
.\scripts\build-benchmarks.ps1

# 24 analytical/lifecycle checks, then 24 JMH cases, two forks, six iteration means/case:
.\scripts\benchmark-core.ps1

# Run only after JMH ends: starts/stops only its own real app, then verifies six large responses:
.\scripts\benchmark-api.ps1

# Repeat required gates after additions; timing is not a replacement for regression tests:
.\mvnw.cmd clean verify
.\mvnw.cmd -f benchmarks/pom.xml clean verify
```

The core runner records baseline/core tree identity, harness source SHA and actual JAR SHA-256
values before measurement in raw/identity.json. It rejects production differences and uncommitted
harness source. Java agents are rejected at runtime. The root JaCoCo quality gate is deliberately
separate; neither timed JMH forks nor the API JVM inherit the test-plugin agent argument.

Exact JMH invocation (the script resolves the project-local tmpdir):

```powershell
java -Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=F:/projects/anomaly-detection/.cache/tmp `
  -jar benchmarks/target/benchmarks.jar io.anomaly.benchmark.DetectionBenchmark.detect `
  -wi 2 -w 1s -i 3 -r 1s -f 2 -t 1 -prof gc `
  -jvmArgs '-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=F:/projects/anomaly-detection/.cache/tmp' `
  -rf json -rff docs/benchmarks/v0.2/raw/jmh.json
```

Unix hosts can execute the same JMH command without PowerShell backticks after preparing the
same classifier using `./mvnw org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file`
with the arguments in build-benchmarks.ps1. Hardware/OS changes require a new machine record and
new result identity; these measurements are Windows observations, not claimed Unix results.
Use a separate checkout/run label for repetitions; preserve accepted raw evidence and issued result
records rather than silently replacing them with a new run under the same result ID.

Raw machine-readable evidence is in raw/: machine.json, identity.json, correctness.csv, jmh.json,
api.json and api-process.json. Additional derived summaries are described in report.md.
JMH console output, full process logs and massive request/response files stay in ignored .cache.
Compact GC and post-run heap-info copies are retained under raw; no full heap dump is committed. No multi-million-element body is committed. JMH allocation is B/op, not peak heap;
API peak working set is process-wide resident memory, not the Java heap or per-request allocation.

Fixture generation and validation are outside timings. Both real algorithms include their own
input copy, validation, sort/statistics, boxing and complete result partition allocation. The input
is immutable and regenerated at trial setup, never a previously mutated sorted working array.
No concurrency capacity, production SLO, p99 or theoretical complexity is inferred from a few
local samples. Source-loop/sort review supplies the complexity argument.

Generated text has trailing whitespace/EOF blanks normalized for Git. Numeric raw values are
unchanged; byte-for-byte originals remain under ignored .cache for this execution.
