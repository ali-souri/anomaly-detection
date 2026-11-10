# Development

Use Java 17+ (the verified workstation uses Temurin 21.0.10). Run commands from the repository root.
The wrapper pins Maven 3.9.16 and verifies its distribution SHA-256. Spring Boot 3.5.16
is a stable Java 17-compatible line; springdoc 2.10.0 explicitly supports Boot 3.5.
The parent pins transitive dependency versions. Spotless 3.0.0 uses google-java-format 1.24.0;
Checkstyle plugin 3.6.0 uses Checkstyle 10.26.1 with complementary structural rules.
Surefire/Failsafe are both pinned to 3.5.4; JavaDoc plugin is 3.11.2.

Version sources checked on 2026-11-10:
[Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[springdoc compatibility](https://springdoc.org/v2/),
[Maven stable releases](https://maven.apache.org/download.cgi),
[Wrapper](https://maven.apache.org/tools/wrapper/download.cgi),
[Spotless](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md),
[Checkstyle plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/).
Artifacts are resolved from official Maven Central. Maven 3.9 is the maintained previous stable
line and avoids introducing a new build-tool minor during this small implementation.

## Commands

| Purpose | Windows PowerShell | Unix |
| --- | --- | --- |
| Install local hooks | `./scripts/install-hooks.ps1` | `bash scripts/install-hooks.sh` |
| Explicitly fix formatting | `./mvnw.cmd spotless:apply` | `./mvnw spotless:apply` |
| Check format/lint | `./mvnw.cmd spotless:check checkstyle:check` | `./mvnw spotless:check checkstyle:check` |
| Unit tests | `./mvnw.cmd test` | `./mvnw test` |
| Full checks + real HTTP tests + JavaDoc | `./mvnw.cmd verify` | `./mvnw verify` |
| One unit suite | `./mvnw.cmd test '-Dtest=IqrDetectorTest'` | `./mvnw test -Dtest=IqrDetectorTest` |
| Standalone JavaDoc | `./mvnw.cmd javadoc:javadoc` | `./mvnw javadoc:javadoc` |

Review the formatter diff, then stage intended changes yourself. The pre-commit hook never fixes
or stages files. It rejects partially staged relevant files before checking the working tree,
so an unstaged edit cannot disguise the content being committed. It also rejects formatting/lint
problems anywhere in the checked source tree. Hooks require Bash (included with Git for Windows).
The pre-push hook always runs full `verify`, including Surefire unit tests, Failsafe `*IT` real
HTTP tests, format/lint and JavaDoc. No skipping flags are used. CI repeats the same command on
push/PR and supports manual `workflow_dispatch`. Remote status is checked separately; local
success is never used as evidence of a successful GitHub run.
Hook fixture evidence and commands are recorded under [tdd](tdd/).

The wrapper keeps distributions in `.cache/maven`, dependency artifacts in `.cache/repository`,
OS/JVM temporary files in `.cache/tmp`, and outputs in `target`. These are ignored and remain
inside the project on F: on the canonical workstation. The wrapper deliberately starts from its
own directory. It does not change global Maven/Java installations or Git settings.

## Git flow

`master` is the released baseline. Create `develop` from it. Each logical change starts at current
`develop` on `feature/<name>`; merge with `git merge --no-ff`. After the whole release passes
`verify`, merge `develop` into `master` with a merge commit, verify that exact commit, then create
`release/v0.1` (later `release/v0.2`) and annotated `v0.1.0` / `v0.2.0` tags. Never move tags.
Only the empty-remote documentation/.gitignore root commit is a bootstrap exception.
Keep feature heads and merge commits. Push completed verified work without rewriting history.

A late direct-Swagger-resource regression was backported on `hotfix/v01-swagger-resources` from
released master. This patch branch preserves the immutable v0.1.0 tag and adds v0.1.1; it never
imports Z-Score into the v0.1 line. Its history is merged into develop before the v0.2 release.

## TDD

For each behavior, write a compiling contract test, run and capture its intended failure,
implement, then rerun and refactor. Download/build failures are not RED evidence. Actual
commands and results are in `docs/tdd/`; generated full logs stay under `.cache/logs`.
No code predates this execution: both the local folder and remote were empty when inspected.

Use the next prompt to benchmark v0.2 before selecting a v0.3 architecture.

The final release gates run `clean verify` to avoid stale reports. v0.2 has 39 unit test cases
and 47 integration cases; parameterized invocations are counted individually. No tests are skipped.
The Mermaid class diagram was parsed with Mermaid 11.12.0 and its named classes checked against
production source; relative documentation links were checked locally. Parser tooling lives only in
the ignored project `.cache`, alongside other verification artifacts.
