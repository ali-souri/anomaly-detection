$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & git diff --quiet v0.2.1 -- src/main/java src/main/resources
    if ($LASTEXITCODE -ne 0) { throw 'Production source differs from the pinned v0.2.1 baseline' }
    & ./mvnw.cmd -B clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Baseline quality gate failed' }
    & ./mvnw.cmd -B org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file '-Dfile=target/anomaly-detection-0.2.1.jar.original' '-DgroupId=io.anomaly' '-DartifactId=anomaly-detection' '-Dversion=0.2.1' '-Dclassifier=benchmark-baseline' '-Dpackaging=jar' '-DgeneratePom=true'
    if ($LASTEXITCODE -ne 0) { throw 'Baseline plain-JAR installation failed' }
    & ./mvnw.cmd -B -f benchmarks/pom.xml clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Benchmark harness verification failed' }
} finally { Pop-Location }
exit 0
