param(
  [string]$PlanPath = '.\docs\plans\2026-03-03_gallery-auth-detailed-implementation-plan.md'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (!(Test-Path $PlanPath)) {
  Write-Output "[NG] plan file not found: $PlanPath"
  exit 1
}

$todos = Select-String -Path $PlanPath -Pattern '^- \[ \]' -CaseSensitive | ForEach-Object { $_.Line.Trim() }

if (-not $todos -or $todos.Count -eq 0) {
  Write-Output "No TODO checklist items."
  exit 0
}

Write-Output "Remaining TODO items:"
$idx = 1
foreach ($t in $todos) {
  Write-Output ("{0}. {1}" -f $idx, $t.Substring(6))
  $idx++
}
exit 0
