param(
  [string]$PlanPath = '.\docs\plans\2026-03-03_gallery-auth-detailed-implementation-plan.md'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (!(Test-Path $PlanPath)) {
  Write-Output "[NG] plan file not found: $PlanPath"
  exit 1
}

$total = (Select-String -Path $PlanPath -Pattern '^- \[[x ]\]' -CaseSensitive).Count
$done = (Select-String -Path $PlanPath -Pattern '^- \[x\]' -CaseSensitive).Count
$todo = (Select-String -Path $PlanPath -Pattern '^- \[ \]' -CaseSensitive).Count

if ($total -eq 0) {
  Write-Output "Plan progress: 0% (no checklist items)"
  exit 0
}

$progress = [math]::Round(($done / $total) * 100, 1)
Write-Output "Plan progress: $progress% (done=$done / total=$total, todo=$todo)"
exit 0
