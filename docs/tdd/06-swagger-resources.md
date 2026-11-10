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
