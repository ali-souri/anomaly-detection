param()
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$cache = Join-Path $root '.cache'
$fixtureBase = Join-Path $cache ('hook-fixture-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$fixture = Join-Path $fixtureBase 'work'
$remote = Join-Path $fixtureBase 'remote.git'
$log = Join-Path $fixtureBase 'evidence.txt'

function Source-State {
    $status = & git -C $root status --porcelain=v1
    $work = & git -C $root diff --binary
    $index = & git -C $root diff --cached --binary
    return (@($status) + @($work) + @($index)) -join "`n"
}

$before = Source-State
if (-not $fixtureBase.StartsWith($cache + [IO.Path]::DirectorySeparatorChar)) {
    throw 'Fixture must stay inside the project cache'
}
New-Item -ItemType Directory -Path $fixtureBase | Out-Null
& git clone --no-hardlinks --single-branch $root $fixture *> $log
if ($LASTEXITCODE -ne 0) { throw 'Fixture clone failed' }
New-Item -ItemType Directory -Path (Join-Path $fixture '.cache') | Out-Null
foreach ($name in @('repository', 'maven')) {
    New-Item -ItemType Junction -Path (Join-Path $fixture ".cache/$name") -Target (Join-Path $cache $name) | Out-Null
}
& (Join-Path $fixture 'scripts/install-hooks.ps1') >> $log
& git init --bare $remote >> $log
if ($LASTEXITCODE -ne 0) { throw 'Fixture remote creation failed' }

Push-Location $fixture
try {
    $formatPath = 'src/main/java/io/anomaly/core/algorithm/FormattingFixture.java'
    [IO.File]::WriteAllText((Join-Path $fixture $formatPath), "package io.anomaly.core.algorithm;`nclass FormattingFixture{}`n")
    & git add -- $formatPath
    & git commit -m 'fixture: malformed formatting must fail' *> (Join-Path $fixtureBase 'format-commit.log')
    if ($LASTEXITCODE -eq 0) { throw 'Formatting failure did not block commit' }
    if (-not (Select-String -Path (Join-Path $fixtureBase 'format-commit.log') -Pattern 'format violations' -Quiet)) {
        throw 'Commit failed for an unexpected reason; inspect fixture log'
    }
    & git rm --cached -- $formatPath >> $log
    Remove-Item -LiteralPath (Join-Path $fixture $formatPath)
    'PASS: formatting failure blocked a real git commit' >> $log

    $partialPath = 'src/main/java/io/anomaly/AnomalyApplication.java'
    $partialAbsolute = Join-Path $fixture $partialPath
    [IO.File]::AppendAllText($partialAbsolute, "`n// staged fixture`n")
    & git add -- $partialPath
    [IO.File]::AppendAllText($partialAbsolute, "// unstaged fixture`n")
    $indexBefore = (& git diff --cached --binary) -join "`n"
    $hashBefore = (Get-FileHash -LiteralPath $partialAbsolute).Hash
    & git commit -m 'fixture: partial staging must fail' *> (Join-Path $fixtureBase 'partial-commit.log')
    if ($LASTEXITCODE -eq 0) { throw 'Partial staging did not block commit' }
    if (-not (Select-String -Path (Join-Path $fixtureBase 'partial-commit.log') -Pattern 'partially staged relevant file' -Quiet)) {
        throw 'Partial-staging rejection was not observed'
    }
    if ($hashBefore -ne (Get-FileHash -LiteralPath $partialAbsolute).Hash -or $indexBefore -ne ((& git diff --cached --binary) -join "`n")) {
        throw 'Pre-commit altered fixture worktree or index'
    }
    & git restore --source=HEAD --staged --worktree -- $partialPath
    'PASS: partial staging rejected and fixture worktree/index unchanged' >> $log

    $testPath = 'src/test/java/io/anomaly/core/algorithm/HookFailureTest.java'
    $test = @'
package io.anomaly.core.algorithm;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class HookFailureTest {
  @Test
  void controlledFailure() {
    fail("CONTROLLED_PUSH_FAILURE");
  }
}
'@
    [IO.File]::WriteAllText((Join-Path $fixture $testPath), $test + "`n")
    & git add -- $testPath
    & git commit -m 'fixture: controlled failing unit test' *> (Join-Path $fixtureBase 'test-commit.log')
    if ($LASTEXITCODE -ne 0) { throw 'Well-formatted failing test could not be committed in fixture' }
    & git push $remote HEAD:refs/heads/probe *> (Join-Path $fixtureBase 'test-push.log')
    if ($LASTEXITCODE -eq 0) { throw 'Failing test did not block push' }
    if (-not (Select-String -Path (Join-Path $fixtureBase 'test-push.log') -Pattern 'CONTROLLED_PUSH_FAILURE' -Quiet)) {
        throw 'Push failed for an unexpected reason; inspect fixture log'
    }
    & git --git-dir=$remote show-ref --verify refs/heads/probe 2>> $log
    if ($LASTEXITCODE -eq 0) { throw 'Rejected push changed fixture remote' }
    'PASS: failing unit test blocked a real git push; remote ref absent' >> $log
} finally {
    Pop-Location
}
if ($before -ne (Source-State)) { throw 'Original project files/index changed during hook verification' }
'PASS: original project worktree/index preserved' >> $log
Get-Content -LiteralPath $log
Write-Host "Fixture evidence: $fixtureBase"
