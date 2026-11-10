# HTTP/API TDD

Request: AD-PRM-20261110-002. Core suites were already green.

Tests use `@SpringBootTest(RANDOM_PORT)` and Java's real `HttpClient`. Algorithms are not mocked.
The only test-only endpoint deliberately throws to verify unexpected-error sanitization.

RED, 2026-11-10 20:16 UTC:

```powershell
./mvnw.cmd -B spotless:apply verify
```

Exit 1: **21 unit tests passed; 35 integration tests ran with 31 failures, 1 error, 0 skipped**.
The stub endpoint returned empty 200 responses instead of the required JSON/validation behavior;
framework errors lacked the required Problem Details contract. Documentation mode checks already
passed using the bootstrap profile configuration. Log: `.cache/logs/api-red.log`.

The additional generated-schema contract test was then run before adding API annotations:

```powershell
./mvnw.cmd -B spotless:apply verify '-Dit.test=DevDocsEnabledIT'
```

Its result and subsequent GREEN verification are recorded below after observation.
