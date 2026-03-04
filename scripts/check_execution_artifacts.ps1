param(
  [string]$DateTag = '2026-03-03',
  [string]$Dir = '.\docs\testing\executions'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$paths = @{
  Fault = Join-Path $Dir "$DateTag`_gallery-auth-fault-injection-results.md"
  Ownership = Join-Path $Dir "$DateTag`_gallery-auth-e2e-ownership.md"
  Coverage = Join-Path $Dir "$DateTag`_user-coverage-audit-result.md"
}

$failed = $false

function Need-File([string]$label, [string]$path) {
  if (Test-Path $path) {
    Write-Output "[OK] $label file exists"
    return $true
  }
  Write-Output "[NG] $label file missing: $path"
  $script:failed = $true
  return $false
}

function Must-HaveValue([string]$label, [string]$content, [string]$regex) {
  $hit = [regex]::Match($content, $regex, [System.Text.RegularExpressions.RegexOptions]::Multiline)
  if ($hit.Success) {
    Write-Output "[OK] $label"
  } else {
    Write-Output "[NG] $label"
    $script:failed = $true
  }
}

function Must-HaveFilledField([string]$label, [string]$content, [string]$fieldName) {
  $pattern = "(?m)^- " + [regex]::Escape($fieldName) + ":[ \t]*([^\r\n]*)[ \t]*$"
  $hit = [regex]::Match($content, $pattern)
  if (-not $hit.Success) {
    Write-Output "[NG] $label (field missing)"
    $script:failed = $true
    return
  }
  $value = ($hit.Groups[1].Value).Trim()
  if ([string]::IsNullOrWhiteSpace($value)) {
    Write-Output "[NG] $label (empty)"
    $script:failed = $true
    return
  }
  if ($value -in @('PASS / FAIL', '-', 'TBD', 'TODO')) {
    Write-Output "[NG] $label (placeholder)"
    $script:failed = $true
    return
  }
  Write-Output "[OK] $label"
}

if (Need-File 'fault-injection' $paths.Fault) {
  $c = Get-Content $paths.Fault -Raw
  Must-HaveValue 'fault final judgement filled' $c '(?m)^## Final judgement[\s\S]*^- Result:\s*(PASS|FAIL)\s*$'
  Must-HaveValue 'fault cases recorded' $c '(?m)^## Case 5:'
}

if (Need-File 'coverage-audit' $paths.Coverage) {
  $c = Get-Content $paths.Coverage -Raw
  Must-HaveFilledField 'coverage authUsersAudited filled' $c 'authUsersAudited'
  Must-HaveFilledField 'coverage missingUserDocs filled' $c 'missingUserDocs'
  Must-HaveFilledField 'coverage coverageRate filled' $c 'coverageRate'
}

if (Need-File 'ownership' $paths.Ownership) {
  $c = Get-Content $paths.Ownership -Raw
  Must-HaveFilledField 'e2e owner name filled' $c 'Owner name'
  Must-HaveFilledField 'planned execution date filled' $c 'Planned execution date (JST)'
  Must-HaveFilledField 'rollback owner name filled' $c 'Owner name'
  Must-HaveFilledField 'sign-offs filled' $c 'Project lead sign-off'
}

if ($failed) {
  Write-Output "Execution artifacts check: FAILED"
  exit 1
}

Write-Output "Execution artifacts check: PASSED"
exit 0
