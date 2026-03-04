Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$failed = $false

function Check-Contains([string]$file, [string]$pattern, [string]$label) {
  if (!(Test-Path $file)) {
    Write-Output "[NG] missing $file"
    $script:failed = $true
    return
  }
  $hit = Select-String -Path $file -Pattern $pattern -AllMatches
  if ($hit) {
    Write-Output "[OK] $file $label"
  } else {
    Write-Output "[NG] $file missing $label"
    $script:failed = $true
  }
}

# Firestore rules contract
Check-Contains 'firestore.rules' 'function canUpdateProjectFields\(' 'has canUpdateProjectFields()'
Check-Contains 'firestore.rules' 'function hasProjectImmutableFields\(' 'has hasProjectImmutableFields()'
Check-Contains 'firestore.rules' 'request\.resource\.data\.userId == resource\.data\.userId' 'protects project.userId immutability'
Check-Contains 'firestore.rules' 'request\.resource\.data\.classroomId == resource\.data\.classroomId' 'protects project.classroomId immutability'
Check-Contains 'firestore.rules' 'request\.resource\.data\.createdAt == resource\.data\.createdAt' 'protects project.createdAt immutability'
Check-Contains 'firestore.rules' 'changedKeys\(\)\.hasOnly\(' 'limits changed keys for project updates'

# Storage rules contract
Check-Contains 'storage.rules' 'match /recordings/\{ownerUid\}/\{fileName\}' 'has recordings path rule'
Check-Contains 'storage.rules' 'isOwner\(ownerUid\) \|\| isAdmin\(\)' 'restricts recordings to owner or admin'
Check-Contains 'storage.rules' 'match /projects/\{ownerUid\}/\{projectId\}/\{allPaths=\*\*\}' 'has project storage rule'
Check-Contains 'storage.rules' 'canAccessProject\(projectId\)' 'checks project access scope'
Check-Contains 'storage.rules' 'canAccessUserClassroom\(ownerUid\)' 'checks teacher classroom scope'

if ($failed) {
  Write-Output "Rules contract: FAILED"
  exit 1
}

Write-Output "Rules contract: PASSED"
exit 0
