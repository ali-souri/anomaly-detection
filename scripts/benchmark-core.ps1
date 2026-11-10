$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Push-Location $root
try {
    $env:TEMP = Join-Path $root '.cache/tmp'
    $env:TMP = $env:TEMP
    & git diff --quiet v0.2.1 -- src/main/java src/main/resources
    if ($LASTEXITCODE -ne 0) { throw 'Released production source was modified' }
    & git diff --quiet HEAD -- benchmarks/src benchmarks/pom.xml scripts/benchmark-core.ps1 scripts/benchmark-api.ps1
    if ($LASTEXITCODE -ne 0) { throw 'Commit the harness before measuring it' }
    $raw = Join-Path $root 'docs/benchmarks/v0.2/raw'
    [ordered]@{
        baselineCommit = (& git rev-parse 'v0.2.1^{commit}')
        baselineCoreTree = (& git rev-parse 'v0.2.1:src/main/java/io/anomaly/core')
        measuredCoreTree = (& git rev-parse 'HEAD:src/main/java/io/anomaly/core')
        harnessCommit = (& git rev-parse HEAD)
        plainJarSha256 = (Get-FileHash target/anomaly-detection-0.2.1.jar.original -Algorithm SHA256).Hash
        applicationJarSha256 = (Get-FileHash target/anomaly-detection-0.2.1.jar -Algorithm SHA256).Hash
        harnessJarSha256 = (Get-FileHash benchmarks/target/benchmarks.jar -Algorithm SHA256).Hash
        recordedUtc = [DateTime]::UtcNow.ToString('o')
        agentPolicy = 'No active Java agent in timed JVMs; runtime assertions enabled'
    } | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $raw 'identity.json')
    & java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$env:TEMP" -cp benchmarks/target/benchmarks.jar io.anomaly.benchmark.CheckFixtures (Join-Path $raw 'correctness.csv')
    if ($LASTEXITCODE -ne 0) { throw 'Fixture preflight failed' }
    $forkOptions = '-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=' + $env:TEMP.Replace('\','/')
    & java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$env:TEMP" -jar benchmarks/target/benchmarks.jar 'io.anomaly.benchmark.DetectionBenchmark.detect' -wi 2 -w 1s -i 3 -r 1s -f 2 -t 1 -prof gc -jvmArgs $forkOptions -rf json -rff (Join-Path $raw 'jmh.json') *> .cache/logs/003-jmh.log
    if ($LASTEXITCODE -ne 0) { throw 'JMH execution failed; inspect .cache/logs/003-jmh.log' }
    Write-Host 'JMH matrix completed; machine-readable measurements saved.'
} finally { Pop-Location }
exit 0
