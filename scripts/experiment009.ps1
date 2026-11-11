param([switch]$SkipDiagnostics)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
$raw=Join-Path $root 'docs/performance/009/raw'
$cache=Join-Path $root '.cache/009'
New-Item -ItemType Directory -Force "$cache/profiles" | Out-Null
$baseline='sha256:26a1bf88cfee5aebb907539488227f083aae105509de4c53a38bfc3a77251df5'
$candidate=(& docker image inspect anomaly-detection:009 --format '{{.Id}}')
if ($LASTEXITCODE -ne 0) { throw 'Candidate missing' }
$env:HTTP_PORT='18080'; $env:MAX_ACTIVE='3'; $env:APP_CPUS='3.75'; $env:PROXY_CPUS='1'
$files=@('-p','anomaly-009','-f','compose.yaml','-f','compose.capacity.yaml','-f','compose.009.yaml')
function Start-Deployment([string]$Image) {
    if ((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory -lt 1572864) { throw 'Insufficient host headroom' }
    $env:PERFORMANCE_IMAGE=$Image
    & docker compose @files up -d --no-build --scale app=3 --wait --wait-timeout 90
    if ($LASTEXITCODE -ne 0) { throw 'Deployment failed' }
}
function Stop-Deployment { & docker compose @files down --timeout 25 }
function Capture([string]$Run,[string]$Phase) {
    $nodes=@()
    foreach ($name in 'anomaly-009-app-1','anomaly-009-app-2','anomaly-009-app-3','anomaly-009-proxy-1') {
        $nodes += [ordered]@{name=$name;cpu=@(& docker exec $name cat /sys/fs/cgroup/cpu.stat);memoryCurrentBytes=(& docker exec $name cat /sys/fs/cgroup/memory.current);memoryPeakBytes=(& docker exec $name cat /sys/fs/cgroup/memory.peak);memoryEvents=@(& docker exec $name cat /sys/fs/cgroup/memory.events)}
    }
    [ordered]@{utc=[DateTime]::UtcNow.ToString('o');availableHostKiB=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory;containers=$nodes} | ConvertTo-Json -Depth 6 > "$raw/$Run-$Phase.json"
}
# Exhaustive validation of BOTH versions and algorithms precedes any timed load.
foreach ($version in 'A','B') {
    $image=if($version -eq 'A'){$baseline}else{$candidate}
    Start-Deployment $image
    try {
        & docker compose @files config > "$raw/$version-compose.yaml"
        & docker inspect anomaly-009-app-1 anomaly-009-app-2 anomaly-009-app-3 anomaly-009-proxy-1 > "$cache/$version-containers.json"
        & docker image inspect $image > "$raw/$version-image.json"
        $digest=(& docker exec anomaly-009-app-1 sha256sum /app/app.jar).Split(' ')[0]
        if ($version -eq 'A' -and $digest -ne 'f23766f4d65e8764b0021f87ead2752aa2607715e985be8b4968f87250d2cfbe') { throw 'Baseline JAR identity mismatch' }
        [ordered]@{version=$version;image=$image;jarSha256=$digest;sourceCommit=(& git rev-parse HEAD);baselinePublication='8d7d41c34c9e2d10e955092239fd87bb91a8acf7';java=@(& docker exec anomaly-009-app-1 java -version 2>&1 | ForEach-Object {"$_"});jvmEnvironment=(& docker inspect anomaly-009-app-1 --format '{{json .Config.Env}}');harnessSha256=(Get-FileHash "$cache/baseline/benchmarks.jar").Hash} | ConvertTo-Json -Depth 5 > "$raw/$version-identity.json"
        & ./scripts/api009.ps1 -Implementation baseline -Run $version -BaseUrl 'http://127.0.0.1:18080'
        if ($LASTEXITCODE -ne 0) { throw 'Exhaustive API validation failed' }
    } finally { Stop-Deployment }
}
if (-not $SkipDiagnostics) {
    & docker network create anomaly-009-profile | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Profile network exists or failed' }
    try {
        foreach ($version in 'A','B') {
            $image=if($version -eq 'A'){$baseline}else{$candidate}
            $run="$version-profile-IQR"
            $jvm='-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=/app/tmp -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=name=profile009,settings=profile,delay=20s,duration=15s,maxsize=128m,filename=/app/tmp/profile.jfr -Xlog:gc:stdout:time,uptime,level,tags'
            & docker run -d --name anomaly-009-profile --network anomaly-009-profile --network-alias app --cpus 3.75 --memory 3g --memory-swap 3g -e "JAVA_TOOL_OPTIONS=$jvm" -e DETECTION_LIMITS_MAX_ACTIVE=3 $image | Out-Null
            try {
                $ready=$false
                for ($attempt=0;$attempt -lt 90;$attempt++) {
                    & docker exec anomaly-009-profile wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness 2>$null
                    if ($LASTEXITCODE -eq 0) {$ready=$true;break}
                    Start-Sleep -Milliseconds 500
                }
                if (-not $ready) {throw 'Profile readiness failed'}
                $before=@(& docker exec anomaly-009-profile cat /sys/fs/cgroup/cpu.stat)
                & ./scripts/run009-load.ps1 -Run $run -Algorithm IQR -Mode clients -Clients 3 -Duration 30s -Network anomaly-009-profile
                if ($LASTEXITCODE -ne 0) {throw 'Profile load failed'}
                & docker cp anomaly-009-profile:/app/tmp/profile.jfr "$cache/profiles/$run.jfr"
                if ($LASTEXITCODE -ne 0) {throw 'Profile recording missing'}
                [ordered]@{run=$run;image=$image;sourceCommit=(& git rev-parse HEAD);active=3;clients=3;cpus=3.75;containerBytes=3221225472;jvmOptions=$jvm;containerStart=(& docker inspect anomaly-009-profile --format '{{.State.StartedAt}}');cpuBefore=$before;cpuAfter=@(& docker exec anomaly-009-profile cat /sys/fs/cgroup/cpu.stat);memoryPeakBytes=(& docker exec anomaly-009-profile cat /sys/fs/cgroup/memory.peak);memoryEvents=@(& docker exec anomaly-009-profile cat /sys/fs/cgroup/memory.events);recordingBytes=(Get-Item "$cache/profiles/$run.jfr").Length;recordingSha256=(Get-FileHash "$cache/profiles/$run.jfr").Hash} | ConvertTo-Json -Depth 5 > "$raw/$run-profile.json"
            } finally {
                & docker logs anomaly-009-profile *> "$cache/profiles/$run-server.log"
                & docker stop -t 10 anomaly-009-profile | Out-Null
                & docker rm anomaly-009-profile | Out-Null
            }
        }
    } finally { & docker network rm anomaly-009-profile | Out-Null }
}
foreach ($algorithm in 'IQR','Z_SCORE','MIXED') {
    foreach ($version in 'A','B') {
        $run="$version-$algorithm"
        $image=if($version -eq 'A'){$baseline}else{$candidate}
        Start-Deployment $image
        $sampler=$null
        try {
            Capture $run 'before'
            $sampler=Start-Process -FilePath (Get-Command pwsh).Source -ArgumentList @('-NoProfile','-File',"$root/scripts/sample009.ps1",'-Run',$run,'-Samples','60') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$cache/$run-sampler.log" -RedirectStandardError "$cache/$run-sampler-error.log"
            & ./scripts/run009-load.ps1 -Run $run -Algorithm $algorithm -Duration 60s -BaseUrl http://proxy:8080 -Network anomaly-009_default
            $loadStatus=$LASTEXITCODE
            Capture $run 'after'
            if ($loadStatus -notin 0,99) {throw "Capacity generator failed: $loadStatus"}
        } finally {
            if ($sampler -and -not $sampler.HasExited) {Stop-Process -Id $sampler.Id}
            & docker compose @files logs --no-color > "$cache/$run-server.log"
            Stop-Deployment
        }
    }
}
