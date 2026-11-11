param([string]$Run, [int]$Samples = 30)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$output=Join-Path $root "docs/load-tests/raw/$Run-resources.jsonl"
for ($sample=0;$sample -lt $Samples;$sample++) {
    $stamp=[DateTime]::UtcNow.ToString('o')
    $names=@(& docker ps --format '{{.Names}}' | Where-Object { $_ -match '^anomaly-v03-(app-[1-3]|proxy-1)$|^anomaly-007-generator$' })
    if ($names.Count -gt 0) {
        & docker stats --no-stream --format '{{json .}}' @names |
            ForEach-Object { [ordered]@{utc=$stamp;stats=($_ | ConvertFrom-Json)} | ConvertTo-Json -Compress | Add-Content $output }
    }
    Start-Sleep -Seconds 2
}
