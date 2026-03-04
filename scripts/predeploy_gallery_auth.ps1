param(
  [switch]$Deploy,
  [switch]$RequireExecutionArtifacts,
  [switch]$AllowIncompleteExecutionArtifacts,
  [ValidateSet('gallery-auth', 'all')]
  [string]$Scope = 'gallery-auth',
  [string]$Project = 'laughtale-scratch-bcc8a'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Fail([string]$message) {
  Write-Output "[NG] $message"
  exit 1
}

Write-Output "Predeploy started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Output "Project: $Project"
Write-Output "Scope: $Scope"
if ($Deploy -and $AllowIncompleteExecutionArtifacts) {
  Write-Output "Execution artifacts gate: BYPASSED (AllowIncompleteExecutionArtifacts=true)"
}

$firebaseCmd = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseCmd) {
  Fail "firebase CLI not found in PATH"
}

# 1) Mandatory verification gate
powershell -ExecutionPolicy Bypass -File .\scripts\run_verification_suite.ps1
if ($LASTEXITCODE -ne 0) {
  Fail "verification suite failed"
}

# 1.5) Mandatory execution-artifacts gate for deploy
$mustCheckExecutionArtifacts = $RequireExecutionArtifacts -or ($Deploy -and -not $AllowIncompleteExecutionArtifacts)
if ($mustCheckExecutionArtifacts) {
  powershell -ExecutionPolicy Bypass -File .\scripts\check_execution_artifacts.ps1
  if ($LASTEXITCODE -ne 0) {
    Fail "execution artifacts check failed"
  }
  Write-Output "[OK] execution artifacts check passed"
}

# 2) Determine deploy targets
$deployOnly = if ($Scope -eq 'gallery-auth') {
  'hosting,functions,firestore:rules,firestore:indexes,storage'
} else {
  'functions,firestore,storage,hosting'
}

$deployArgs = @('deploy', '--project', $Project, '--only', $deployOnly)
$deployCommandPreview = "firebase $($deployArgs -join ' ')"

Write-Output "[OK] verification suite passed"
Write-Output "Deploy command:"
Write-Output "  $deployCommandPreview"

if (-not $Deploy) {
  Write-Output "Dry-run mode: no deploy executed. Use -Deploy to run."
  exit 0
}

# 3) Execute deploy
Write-Output "Executing deploy..."
& firebase @deployArgs
if ($LASTEXITCODE -ne 0) {
  Fail "firebase deploy failed"
}

Write-Output "Predeploy completed: DEPLOYED"
exit 0
