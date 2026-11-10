$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
Push-Location $root
try {
    $env:TEMP = Join-Path $root '.cache/tmp'
    $env:TMP = $env:TEMP
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $listener.Stop()
    $jar = Join-Path $root 'target/anomaly-detection-0.2.1.jar'
    $java = (Get-Command java).Source
    New-Item -ItemType Directory -Force .cache/benchmark-api | Out-Null
    $options = @('-Xms512m','-Xmx2g','-XX:+UseG1GC',"-Djava.io.tmpdir=$env:TEMP",'-Xlog:gc:file=.cache/benchmark-api/server-gc.log:time,uptime,level,tags','-jar',$jar,"--server.port=$port",'--server.address=127.0.0.1','--spring.main.banner-mode=off')
    $server = Start-Process -FilePath $java -ArgumentList $options -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput .cache/benchmark-api/server.log -RedirectStandardError .cache/benchmark-api/server-error.log -PassThru
    try {
        $ready = $false
        for ($attempt=0; $attempt -lt 60; $attempt++) {
            if ($server.HasExited) { throw 'Project application exited during startup' }
            try { $socket=[Net.Sockets.TcpClient]::new('127.0.0.1',$port); $socket.Dispose(); $ready=$true; break } catch { Start-Sleep -Milliseconds 500 }
        }
        if (-not $ready) { throw 'Project application did not bind its selected port' }
        & java -Xms512m -Xmx2g -XX:+UseG1GC "-Djava.io.tmpdir=$env:TEMP" -cp benchmarks/target/benchmarks.jar io.anomaly.benchmark.ApiProbe "http://127.0.0.1:$port" .cache/benchmark-api docs/benchmarks/v0.2/raw/api.json
        if ($LASTEXITCODE -ne 0) { throw 'Real API measurement/validation failed' }
        $server.Refresh()
        [ordered]@{pid=$server.Id;port=$port;serverJvmOptions=$options;peakWorkingSetBytes=$server.PeakWorkingSet64;privateBytesAfter=$server.PrivateMemorySize64;totalCpuSeconds=$server.TotalProcessorTime.TotalSeconds;scope='Whole application lifetime including startup/warmups/all samples; working set is not Java heap'} | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 docs/benchmarks/v0.2/raw/api-process.json
        & jcmd $server.Id VM.command_line > .cache/benchmark-api/vm-command-line.txt
        & jcmd $server.Id GC.heap_info > .cache/benchmark-api/heap-after.txt
    } finally {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($server.Id)"
        if ($process -and $process.CommandLine.Contains($jar)) { Stop-Process -Id $server.Id }
    }
} finally { Pop-Location }
exit 0
