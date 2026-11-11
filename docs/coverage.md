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

Historical v0.2.1 scope: all 25 production Java files under io.anomaly, including configuration, API, statistics and
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

## Verified 007 v0.3 candidate

The feature candidate has 29 production Java files, 30 XML class entries including the bounded
body reader's anonymous HttpInputMessage, 27 executable classes and the same three pure interfaces.
Combined 59 unit / 55 genuine HTTP integration tests pass with no failures/errors/skips.
Fresh exact counters: LINE 294 covered / 0 missed, BRANCH 132 / 0, INSTRUCTION 1377 / 0,
METHOD 68 / 0, CLASS 27 / 0 and COMPLEXITY 136 / 0. Built-in generated-code filtering is unchanged;
there are still no handwritten exclusions. Both forked agent/tmpdir settings and nonempty coverage
checks remain mandatory. The separate benchmark's nine tests and Node/Python tooling are not
included in these application coverage counters. The capacity target failed independently, so
this positive coverage evidence does not authorize a v0.3 release. See result 007 for exact CI/source.

008 preserves those checks and adds two numerical/signed-zero unit cases plus one genuine HTTP
case: **61 unit / 56 integration**. Exact fresh counters: LINE296/0, BRANCH134/0, INSTRUCTION1380/0,
METHOD68/0, CLASS27/0 and COMPLEXITY137/0. Source/XML scope and built-in filters are unchanged;
the additional loop contributes two branch outcomes. No handwritten exclusions or reduced gates.

009 retains the same exact gates: **67 unit / 101 real HTTP** tests, LINE364/0, BRANCH172/0,
INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0. It has 30 handwritten production
Java files, 32 XML class entries (including private Data and anonymous HttpInputMessage), and
29 executable classes. No exclusions or generated markers were added. Characterization tests
first passed on the unchanged 008 parser. Separate harness/tooling remain outside this denominator.

009 retains the same exact gates: **67 unit / 101 real HTTP** tests, LINE364/0, BRANCH172/0,
INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0. It has 30 handwritten production
Java files, 32 XML class entries (including private Data and anonymous HttpInputMessage), and
29 executable classes. No exclusions or generated markers were added. Characterization tests
first passed on the unchanged 008 parser. Separate harness/tooling remain outside this denominator.
