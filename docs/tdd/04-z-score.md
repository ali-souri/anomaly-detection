# Z-Score TDD

Request AD-PRM-20261110-002. v0.1 was fully verified, tagged and pushed before this extension.

Core RED, 2026-11-10 20:29 UTC:

```powershell
./mvnw.cmd -B spotless:apply test '-Dtest=ZScoreDetectorTest'
```

Exit 1, **18 tests, 6 failures, 12 scaffold errors, 0 skipped**.
Log: `.cache/logs/zscore-core-red.log`. The compiling tests specify population statistics,
strict score boundaries, defaults/custom thresholds, small/empty data, zero variance,
ordered duplicate retention, immutable results, large-offset stability and arithmetic limits.
The original ten-value IQR sample explicitly has no default three-sigma Z-Score outlier.

GREEN and the separate real HTTP/schema extension evidence follow after observation.
