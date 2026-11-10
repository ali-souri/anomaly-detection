$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
$env:TEMP=Join-Path $root '.cache/tmp'
$env:TMP=$env:TEMP
& git diff --quiet HEAD -- src/main pom.xml benchmarks/src benchmarks/pom.xml
if ($LASTEXITCODE -ne 0) { throw 'Commit source and harness before building measured artifacts' }
& ./mvnw.cmd -B clean verify
if ($LASTEXITCODE -ne 0) { throw 'Candidate application quality gate failed' }
$archiveRoot=Join-Path $root ('.cache/v0.3/archive-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$archiveZip="$archiveRoot.zip"
& git archive --format=zip "--output=$archiveZip" v0.2.1
if ($LASTEXITCODE -ne 0) { throw 'Pinned baseline archive failed' }
Expand-Archive -LiteralPath $archiveZip -DestinationPath $archiveRoot
New-Item -ItemType Directory -Force "$archiveRoot/.cache/tmp",.cache/v0.3/baseline,.cache/v0.3/candidate | Out-Null
$mavenExe=(Get-ChildItem .cache/maven/wrapper/dists -Recurse -Filter mvn.cmd | Select-Object -First 1).FullName
& $mavenExe -B -f "$archiveRoot/pom.xml" "-Dmaven.repo.local=$root/.cache/repository" clean verify
if ($LASTEXITCODE -ne 0) { throw 'Immutable baseline quality gate failed' }
Copy-Item "$archiveRoot/target/anomaly-detection-0.2.1.jar.original" .cache/v0.3/baseline/detector.jar
Copy-Item "$archiveRoot/target/anomaly-detection-0.2.1.jar" .cache/v0.3/baseline/application.jar
Copy-Item target/anomaly-detection-0.3.0-SNAPSHOT.jar.original .cache/v0.3/candidate/detector.jar
Copy-Item target/anomaly-detection-0.3.0-SNAPSHOT.jar .cache/v0.3/candidate/application.jar
& ./mvnw.cmd -B org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file '-Dfile=target/anomaly-detection-0.3.0-SNAPSHOT.jar.original' '-DgroupId=io.anomaly' '-DartifactId=anomaly-detection' '-Dversion=0.3.0-SNAPSHOT' '-Dclassifier=benchmark-candidate' '-Dpackaging=jar' '-DgeneratePom=true'
if ($LASTEXITCODE -ne 0) { throw 'Candidate installation failed' }
& ./mvnw.cmd -B -f benchmarks/pom.xml '-Ddetector.version=0.3.0-SNAPSHOT' '-Ddetector.classifier=benchmark-candidate' clean verify
if ($LASTEXITCODE -ne 0) { throw 'Separate harness quality gate failed' }
Copy-Item benchmarks/target/benchmarks.jar .cache/v0.3/candidate/benchmarks.jar
& java -Xmx512m "-Djava.io.tmpdir=$env:TEMP" -cp .cache/v0.3/candidate/benchmarks.jar io.anomaly.benchmark.GenerateLoadFixtures .cache/v0.3/fixtures
if ($LASTEXITCODE -ne 0) { throw 'Fixture generation failed' }
