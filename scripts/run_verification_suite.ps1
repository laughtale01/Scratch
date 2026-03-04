param(
  [string]$LogFile = "",
  [switch]$IncludeExecutionArtifacts
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ([string]::IsNullOrWhiteSpace($LogFile)) {
  $timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
  $LogFile = ".\docs\testing\logs\verification_suite_$timestamp.log"
}

$logDir = Split-Path -Parent $LogFile
if (!(Test-Path $logDir)) {
  New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

function Run-Step([string]$name, [string]$scriptPath) {
  Write-Output "== $name =="
  powershell -ExecutionPolicy Bypass -File $scriptPath
  if ($LASTEXITCODE -ne 0) {
    throw "$name failed"
  }
}

try {
  $transcriptStarted = $false
  try {
    Start-Transcript -Path $LogFile -Force | Out-Null
    $transcriptStarted = $true
  } catch {
    Write-Output "Warning: transcript unavailable, continuing without file log"
  }
  Write-Output "Verification suite started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
  Run-Step -name 'Static verification' -scriptPath '.\scripts\verify_gallery_auth_static.ps1'
  if ($IncludeExecutionArtifacts) {
    Run-Step -name 'Execution artifacts check' -scriptPath '.\scripts\check_execution_artifacts.ps1'
  }
  Write-Output "Verification suite completed: PASSED"
  if ($transcriptStarted) {
    Stop-Transcript | Out-Null
  }
  exit 0
} catch {
  Write-Output "Verification suite completed: FAILED"
  Write-Output $_.Exception.Message
  try { Stop-Transcript | Out-Null } catch { }
  exit 1
}
