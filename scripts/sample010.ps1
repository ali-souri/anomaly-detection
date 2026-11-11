param([string]$Run,[int]$Samples=90)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$file=Join-Path $root "docs/performance/010/raw/$Run-resources.jsonl"
$high=@{}
for ($i=0;$i -lt $Samples;$i++) {
 $stamp=[DateTime]::UtcNow.ToString('o');$free=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory
 $names=@(docker ps --format '{{.Names}}' | Where-Object {$_ -match '^anomaly-010-(app-[1-3]|proxy-1|generator)$'})
 $reason=$null;$nodes=@()
 foreach ($name in @($names | Where-Object {$_ -match '-app-'})) {
  $data=@(docker exec $name sh -c 'cat /sys/fs/cgroup/memory.current /sys/fs/cgroup/memory.peak /sys/fs/cgroup/memory.events; cat /proc/[0-9]*/status')
  if ($LASTEXITCODE -ne 0) {$reason="Application exited: $name";continue}
  $current=[long]$data[0];$peak=[long]$data[1];$events=@($data | Where-Object {$_ -match '^(max|oom|oom_kill|oom_group_kill) '})
  $status=$data -join "`n";$java=[regex]::Match($status,'(?ms)^Name:\s+java\n.*?^VmHWM:\s+(\d+) kB.*?^VmRSS:\s+(\d+) kB')
  $nodes += [ordered]@{name=$name;currentBytes=$current;peakBytes=$peak;memoryEvents=$events;javaHighWaterKiB=$java.Groups[1].Value;javaRssKiB=$java.Groups[2].Value}
  if ($current -ge 3060164198) {$high[$name]=1+[int]$high[$name]}else{$high[$name]=0}
  if ($current -ge 3156800963 -or $high[$name] -ge 2) {$reason="Container memory ceiling approached: $name"}
  if (@($events | Where-Object {($_ -split ' ')[1] -ne '0'}).Count) {$reason="Container max/OOM event: $name"}
 }
 if ($free -lt 1572864) {$reason='Host free RAM below1.5GiB'}
 [ordered]@{utc=$stamp;availableHostKiB=$free;apps=$nodes;stopReason=$reason} | ConvertTo-Json -Depth 5 -Compress | Add-Content $file
 if ($reason -and $names.Count) {
  [ordered]@{utc=$stamp;reason=$reason;names=$names} | ConvertTo-Json > "$root/docs/performance/010/raw/$Run-safety-stop.json"
  docker inspect @names > "$root/.cache/010/$Run-guard-state.json"
  docker stop -t 3 @names | Out-Null
  exit 2
 }
 if ($i % 3 -eq 0 -and $names.Count) {docker stats --no-stream --format '{{json .}}' @names > "$root/.cache/010/$Run-stats-$i.jsonl"}
 Start-Sleep -Seconds 1
}
