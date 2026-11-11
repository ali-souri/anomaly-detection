param(
    [ValidateSet('IQR','Z_SCORE','MIXED')] [string]$Algorithm = 'MIXED',
    [ValidateSet('arrival','clients')] [string]$Mode = 'arrival',
    [int]$Size = 2000000,
    [int]$Clients = 1,
    [string]$Duration = '60s',
    [string]$BaseUrl = 'http://app:8080',
    [string]$Run = 'single'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$raw = Join-Path $root 'docs/load-tests/raw'
$metrics = Join-Path $root '.cache/v0.3/load-metrics'
New-Item -ItemType Directory -Force $raw,$metrics | Out-Null
$name = "$Run-$Mode-$Algorithm-$Size-c$Clients"
$started=[DateTime]::UtcNow.ToString('o')
& docker run --rm --name anomaly-007-generator --network anomaly-v03_default --cpus 2 --memory 2g --memory-swap 2g -e GOMAXPROCS=2 `
    --mount "type=bind,source=$root/load-tests,target=/suite,readonly" `
    --mount "type=bind,source=$root/.cache/v0.3/fixtures,target=/fixtures,readonly" `
    --mount "type=bind,source=$raw,target=/results" `
    --mount "type=bind,source=$metrics,target=/metrics" `
    -e "ALGORITHM=$Algorithm" -e "MODE=$Mode" -e "SIZE=$Size" -e "CLIENTS=$Clients" -e "DURATION=$Duration" -e "BASE_URL=$BaseUrl" `
    grafana/k6:1.8.2 run --summary-export "/results/$name-summary.json" --out "json=/metrics/$name-metrics.json" /suite/suite.js `
    *> (Join-Path $raw "$name-console.txt")
$exitCode = $LASTEXITCODE
[ordered]@{run=$name;startedUtc=$started;endedUtc=[DateTime]::UtcNow.ToString('o');exitCode=$exitCode;sourceCommit=(& git -C $root rev-parse HEAD);suiteSha256=(Get-FileHash (Join-Path $root 'load-tests/suite.js')).Hash;algorithm=$Algorithm;mode=$Mode;size=$Size;duration=$Duration;clients=$Clients;maxVUs=20;generatorCpu=2;generatorMemoryBytes=2147483648;baseUrl=$BaseUrl} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $raw "$name-run.json")
Write-Host "$name exit=$exitCode; raw timestamps, metrics and summary retained"
exit $exitCode
