# CI and final regression review

The workflow repeats full Maven `verify` on push and PR, and now supports explicit
`workflow_dispatch` for a visible manual run when automatic events do not produce one.
This configuration change adds no statistical behavior and claims no statistical RED cycle.

Regression review strengthened existing tests for zero-threshold IQR tails, the framework Allow
header on 405, and both JSON/YAML documentation routes. These were already implemented behaviors;
the added assertions passed against the existing production code.

```powershell
./mvnw.cmd -B spotless:apply verify
```

2026-11-10 20:36 UTC: exit 0, **39 unit tests, 47 real HTTP integration tests,
0 failures/errors/skips**, clean formatting/Checkstyle and strict JavaDoc.
Log: `.cache/logs/ci-feature-verify.log`.

Initial remote inspection after the v0.1 push: Actions enabled, zero workflow runs/status checks
listed. GitHub initially selected develop as the default on the first multi-ref push; the default
was aligned to master as requested. Visibility remains private. Remote execution status is reported
from the actual API in the final result, independently of local success.
