# Isolated hook verification

Request AD-PRM-20261110-002. Verified on Windows with Git Bash, 2026-11-10 20:23 UTC.

```powershell
./scripts/verify-hooks.ps1
```

The script clones the committed API feature into a new directory under `.cache`, installs
the tracked local hooks, and uses a new local bare remote. Only the fixture has intentionally
broken files/tests. The main worktree/index are snapshotted and compared afterwards.

Observed output (exit 0):

```text
PASS: formatting failure blocked a real git commit
PASS: partial staging rejected and fixture worktree/index unchanged
PASS: failing unit test blocked a real git push; remote ref absent
PASS: original project worktree/index preserved
```

- A staged `FormattingFixture.java` with `class FormattingFixture{}` caused Spotless to reject
  `git commit`. The fixture file was then removed from the fixture index/worktree.
- A staged edit plus a different unstaged edit in `AnomalyApplication.java` was rejected before
  Maven ran. The fixture file hash and staged diff were identical before/after the rejection.
  The fixture version was restored from its own HEAD.
- A formatted `HookFailureTest` was committed in the fixture. `git push` ran full `verify` and
  Surefire observed `CONTROLLED_PUSH_FAILURE`; the push failed and `refs/heads/probe` was absent
  in the local fixture remote. The failing test remains only in the ignored fixture.

Fixture: `.cache/hook-fixture-20261110-235249/` (workstation local time in directory name).
Logs: `format-commit.log`, `partial-commit.log`, `test-commit.log`, `test-push.log`, `evidence.txt`.
Main summary log: `.cache/logs/hook-verification.log`.
The cache junctions share downloaded artifacts within this same F: project; no global files or
settings were modified. Actual remote release pushes also run the positive full-verification hook.
