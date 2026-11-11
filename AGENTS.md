# Continuing project rules

- Use one Maven application, Java 17+ source, constructor injection and readable small classes.
- Production packages are rooted at `io.anomaly`, physically under `src/main/java/io/anomaly`.
  Keep lowercase role directories: config; api.controller/dto/mapper/parser/filter/exception;
  core.algorithm/service/registry/model/validation/exception. The statistical core imports no
  Spring, Jackson or OpenAPI types. Tests mirror roles; real HTTP *IT tests belong in
  io.anomaly.integration.api and shared helpers in io.anomaly.support.
- Preserve HTTP contracts, ordering, duplicates, numerical conventions and Swagger isolation.
- Run `./mvnw.cmd clean verify` on Windows or `./mvnw clean verify` on Unix before publication.
  All handwritten production code is covered by combined unit + real HTTP integration execution:
  zero missed LINE and BRANCH counters, no handwritten exclusions, and mandatory nonempty data,
  XML and analyzed-class checks. Keep JaCoCo's test agent and the project tmpdir in both fork argLines.
- Pre-push and CI must use clean verify. Keep all local generated files under this F: project.
- Follow feature → develop (merge commits) → tested master → new release branch/annotated tag.
  Never move existing release refs, rewrite history, bypass hooks or edit issued prompts/results.
- Benchmark handoff: AD-PRM-20261110-003 must use immutable **v0.2.1**, not historical v0.2.0.
  First read docs/results/AD-RES-20261111-005.md and confirm COMPLETE; resolve the exact baseline
  with `git rev-parse 'v0.2.1^{commit}'`. Do not start that prompt automatically from maintenance.
- Timed production/JMH processes must have no active JaCoCo agent. Coverage verification is a
  separate mandatory gate; never disable pre-push/CI gates to obtain benchmark numbers.
- Benchmark 003 has executed on v0.2.1. Before any conditional v0.3 implementation, read
  docs/results/AD-RES-20261110-003.md, docs/benchmarks/v0.2/report.md and the architecture decision.
  The evidence recommends a stateless modular application, allocation reduction and bounded
  admission/load validation. Prompt 007 later supplied a 10 successful 2M FULL requests/second
  target (IQR, Z_SCORE and 50/50 mixed independently), without a latency SLO. Read result 007,
  docs/load-tests/README.md and the updated decision before any follow-up. Keep incomplete
  capacity work on its feature branch; no release or automatic next task until its gates pass.
- Continuation 008 is on feature/v0.3-profile-optimization, based on preserved 007, not develop.
  Read result 008 and docs/performance/008/README.md before further work. Its single production
  change is IQR primitive scratch sorting; strict parsing/FULL output and 007 reference reuse stay.
  Capacity still failed. The profile supports a future bounded-parser experiment, not automatic
  work, extra resources, microservices or more permits as an assumed fix.
