Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$scriptFiles = Get-ChildItem .\scripts -Filter *.ps1 -File | Sort-Object Name
$failed = $false

foreach ($file in $scriptFiles) {
  $tokens = $null
  $errors = $null
  [System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$errors) | Out-Null

  if ($errors -and $errors.Count -gt 0) {
    Write-Output "[NG] $($file.Name) syntax error(s):"
    foreach ($e in $errors) {
      Write-Output ("  - line {0}: {1}" -f $e.Extent.StartLineNumber, $e.Message)
    }
    $failed = $true
  } else {
    Write-Output "[OK] $($file.Name) syntax OK"
  }
}

if ($failed) {
  Write-Output "PowerShell script syntax: FAILED"
  exit 1
}

Write-Output "PowerShell script syntax: PASSED"
exit 0
