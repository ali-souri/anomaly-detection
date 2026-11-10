# IQR and registry TDD

Request: AD-PRM-20261110-002. No pre-existing implementation.

Contracts were written first in `IqrDetectorTest` and `DetectionServiceTest`, against minimal
compilable detector/service/registry scaffolds. Hand calculations cover even/odd halves, n=0..3,
both tails, boundaries, duplicates/order, unchanged inputs, defaults and numerical limits.
The registry tests include an additional test-only strategy and duplicate identifier rejection.

RED, 2026-11-10 20:14 UTC:

```powershell
./mvnw.cmd -B spotless:apply test '-Dtest=IqrDetectorTest,DetectionServiceTest'
```

Exit 1, **21 tests, 7 failures, 14 errors, 0 skipped**. The assertions observed missing validation
and duplicate rejection; detection calls raised the explicit `TDD scaffold` exception.
Full local log: `.cache/logs/core-red.log`. Earlier Maven dependency timeouts and one PowerShell
argument-splitting error were infrastructure failures, not statistical RED evidence.

GREEN, 2026-11-10 20:16 UTC: same command, exit 0, **21 tests, 0 failures/errors/skips**.
Full local log: `.cache/logs/core-green.log`.
Shared validation and immutable summary construction were extracted; the same suites stayed green.
Median averaging rescales only on sum overflow, preserving equal subnormal values and large equal
values. Classification scans the original copy, keeping order and duplicates.
