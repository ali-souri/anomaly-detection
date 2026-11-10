$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    git config --local core.hooksPath .githooks
    if ($LASTEXITCODE -ne 0) { throw 'Unable to configure local hooks' }
    Write-Host 'Installed local Git hooks. Format explicitly with .\mvnw.cmd spotless:apply, review, then stage.'
} finally {
    Pop-Location
}
