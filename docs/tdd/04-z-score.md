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

The first implementation run had 1 overly exact assertion: Welford returned mean
5.249999999999999 for the hand-calculated 5.25. The assertion now allows 1e-14, without changing
or rounding production arithmetic. Exact representable score boundaries still use exact checks.

Core GREEN, 2026-11-10 20:31 UTC:

```powershell
./mvnw.cmd -B spotless:apply test
```

Exit 0: **39 unit tests (18 Z-Score + 21 existing), 0 failures/errors/skips**.
Log: `.cache/logs/zscore-core-green-final.log`.

HTTP/schema RED, 2026-11-10 20:32 UTC, before registering the new detector or exposing its DTO:

```powershell
./mvnw.cmd -B spotless:apply verify
```

Exit 1: **39 unit tests passed; 47 integration tests, 6 failures, 1 error, 0 skipped**.
Real HTTP requests got UNSUPPORTED_ALGORITHM and the generated schema had no two-way
discriminator. Existing IQR behavior and generic validation remained green.
Log: `.cache/logs/zscore-api-red.log`.

Full GREEN, 2026-11-10 20:35 UTC, same unfiltered `spotless:apply verify` command:
exit 0, **39 unit tests and 47 integration tests, 0 failures/errors/skips**.
Formatting, Checkstyle (0 violations) and strict JavaDoc all passed.
Log: `.cache/logs/zscore-api-green.log`.
The schema test checks oneOf, explicit discriminator mappings, canonical one-value enums,
and absence of irrelevant statistics. Mixed concurrent requests exercise both real detectors.
