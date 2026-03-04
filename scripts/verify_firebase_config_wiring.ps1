Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$configPath = 'firebase.json'
if (!(Test-Path $configPath)) {
  Write-Output "[NG] missing firebase.json"
  exit 1
}

$cfg = Get-Content $configPath -Raw | ConvertFrom-Json
$failed = $false

function Check-Path([string]$path, [string]$label) {
  if ([string]::IsNullOrWhiteSpace($path)) {
    Write-Output "[NG] $label is empty"
    $script:failed = $true
    return
  }
  if (Test-Path $path) {
    Write-Output "[OK] $label => $path"
  } else {
    Write-Output "[NG] $label path not found => $path"
    $script:failed = $true
  }
}

if ($null -eq $cfg.firestore) {
  Write-Output "[NG] firebase.json.firestore missing"
  $failed = $true
} else {
  Check-Path -path $cfg.firestore.rules -label 'firestore.rules'
  Check-Path -path $cfg.firestore.indexes -label 'firestore.indexes'
}

if ($null -eq $cfg.storage) {
  Write-Output "[NG] firebase.json.storage missing"
  $failed = $true
} else {
  Check-Path -path $cfg.storage.rules -label 'storage.rules'
}

if ($failed) {
  Write-Output "Firebase config wiring: FAILED"
  exit 1
}

Write-Output "Firebase config wiring: PASSED"
exit 0
