param([ValidateSet('baseline','candidate')] [string]$Implementation)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
& git diff --quiet HEAD -- src/main benchmarks/src benchmarks/pom.xml pom.xml scripts/benchmark-v03.ps1
if ($LASTEXITCODE -ne 0) { throw 'Commit all measured source and harness before timing' }
$plain = Join-Path $root ".cache/v0.3/$Implementation/detector.jar"
$harness = Join-Path $root '.cache/v0.3/candidate/benchmarks.jar'
$raw = Join-Path $root 'docs/benchmarks/v0.3/raw'
$classpath = "$plain;$harness"
$tempDir = Join-Path $root '.cache/tmp'
[ordered]@{
    implementation=$Implementation
    baselineCommit=(& git rev-parse 'v0.2.1^{commit}')
    sourceAndHarnessCommit=(& git rev-parse HEAD)
    candidateCoreTree=(& git rev-parse 'HEAD:src/main/java/io/anomaly/core')
    measuredCoreTree=$(if ($Implementation -eq 'baseline') { & git rev-parse 'v0.2.1:src/main/java/io/anomaly/core' } else { & git rev-parse 'HEAD:src/main/java/io/anomaly/core' })
    detectorJarSha256=(Get-FileHash $plain -Algorithm SHA256).Hash
    harnessJarSha256=(Get-FileHash $harness -Algorithm SHA256).Hash
    classpath=$classpath
    utc=[DateTime]::UtcNow.ToString('o')
} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $raw "$Implementation-identity.json")
& java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$tempDir" -cp $classpath io.anomaly.benchmark.CheckFixtures (Join-Path $raw "$Implementation-correctness.csv")
if ($LASTEXITCODE -ne 0) { throw 'Independent full-result validation failed' }
$forkOptions = '-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=' + $tempDir.Replace('\','/')
& java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$tempDir" -cp $classpath org.openjdk.jmh.Main `
    'io.anomaly.benchmark.DetectionBenchmark.detect' -p size=2000000 -p kind=MIXED -p seed=20261110 `
    -wi 2 -w 1s -i 3 -r 1s -f 2 -t 1 -prof gc -jvmArgs $forkOptions -rf json -rff (Join-Path $raw "$Implementation-jmh.json") `
    *> (Join-Path $root ".cache/logs/007-$Implementation-jmh.log")
if ($LASTEXITCODE -ne 0) { throw 'JMH failed; inspect the implementation log' }
