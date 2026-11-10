# Benchmark harness TDD — AD-PRM-20261110-003

The released production code is unchanged. Benchmark prerequisite is v0.2.1 from result 005.
The separate benchmarks project depends on its plain classes JAR with a local classifier.

Before implementing fixtures/checks, compiling contract tests were run with:
`./mvnw.cmd -B -f benchmarks/pom.xml spotless:apply test`.
RED: 7 cases, 1 failure, 6 scaffold errors, 0 skipped. Log: .cache/logs/003-harness-red.log.
They specify exact histograms, repeatable seeds, duplicate multiplicity, input immutability,
ordered arrangement, invalid declarations, analytical correctness and rejection of wrong
statistics/partition ordering even when total counts are unchanged.

GREEN: `./mvnw.cmd -B -f benchmarks/pom.xml spotless:apply clean verify`, 7 passing unit cases,
format/lint and strict public harness JavaDoc. Log: .cache/logs/003-harness-verify.log.
Inherited shade transformer merging and generated JMH source lint were corrected as build
configuration issues, not counted as statistical RED. All authored sources remain checked;
only JMH-generated annotation-processing sources are outside authored-source lint/JavaDoc.

The root application clean verify independently retains 46 unit / 47 real HTTP integration
cases and exact 100% combined LINE/BRANCH coverage. Timed Java commands do not use the test
agent; both JMH setup and the API probe reject active Java agents at runtime.
