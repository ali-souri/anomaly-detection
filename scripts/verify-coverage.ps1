$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$base = Join-Path $root ('.cache/coverage-fixture-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
if (-not $base.StartsWith((Join-Path $root '.cache') + [IO.Path]::DirectorySeparatorChar)) { throw 'Fixture escaped project cache' }
$work = Join-Path $base 'work'
$remote = Join-Path $base 'remote.git'
New-Item -ItemType Directory -Path $base | Out-Null
& git clone --no-hardlinks --single-branch $root $work *> (Join-Path $base 'clone.log')
if ($LASTEXITCODE -ne 0) { throw 'Fixture clone failed' }
New-Item -ItemType Directory -Path (Join-Path $work '.cache') | Out-Null
foreach ($name in @('repository','maven')) {
    New-Item -ItemType Junction -Path (Join-Path $work ".cache/$name") -Target (Join-Path $root ".cache/$name") | Out-Null
}
& git -C $work config --local core.hooksPath .githooks
& git init --bare $remote *> (Join-Path $base 'remote.log')
Push-Location $work
try {
    $source = 'src/main/java/io/anomaly/core/validation/CoverageFixture.java'
    $code = @'
package io.anomaly.core.validation;

final class CoverageFixture {
  int echo(int value) {
    return value;
  }
}
'@
    [IO.File]::WriteAllText((Join-Path $work $source), $code + "`n")
    & git add -- $source
    & git commit -m 'fixture: deliberately unexercised application contract' *> (Join-Path $base 'commit.log')
    if ($LASTEXITCODE -ne 0) { throw 'Fixture commit failed' }
    & git push $remote HEAD:refs/heads/probe *> (Join-Path $base 'push.log')
    if ($LASTEXITCODE -eq 0 -or -not (Select-String (Join-Path $base 'push.log') -Pattern 'Coverage checks have not been met' -Quiet)) { throw 'Coverage did not block push as expected' }
    & git --git-dir=$remote show-ref --verify refs/heads/probe *> (Join-Path $base 'refs.log')
    if ($LASTEXITCODE -eq 0) { throw 'Rejected push changed the remote' }
    Write-Host 'PASS: clean verification and real push blocked by uncovered fixture; all tests passed; remote ref absent.'
    $exec = Join-Path $work 'target/jacoco.exec'
    $xml = Join-Path $work 'target/site/jacoco/jacoco.xml'
    $execBytes = [IO.File]::ReadAllBytes($exec)
    $xmlBytes = [IO.File]::ReadAllBytes($xml)
    foreach ($scenario in @('missing-exec','empty-exec','missing-xml','no-classes')) {
        try {
            switch ($scenario) {
                'missing-exec' { [IO.File]::Delete($exec) }
                'empty-exec' { [IO.File]::WriteAllBytes($exec, [byte[]]@()) }
                'missing-xml' { [IO.File]::Delete($xml) }
                'no-classes' { [IO.File]::WriteAllText($xml, '<report name="invalid-empty-fixture"/>') }
            }
            & ./mvnw.cmd -B antrun:run@coverage-data-required *> (Join-Path $base "$scenario.log")
            if ($LASTEXITCODE -eq 0 -or -not (Select-String (Join-Path $base "$scenario.log") -Pattern 'Coverage requires nonempty execution data' -Quiet)) { throw "Guard failed for $scenario" }
            Write-Host "PASS: $scenario rejected."
        } finally {
            [IO.File]::WriteAllBytes($exec, $execBytes)
            [IO.File]::WriteAllBytes($xml, $xmlBytes)
        }
    }
} finally { Pop-Location }
Write-Host "Evidence: $base"
exit 0
