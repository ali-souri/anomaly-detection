param([ValidateSet('screens','final')][string]$Phase='screens',[ValidateSet('B','C')][string]$Selected='B')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path;Set-Location $root
$raw="$root/docs/performance/010/raw";$cache="$root/.cache/010"
$env:HTTP_PORT='18080';$env:MAX_ACTIVE='3';$env:APP_CPUS='3.75';$env:PROXY_CPUS='1'
$files=@('-p','anomaly-010','-f','compose.yaml','-f','compose.capacity.yaml','-f','compose.010.yaml')
$common='-XX:+UseG1GC -Djava.io.tmpdir=/app/tmp -XX:+PrintFlagsFinal -Xlog:gc,gc+init:stdout:time,uptime,level,tags'
$options=@{A="-Xms512m -Xmx2048m $common";B="-Xms512m -Xmx2560m $common";C="-Xms512m -Xmx2560m -XX:MaxGCPauseMillis=500 $common"}
function Capture([string]$Run,[string]$Position,[int]$Count) {
 $nodes=@()
 foreach ($n in 1..$Count) {
  $name="anomaly-010-app-$n";$state=(& docker inspect $name --format '{{json .State}}' | ConvertFrom-Json)
  $node=[ordered]@{name=$name;state=$state}
  if ($state.Running) { $node.cpu=@(docker exec $name cat /sys/fs/cgroup/cpu.stat);$node.currentBytes=(docker exec $name cat /sys/fs/cgroup/memory.current);$node.peakBytes=(docker exec $name cat /sys/fs/cgroup/memory.peak);$node.memoryEvents=@(docker exec $name cat /sys/fs/cgroup/memory.events);$node.commands=@(docker top $name -eo pid,args) }
  $nodes+=$node
 }
 [ordered]@{utc=[DateTime]::UtcNow.ToString('o');availableHostKiB=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory;apps=$nodes} | ConvertTo-Json -Depth 7 > "$raw/$Run-$Position.json"
}
function Run-Trial([string]$Run,[string]$Variant,[int]$Count,[string]$Algorithm,[string]$Mode,[string]$Duration,[bool]$Validate) {
 if ((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory -lt 1572864) {throw 'Host headroom unavailable'}
 if (Test-Path "$raw/$Run-run.json") {throw 'Refusing to overwrite an executed trial'}
 $env:TUNING_JVM_OPTIONS=$options[$Variant]
 $sampler=$null
 & docker compose @files up -d --no-build --pull never --scale "app=$Count" --wait --wait-timeout 90
 if ($LASTEXITCODE -ne 0) {throw 'Startup/readiness failed'}
 try {
  & docker compose @files config > "$raw/$Run-compose.yaml"
  $jar=(& docker exec anomaly-010-app-1 sha256sum /app/app.jar).Split(' ')[0]
  if ($jar -ne 'de69e4871d3b94605bb436aca49ff10dfd42684f110316ed21ad0aab2f082aac') {throw 'JAR identity mismatch'}
  [ordered]@{run=$Run;variant=$Variant;replicas=$Count;image=(& docker inspect anomaly-010-app-1 --format '{{.Image}}');jarSha256=$jar;jvmOptions=$options[$Variant];experimentSource=(& git rev-parse HEAD);applicationPublication='bc18d635bcf8359794b0f75aaedbf63d57f481fa';actualCommands=@(docker top anomaly-010-app-1 -eo pid,args)} | ConvertTo-Json > "$raw/$Run-runtime.json"
  Capture $Run 'before' $Count
  $sampler=Start-Process -FilePath (Get-Command pwsh).Source -ArgumentList @('-NoProfile','-File',"$root/scripts/sample010.ps1",'-Run',$Run) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$cache/$Run-sampler.log" -RedirectStandardError "$cache/$Run-sampler-error.log"
  if ($Validate) { & ./scripts/api010.ps1 -Implementation baseline -Run $Run -BaseUrl http://127.0.0.1:18080; if ($LASTEXITCODE -ne 0) {throw 'Exhaustive FULL validation failed'} }
  & ./scripts/run010-load.ps1 -Run $Run -Algorithm $Algorithm -Mode $Mode -Clients 3 -Duration $Duration -BaseUrl http://proxy:8080 -Network anomaly-010_default
  $code=$LASTEXITCODE
  Capture $Run 'after' $Count
  if ($code -notin 0,99) {throw "Load exited $code; retained failure state"}
  if (Test-Path "$raw/$Run-safety-stop.json") {throw 'Safety guard stopped the trial'}
  $response=Invoke-WebRequest -Method Post -Uri http://127.0.0.1:18080/api/v1/anomaly-detection -ContentType application/json -Body '{"algorithm":"IQR","data":[]}'
  [ordered]@{utc=[DateTime]::UtcNow.ToString('o');status=[int]$response.StatusCode;body=$response.Content} | ConvertTo-Json > "$raw/$Run-recovery.json"
 } finally {
  if ($sampler -and -not $sampler.HasExited) {Stop-Process -Id $sampler.Id}
  & docker compose @files logs --no-color > "$cache/$Run-server.log"
  & docker compose @files ps -a --format json > "$raw/$Run-final-state.json"
  & docker compose @files down --timeout 25
 }
}
if ($Phase -eq 'screens') {
 foreach ($variant in 'A','B','C') {
  try {Run-Trial "screen-$variant" $variant 1 MIXED clients 25s $true} catch { $_ | Out-String | Set-Content "$raw/screen-$variant-failure.txt"; break }
  & python -B scripts/safety010.py "screen-$variant"
  if ($LASTEXITCODE -ne 0) {break}
 }
} else {
 foreach ($algorithm in 'IQR','Z_SCORE','MIXED') { foreach ($variant in @('A',$Selected)) {Run-Trial "$variant-$algorithm" $variant 3 $algorithm arrival 60s $false} }
}
