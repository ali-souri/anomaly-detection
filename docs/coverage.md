# Testing and coverage

Unit tests under `src/test/java/io/anomaly` mirror production roles. Real random-port HTTP tests
are in `io.anomaly.integration.api` and use the actual application/algorithms; shared HTTP support
is in `io.anomaly.support`. HttpApiIT starts through the real `AnomalyApplication.main` path.
Surefire discovers `*Test`; Failsafe discovers `*IT`. A failing test or quality check exits nonzero.

JaCoCo **0.8.16** is the stable release, not the documentation site's 0.8.17 snapshot.
[Agent documentation](https://www.jacoco.org/jacoco/trunk/doc/prepare-agent-mojo.html),
[exact checks](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html),
[release history](https://www.jacoco.org/jacoco/trunk/doc/changes.html).
Both forked test plugins use `@{jacocoAgentArgLine}` plus the preserved project-local java.io.tmpdir.
The agent property has an empty default. Both suites append to target/jacoco.exec within one
clean build. Report and checks run at verify, after Failsafe. Pre-push and CI require clean verify.
Convenience/selected runs may leave incomplete or old data; never interpret them as the release gate.

The policy is exactly zero missed LINE and BRANCH counters, not rounded HTML percentages. LINE
means executed source lines; BRANCH means exercised bytecode decision outcomes. They do not count
tests and are not interchangeable with instruction, method or class coverage. Missing/empty execution
data, absent XML or no analyzed application classes fail a separate mandatory Maven guard even when
JaCoCo itself would skip a goal. No handwritten production class is excluded.

Scope: all 25 production Java files under io.anomaly, including configuration, API, statistics and
startup. JaCoCo analyzes 22 executable classes; three pure interfaces have no executable bodies.
Standard built-in filters apply to compiler-generated record accessors/equals/hashCode/toString,
enum helper code and empty private no-arg constructors (Inputs). Handwritten record constructors,
factories and behavior remain in scope. No @Generated, ignore markers or custom filtering is used.

Initial existing-suite measurement: LINE 242 covered / 4 missed of 246 (98.3739837398374%);
BRANCH 110 covered / 6 missed of 116 (94.82758620689655%). Fresh final measurements and exact
release identities are recorded in AD-RES-20261111-005. The GREEN development XML has LINE
246 covered / 0 missed and BRANCH 112 covered / 0 missed, both exactly 100%. The denominator
change comes from two proved redundant conditions, explained in [TDD evidence](tdd/07-maintenance-coverage.md).

Reports after clean verify:

| Artifact | Location |
| --- | --- |
| Unit results | target/surefire-reports/ |
| Integration results | target/failsafe-reports/ |
| Combined HTML | target/site/jacoco/index.html |
| Combined XML / CSV | target/site/jacoco/jacoco.xml and jacoco.csv |
| Execution data | target/jacoco.exec |
| JavaDoc | target/reports/apidocs/index.html (io/anomaly/... class pages) |

Run `./scripts/verify-coverage.ps1` on Windows for isolated negative gate fixtures. It proves an
uncovered class blocks a real push with all tests passing, and rejects missing/empty execution
data, missing XML and an empty analyzed report. Fixtures stay under ignored .cache on F:.
100% exercised lines/branches does not replace meaningful assertions or prove the absence of bugs.

Benchmark prompt AD-PRM-20261110-003 must use v0.2.1 and first read AD-RES-20261111-005.
Timed production/JMH JVMs must have no active JaCoCo agent. Agent injection is confined to test
forks; do not bypass pre-push or CI coverage gates to run a benchmark.
