$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
$env:HTTP_PORT='18080'; $env:MAX_ACTIVE='3'; $env:APP_CPUS='3.75'; $env:PROXY_CPUS='1'; $env:PERFORMANCE_IMAGE='anomaly-detection:008'
$files=@('-p','anomaly-008','-f','compose.yaml','-f','compose.capacity.yaml','-f','compose.008.yaml','-f','compose.008.profile.yaml')
$run='diagnostic-scaled-IQR'
& docker compose @files up -d --scale app=3 --wait --wait-timeout 90
if ($LASTEXITCODE -ne 0) { throw 'Scaled diagnostic startup failed' }
try {
    & docker compose @files config > "docs/performance/008/raw/$run-compose.yaml"
    $beforeCpu=@(& docker exec anomaly-008-app-1 cat /sys/fs/cgroup/cpu.stat)
    & ./scripts/run008-load.ps1 -Run $run -Algorithm IQR -Mode arrival -Duration 35s -BaseUrl http://proxy:8080 -Network anomaly-008_default
    # The 600-success threshold belongs to 60-second capacity runs; this short diagnostic may exit 99.
    if ($LASTEXITCODE -notin 0,99) { throw 'Scaled diagnostic did not execute' }
    & docker cp anomaly-008-app-1:/app/tmp/scaled.jfr ".cache/008/profiles/$run.jfr"
    if ($LASTEXITCODE -ne 0) { throw 'Scaled recording missing' }
    [ordered]@{run=$run;sourceCommit='0ba3803';image=(& docker image inspect anomaly-detection:008 --format '{{.Id}}');active=3;cpus=3.75;containerBytes=3221225472;profiledReplica=1;replicas=3;containerStart=(& docker inspect anomaly-008-app-1 --format '{{.State.StartedAt}}');cpuBefore=$beforeCpu;cpuAfter=@(& docker exec anomaly-008-app-1 cat /sys/fs/cgroup/cpu.stat);memoryPeakBytes=(& docker exec anomaly-008-app-1 cat /sys/fs/cgroup/memory.peak);memoryEvents=@(& docker exec anomaly-008-app-1 cat /sys/fs/cgroup/memory.events);recordingBytes=(Get-Item ".cache/008/profiles/$run.jfr").Length;recordingSha256=(Get-FileHash ".cache/008/profiles/$run.jfr").Hash;settings='profile; delay=25s; duration=15s; maxsize=128m; stackdepth=128'} |
        ConvertTo-Json -Depth 4 > "docs/performance/008/raw/$run-profile.json"
    & docker compose @files logs --no-color > ".cache/008/$run-server.log"
} finally {
    & docker compose @files down --timeout 25
}
& jfr summary ".cache/008/profiles/$run.jfr" > "docs/performance/008/raw/$run-jfr-summary.txt"
& jfr print --json --stack-depth 128 --events 'jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ObjectAllocationSample,jdk.GarbageCollection,jdk.GCHeapSummary,jdk.ThreadPark,jdk.SocketRead,jdk.SocketWrite,jdk.JavaMonitorEnter,jdk.CPULoad,jdk.ContainerCPUThrottling,jdk.ActiveRecording,jdk.ActiveSetting' ".cache/008/profiles/$run.jfr" > ".cache/008/profiles/$run-events.json"
