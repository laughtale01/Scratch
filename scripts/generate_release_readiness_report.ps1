param(
  [string]$PlanPath = '.\docs\plans\2026-03-03_gallery-auth-detailed-implementation-plan.md',
  [string]$OutDir = '.\docs\testing\executions'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (!(Test-Path $OutDir)) {
  New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

function Invoke-Check([string]$name, [string]$scriptPath, [string[]]$args = @()) {
  $allArgs = @('-ExecutionPolicy', 'Bypass', '-File', $scriptPath) + $args
  $output = & powershell @allArgs 2>&1
  $exitCode = $LASTEXITCODE
  return [pscustomobject]@{
    Name = $name
    ExitCode = $exitCode
    Output = ($output | ForEach-Object { "$_" }) -join "`r`n"
    Passed = ($exitCode -eq 0)
  }
}

function Parse-Progress([string]$text) {
  $m = [regex]::Match($text, 'Plan progress:\s*([0-9.]+)%\s*\(done=([0-9]+)\s*/\s*total=([0-9]+),\s*todo=([0-9]+)\)')
  if ($m.Success) {
    return [pscustomobject]@{
      Percent = [double]$m.Groups[1].Value
      Done = [int]$m.Groups[2].Value
      Total = [int]$m.Groups[3].Value
      Todo = [int]$m.Groups[4].Value
    }
  }
  return [pscustomobject]@{
    Percent = 0
    Done = 0
    Total = 0
    Todo = 0
  }
}

$timestamp = Get-Date
$timestampTag = $timestamp.ToString('yyyyMMdd_HHmmss')
$reportPath = Join-Path $OutDir ("release-readiness_$timestampTag.md")

$checks = @()
$checks += Invoke-Check -name 'Static verification suite' -scriptPath '.\scripts\run_verification_suite.ps1'
$checks += Invoke-Check -name 'Execution artifacts check' -scriptPath '.\scripts\check_execution_artifacts.ps1'
$progressResult = Invoke-Check -name 'Plan progress' -scriptPath '.\scripts\calc_plan_progress.ps1' -args @('-PlanPath', $PlanPath)
$checks += $progressResult
$todoResult = Invoke-Check -name 'Plan TODO list' -scriptPath '.\scripts\show_plan_todo.ps1' -args @('-PlanPath', $PlanPath)
$checks += $todoResult

$progress = Parse-Progress -text $progressResult.Output
$failedChecks = @($checks | Where-Object { -not $_.Passed })
$overallPass = $failedChecks.Count -eq 0 -and $progress.Todo -eq 0
$overall = if ($overallPass) { 'GO' } else { 'NO-GO' }

$lines = @()
$lines += '# Release Readiness Report'
$lines += ''
$lines += "- GeneratedAt: $($timestamp.ToString('yyyy-MM-dd HH:mm:ss K'))"
$lines += "- Overall: **$overall**"
$lines += "- PlanProgress: $($progress.Percent)% (done=$($progress.Done) / total=$($progress.Total), todo=$($progress.Todo))"
$lines += ''
$lines += '## Checks'
$lines += ''
foreach ($c in $checks) {
  $status = if ($c.Passed) { 'PASS' } else { 'FAIL' }
  $lines += "- $($c.Name): $status (exit=$($c.ExitCode))"
}
$lines += ''
$lines += '## Remaining TODO'
$lines += ''
$todoLines = $todoResult.Output -split "`r?`n" | Where-Object { $_ -match '^[0-9]+\.\s' }
if ($todoLines.Count -eq 0) {
  $lines += '- none'
} else {
  foreach ($t in $todoLines) {
    $lines += "- $t"
  }
}
$lines += ''
$lines += '## Raw outputs'
$lines += ''
foreach ($c in $checks) {
  $lines += "### $($c.Name)"
  $lines += '```text'
  $lines += $c.Output
  $lines += '```'
  $lines += ''
}

Set-Content -Path $reportPath -Value $lines -Encoding UTF8
Write-Output "[OK] readiness report generated: $reportPath"
Write-Output "Overall: $overall"
exit 0
