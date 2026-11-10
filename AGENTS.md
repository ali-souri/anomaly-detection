# Continuing project rules

- Use one Maven application, Java 17+ source, constructor injection and readable small classes.
- Production packages are rooted at `io.anomaly`, physically under `src/main/java/io/anomaly`.
  Keep lowercase role directories: config; api.controller/dto/mapper/parser/exception;
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
