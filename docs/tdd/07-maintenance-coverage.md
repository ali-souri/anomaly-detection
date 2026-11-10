# AD-PRM-20261111-005 coverage evidence

Existing behavior was refactored into io.anomaly before new tests were added. The unchanged suite
ran 39 unit and 47 real HTTP integration cases. JavaDoc initially caught exception types no longer
in the same package; explicit imports corrected the links. That correction is preserved in history.

First measured combined JaCoCo 0.8.16 XML: LINE covered=242, missed=4 (242/246,
98.3739837398374%); BRANCH covered=110, missed=6 (110/116, 94.82758620689655%).
INSTRUCTION 1165/1180, METHOD 52/53, CLASS 22/22. No earlier coverage percentage is assumed.
Initial XML is saved locally in .cache/evidence005/initial.xml.

Exact-gate RED: `./mvnw.cmd -B spotless:apply clean verify`, exit 1, with all 39/47 tests passing.
JaCoCo reported 4 missed lines and 6 missed branches against maximum zero.
Log: .cache/logs/005-coverage-red.log. This is a coverage-gate RED, not invented test-first
history for existing production code. New tests target the actual reported contracts/paths.

GREEN: .cache/logs/005-coverage-green.log, clean verify exit 0, 46 unit / 47 integration cases,
zero failures/errors/skips. XML LINE covered=246 missed=0; BRANCH covered=112 missed=0.
Additional counters: INSTRUCTION 1174/0 missed, METHOD 53/0, CLASS 22/0, COMPLEXITY 111/0.

Two semantics-preserving simplifications explain the smaller branch denominator (116 → 112):
the private number parser only receives existing fields or array elements, whose JSON null is a
NullNode, so Java-null checking there was unreachable. Public parse(null) is still rejected/tested.
In finite Welford arithmetic the updated mean is between the previous mean and the sample, so
delta and deltaAfter have the same sign or zero; their product cannot be negative. Overflow is
still rejected by Inputs.finite and reachable underflow/zero-variance guards are retained/tested.
No protections for reachable inputs, exclusions, annotations or probes were added to hide gaps.

Controlled gate fixtures: `./scripts/verify-coverage.ps1`, exit 0; evidence is under
.cache/coverage-fixture-20261111-012605. All 46/47 tests passed in the fixture but an uncovered
application class made clean verify fail and blocked a real push to a local bare remote; its ref
remained absent. Separate mandatory guard checks rejected missing exec, empty exec, missing XML,
and an XML fixture with no analyzed classes. Invalid fixture data stayed in that ignored clone.
The script explicitly returns success only after all expected failures are observed.
