param([ValidateSet('baseline','candidate')] [string]$Implementation, [string]$BaseUrl, [string]$Run)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Set-Location $root
if (-not $Run) { $Run=$Implementation }
$directory=Join-Path $root ".cache/010/api-$Run"
New-Item -ItemType Directory -Force $directory | Out-Null
$harness=Join-Path $root '.cache/009/baseline/benchmarks.jar'
$raw=Join-Path $root 'docs/performance/010/raw'
$jar=Join-Path $root ".cache/010/$Implementation/application.jar"
$owned=$null
try {
    if (-not $BaseUrl) {
        $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
        $listener.Start(); $port=$listener.LocalEndpoint.Port; $listener.Stop()
        $options=@('-Xms512m','-Xmx2g','-XX:+UseG1GC',"-Djava.io.tmpdir=$root/.cache/tmp","-Xlog:gc:file=$directory/gc.log:time,uptime,level,tags",'-jar',$jar,"--server.port=$port",'--server.address=127.0.0.1')
        $owned=Start-Process -FilePath (Get-Command java).Source -ArgumentList $options -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$directory/server.log" -RedirectStandardError "$directory/error.log"
        $BaseUrl="http://127.0.0.1:$port"
        $ready=$false
        for ($attempt=0;$attempt -lt 60;$attempt++) {
            if ($owned.HasExited) { throw 'Owned application exited during startup' }
            try { $socket=[Net.Sockets.TcpClient]::new('127.0.0.1',$port); $socket.Dispose(); $ready=$true; break } catch { Start-Sleep -Milliseconds 500 }
        }
        if (-not $ready) { throw 'Owned server did not become ready' }
    }
    & java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$root/.cache/tmp" -cp $harness io.anomaly.benchmark.ApiProbe $BaseUrl $directory (Join-Path $raw "api-$Run.json")
    if ($LASTEXITCODE -ne 0) { throw 'Complete real HTTP response validation failed' }
    if ($owned) {
        $owned.Refresh()
        [ordered]@{pid=$owned.Id;url=$BaseUrl;jarSha256=(Get-FileHash $jar).Hash;jvmOptions=$options;peakWorkingSetBytes=$owned.PeakWorkingSet64;privateBytesAfter=$owned.PrivateMemorySize64;cpuSeconds=$owned.TotalProcessorTime.TotalSeconds} | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $raw "api-$Implementation-process.json")
        & jcmd $owned.Id VM.command_line | Set-Content (Join-Path $raw "api-$Implementation-vm.txt")
        & jcmd $owned.Id GC.heap_info | Set-Content (Join-Path $raw "api-$Implementation-heap.txt")
    }
} finally {
    if ($owned) {
        $process=Get-CimInstance Win32_Process -Filter "ProcessId=$($owned.Id)"
        if ($process -and $process.CommandLine.Contains($jar)) { Stop-Process -Id $owned.Id }
    }
}

