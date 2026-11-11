param(
    [string]$Run,
    [ValidateSet('IQR','Z_SCORE','MIXED')] [string]$Algorithm='MIXED',
    [ValidateSet('arrival','clients')] [string]$Mode='arrival',
    [int]$Clients=1,
    [string]$Duration='60s',
    [string]$BaseUrl='http://app:8080',
    [string]$Network='anomaly-v03_default'
)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$raw=Join-Path $root 'docs/performance/009/raw'
$local=Join-Path $root '.cache/009/load'
New-Item -ItemType Directory -Force $raw,$local | Out-Null
$started=[DateTime]::UtcNow.ToString('o')
& docker run --rm --name anomaly-009-generator --network $Network --cpus 2 --memory 2g --memory-swap 2g -e GOMAXPROCS=2 `
    --mount "type=bind,source=$root/load-tests,target=/suite,readonly" `
    --mount "type=bind,source=$root/.cache/v0.3/fixtures,target=/fixtures,readonly" `
    --mount "type=bind,source=$raw,target=/results" `
    --mount "type=bind,source=$local,target=/local" `
    -e "ALGORITHM=$Algorithm" -e "MODE=$Mode" -e SIZE=2000000 -e "CLIENTS=$Clients" -e "DURATION=$Duration" -e "BASE_URL=$BaseUrl" `
    grafana/k6:1.8.2 run --summary-export "/results/$Run-summary.json" --out "json=/local/$Run-metrics.json" /suite/suite.js `
    *> (Join-Path $local "$Run-console.txt")
$status=$LASTEXITCODE
[ordered]@{run=$Run;startedUtc=$started;endedUtc=[DateTime]::UtcNow.ToString('o');exitCode=$status;sourceCommit=(& git -C $root rev-parse HEAD);suiteSha256=(Get-FileHash "$root/load-tests/suite.js").Hash;algorithm=$Algorithm;mode=$Mode;duration=$Duration;clients=$Clients;maxVUs=20;generatorCpu=2;generatorMemoryBytes=2147483648;baseUrl=$BaseUrl;network=$Network} |
    ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $raw "$Run-run.json")
Write-Output "$Run exit=$status"
exit $status
