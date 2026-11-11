param([ValidateSet('baseline','candidate')] [string]$Implementation)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
& git diff --quiet HEAD -- src/main pom.xml benchmarks/src scripts/benchmark008.ps1
if ($LASTEXITCODE -ne 0) { throw 'Commit measured source/tooling before timing' }
if ("$env:JAVA_TOOL_OPTIONS $env:JDK_JAVA_OPTIONS" -match 'javaagent|StartFlightRecording') { throw 'Timed JVM environment contains instrumentation' }
$plain=Join-Path $root ".cache/008/$Implementation/detector.jar"
$harness=Join-Path $root '.cache/008/baseline/benchmarks.jar'
$raw=Join-Path $root 'docs/performance/008/raw'
$tempDir=Join-Path $root '.cache/tmp'
$source=$(if ($Implementation -eq 'baseline') { '33d4e2f3936723cbc4ec28cd03e4428944cecc60' } else { & git rev-parse HEAD })
[ordered]@{implementation=$Implementation;sourceCommit=$source;harnessSourceCommit='331cb861ee6fbbbbae77aac4e82878647cb2eec4';executionScriptCommit=(& git rev-parse HEAD);coreTree=(& git rev-parse "$($source):src/main/java/io/anomaly/core");detectorSha256=(Get-FileHash $plain).Hash;harnessSha256=(Get-FileHash $harness).Hash;utc=[DateTime]::UtcNow.ToString('o');jvmEnvironment=@{JAVA_TOOL_OPTIONS=$env:JAVA_TOOL_OPTIONS;JDK_JAVA_OPTIONS=$env:JDK_JAVA_OPTIONS};scope='No agent or active JFR recording; matched separate native JVMs'} | ConvertTo-Json -Depth 4 > "$raw/$Implementation-identity.json"
$classpath="$plain;$harness"
& java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$tempDir" -cp $classpath io.anomaly.benchmark.CheckFixtures "$raw/$Implementation-correctness.csv"
if ($LASTEXITCODE -ne 0) { throw 'Independent fixture validation failed' }
$forkOptions='-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=' + $tempDir.Replace('\','/')
& java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$tempDir" -cp $classpath org.openjdk.jmh.Main 'io.anomaly.benchmark.DetectionBenchmark.detect' `
    -p size=2000000 -p kind=MIXED -p seed=20261110 -wi 3 -w 1s -i 3 -r 1s -f 2 -t 1 -prof gc -jvmArgs $forkOptions -rf json -rff "$raw/$Implementation-jmh.json" `
    *> ".cache/logs/008-$Implementation-jmh.log"
if ($LASTEXITCODE -ne 0) { throw 'Paired JMH failed' }
