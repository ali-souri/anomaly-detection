# Direct Swagger resource isolation

Final review added direct versioned/versionless WebJar URL assertions to the existing disabled-mode
real HTTP test. The WebJar version is read from the dependency's own metadata rather than guessed.

RED, 2026-11-10 20:42 UTC:

```powershell
./mvnw.cmd -B spotless:apply verify '-Dit.test=HttpApiIT#swaggerAndJsonStayDisabledOutsideDevEvenWithTheFlag'
```

Exit 1: **39 unit tests passed; 1 integration test failed, 0 errors/skips**. A direct Swagger
WebJar index returned 200 instead of 404, despite the normal Swagger/OpenAPI routes being disabled.
Log: `.cache/logs/swagger-resource-check.log`.

This is an API-only application, so base configuration should not publish general static resources.
The dev profile can opt into those resources using the same explicit SWAGGER_ENABLED flag as
Swagger UI and OpenAPI. A backport must preserve the existing v0.1.0 tag and use a new patch tag.

GREEN on the v0.2 development line, 2026-11-10 20:43 UTC:

```powershell
./mvnw.cmd -B spotless:apply verify
```

Exit 0, **39 unit tests and 47 integration tests, no failures/errors/skips**. Direct WebJar
URLs now return 404 outside dev; both enabled dev documentation endpoints remain available.
Log: `.cache/logs/swagger-resources-green.log`.

The same compiling regression was cherry-picked onto a patch branch from the released v0.1
master. Clean backport RED used `./mvnw.cmd -B clean verify` with the same `-Dit.test=...` selector:
**21 unit tests passed, 1 integration test failed (200 instead of 404), no skips**.
Log: `.cache/logs/v01-backport-red-clean.log`. No compiled v0.2 classes were used.

Backport GREEN, 2026-11-10 20:46 UTC: `./mvnw.cmd -B spotless:apply clean verify`, exit 0,
**21 unit tests and 36 integration tests, 0 failures/errors/skips**, format/lint/strict JavaDoc
passed. Mermaid parsed with 14 production classes and all 13 local links resolved.
Log: `.cache/logs/v011-backport-green.log`. The patch is v0.1.1; v0.1.0 is not moved.
