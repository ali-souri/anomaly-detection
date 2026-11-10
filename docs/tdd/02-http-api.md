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

Generated-schema RED: exit 1, **21 unit tests passed; 2 selected integration tests, 1 failure,
0 errors/skips**. The scaffold had no operation summary/schema. Log: `.cache/logs/openapi-red.log`.

After implementing parsing, mapping, error handling and annotations, all behavioral suites passed.
The first full run stopped on 9 strict JavaDoc warnings; this was a documentation gate failure,
not a test failure. Explicit constructor comments and interface method descriptions fixed them.

GREEN, 2026-11-10 20:22 UTC:

```powershell
./mvnw.cmd -B spotless:apply verify
```

Exit 0, **21 unit tests and 36 integration tests, 0 failures/errors/skips**, 0 Checkstyle violations,
format check passed, JavaDoc generated with doclint and warnings treated as failures.
Log: `.cache/logs/api-green-final.log`; JavaDoc: `target/reports/apidocs/index.html`.
