# 008 actual ordering — representation refactor

1. Read/preserve 007; starting root clean verify passed **59 unit/55 HTTP** with old production.
   Log: .cache/logs/008-starting-green.log. Explicit 007 artifacts were saved before source edits.
2. Execute six warmed JFR diagnostics on unchanged 007 image. Initial numeric JFR name failed
   startup and is retained; it is not a functional RED or accepted recording. Stack evidence
   assigns 42.73–45.96% of IQR Java samples to sorting. Declare the one-workspace hypothesis.
3. Add two mathematical/signed-zero/ordering unit tests and one real HTTP property-order/signed-zero
   test before changing IqrDetector. **61 unit/56 HTTP GREEN against original boxed sort**:
   mvnw.cmd -B spotless:apply clean verify, .cache/logs/008-pre-refactor-green.log.
4. Change only local sort scratch and median array access, preserving primitive arithmetic guards,
   original Double partitions and full eager immutable results. Repeat clean verify: same 61/56,
   LINE296/0, BRANCH134/0, strict JavaDoc/format/lint GREEN. Docker build independently repeats it.
5. Declare/execute paired two-fork 2M measurements, not a fabricated functional RED: IQR mean
   657.289→201.915ms/op, small B/op change; all independent preflight/API validators pass.
   Capacity still fails all three cases, reported separately from the verified core optimization.

No identity-only tests or probes were added. Existing strict JSON/body/count/admission/error/
Swagger and numerical regressions stay. No threshold/exclusion/test-agent/tmpdir change occurred.
The standalone harness has nine tests, Node two and prior Python four. Three new Python tests
check stack population/ancestor accounting, allocation-weight separation and streaming exports
across a 1 MiB chunk; one checks cgroup counter units. These do not claim application coverage.
The failed optional parser install added no dependency; the delivered analysis is standard-library.
