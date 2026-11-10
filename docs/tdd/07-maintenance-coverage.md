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
