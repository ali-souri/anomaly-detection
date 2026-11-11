$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
$image='sha256:e86a3dfeb02bc7c2be559b8a6f79602fdec6d4df41ec52e58d901acfd31cde64'
$network='anomaly-008-profile'
& docker network create $network | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Profile network already exists or cannot be created' }
try {
    foreach ($active in 1,2,3) {
        foreach ($algorithm in 'IQR','Z_SCORE') {
            $run="before-$algorithm-active$active"
            $jvm='-Xms512m -Xmx2g -XX:+UseG1GC -Djava.io.tmpdir=/app/tmp -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=name=profile008,settings=profile,delay=20s,duration=15s,maxsize=128m,filename=/app/tmp/profile.jfr -Xlog:gc:stdout:time,uptime,level,tags'
            & docker run -d --name anomaly-008-profile --network $network --network-alias app --cpus 3.75 --memory 3g --memory-swap 3g -e "JAVA_TOOL_OPTIONS=$jvm" -e "DETECTION_LIMITS_MAX_ACTIVE=$active" $image | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Profile application failed to start' }
            try {
                $ready=$false
                for ($attempt=0;$attempt -lt 60;$attempt++) {
                    if ((& docker inspect anomaly-008-profile --format '{{.State.Running}}') -ne 'true') { throw 'Profile JVM exited during startup; see retained log' }
                    & docker exec anomaly-008-profile wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness 2>$null
                    if ($LASTEXITCODE -eq 0) { $ready=$true; break }
                    Start-Sleep -Milliseconds 500
                }
                if (-not $ready) { throw 'Profile readiness timeout' }
                $beforeCpu=@(& docker exec anomaly-008-profile cat /sys/fs/cgroup/cpu.stat)
                & ./scripts/run008-load.ps1 -Run $run -Algorithm $algorithm -Mode clients -Clients $active -Duration 30s -Network $network
                if ($LASTEXITCODE -ne 0) { throw 'Diagnostic load failed' }
                & docker cp anomaly-008-profile:/app/tmp/profile.jfr ".cache/008/profiles/$run.jfr"
                if ($LASTEXITCODE -ne 0) { throw 'Warmed recording was not written' }
                & docker logs anomaly-008-profile *> ".cache/008/profiles/$run-server.log"
                [ordered]@{run=$run;sourceCommit='33d4e2f3936723cbc4ec28cd03e4428944cecc60';image=$image;active=$active;cpus=3.75;containerBytes=3221225472;jvmOptions=$jvm;containerStart=(& docker inspect anomaly-008-profile --format '{{.State.StartedAt}}');cpuBefore=$beforeCpu;cpuAfter=@(& docker exec anomaly-008-profile cat /sys/fs/cgroup/cpu.stat);memoryPeakBytes=(& docker exec anomaly-008-profile cat /sys/fs/cgroup/memory.peak);memoryEvents=@(& docker exec anomaly-008-profile cat /sys/fs/cgroup/memory.events);recordingBytes=(Get-Item ".cache/008/profiles/$run.jfr").Length;recordingSha256=(Get-FileHash ".cache/008/profiles/$run.jfr").Hash} |
                    ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 "docs/performance/008/raw/$run-profile.json"
            } finally {
                & docker logs anomaly-008-profile *> ".cache/008/profiles/$run-server.log"
                & docker inspect anomaly-008-profile --format '{{json .State}}' > ".cache/008/profiles/$run-container-state.json"
                & docker stop -t 10 anomaly-008-profile | Out-Null
                & docker rm anomaly-008-profile | Out-Null
            }
            & jfr summary ".cache/008/profiles/$run.jfr" > "docs/performance/008/raw/$run-jfr-summary.txt"
            & jfr print --json --stack-depth 128 --events 'jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ObjectAllocationSample,jdk.GarbageCollection,jdk.GCHeapSummary,jdk.ThreadPark,jdk.SocketRead,jdk.SocketWrite,jdk.JavaMonitorEnter,jdk.CPULoad,jdk.ContainerCPUThrottling,jdk.ActiveRecording,jdk.ActiveSetting' ".cache/008/profiles/$run.jfr" > ".cache/008/profiles/$run-events.json"
            if ($LASTEXITCODE -ne 0) { throw 'JFR extraction failed' }
            Write-Output "Recorded and exported $run"
        }
    }
} finally { & docker network rm $network | Out-Null }
