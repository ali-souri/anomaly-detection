param([string]$Run='candidate', [int]$Samples=65)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$file=Join-Path $root "docs/performance/008/raw/$Run-resources.jsonl"
for ($sample=0;$sample -lt $Samples;$sample++) {
    $stamp=[DateTime]::UtcNow.ToString('o')
    $available=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory
    $names=@(& docker ps --format '{{.Names}}' | Where-Object { $_ -match '^anomaly-008-(app-[1-3]|proxy-1|generator)$' })
    if ($available -lt 1572864 -and $names.Count -gt 0) {
        [ordered]@{utc=$stamp;availableHostKiB=$available;safetyStop=$true;ownedContainers=$names} | ConvertTo-Json -Compress | Add-Content $file
        & docker stop -t 3 @names | Out-Null
        throw 'Stopped only task-owned load containers to preserve 1.5 GiB host headroom'
    }
    if ($names.Count -gt 0) {
        & docker stats --no-stream --format '{{json .}}' @names | ForEach-Object {
            [ordered]@{utc=$stamp;availableHostKiB=$available;stats=($_ | ConvertFrom-Json)} | ConvertTo-Json -Compress | Add-Content $file
        }
    }
    Start-Sleep -Seconds 2
}
