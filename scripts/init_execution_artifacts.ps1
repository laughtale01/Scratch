param(
  [string]$DateTag = '2026-03-03',
  [string]$OutDir = '.\docs\testing\executions'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (!(Test-Path $OutDir)) {
  New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

$map = @(
  @{
    Src = ".\docs\testing\2026-03-03_gallery-auth-fault-injection-results-template.md"
    Dst = Join-Path $OutDir "$DateTag`_gallery-auth-fault-injection-results.md"
  },
  @{
    Src = ".\docs\testing\2026-03-03_gallery-auth-e2e-ownership-template.md"
    Dst = Join-Path $OutDir "$DateTag`_gallery-auth-e2e-ownership.md"
  },
  @{
    Src = ".\docs\testing\2026-03-03_user-coverage-audit-result-template.md"
    Dst = Join-Path $OutDir "$DateTag`_user-coverage-audit-result.md"
  }
)

foreach ($entry in $map) {
  if (!(Test-Path $entry.Src)) {
    Write-Output "[NG] template missing: $($entry.Src)"
    exit 1
  }

  if (Test-Path $entry.Dst) {
    Write-Output "[OK] exists: $($entry.Dst)"
    continue
  }

  Copy-Item -Path $entry.Src -Destination $entry.Dst
  Write-Output "[OK] created: $($entry.Dst)"
}

Write-Output "Execution artifacts initialized."
exit 0
