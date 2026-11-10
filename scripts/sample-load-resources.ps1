param([string]$Run, [int]$Samples = 30)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$output=Join-Path $root "docs/load-tests/raw/$Run-resources.jsonl"
for ($sample=0;$sample -lt $Samples;$sample++) {
    $stamp=[DateTime]::UtcNow.ToString('o')
    & docker stats --no-stream --format '{{json .}}' anomaly-v03-app-1 anomaly-v03-app-2 anomaly-v03-app-3 anomaly-v03-proxy-1 anomaly-007-generator 2>$null |
        ForEach-Object { [ordered]@{utc=$stamp;stats=($_ | ConvertFrom-Json)} | ConvertTo-Json -Compress | Add-Content $output }
    Start-Sleep -Seconds 2
}
