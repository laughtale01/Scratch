Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$indexFile = 'firestore.indexes.json'
if (!(Test-Path $indexFile)) {
  Write-Output "[NG] missing $indexFile"
  exit 1
}

$json = Get-Content $indexFile -Raw | ConvertFrom-Json
$indexes = @($json.indexes)

function Has-Index([string]$collectionGroup, [string[]]$parts) {
  foreach ($idx in $indexes) {
    if ($idx.collectionGroup -ne $collectionGroup) { continue }
    $actual = @($idx.fields | ForEach-Object {
      if ($_.order) { "$($_.fieldPath):$($_.order)" } else { "$($_.fieldPath):$($_.arrayConfig)" }
    })
    if ($actual.Count -ne $parts.Count) { continue }
    $same = $true
    for ($i = 0; $i -lt $parts.Count; $i++) {
      if ($actual[$i] -ne $parts[$i]) {
        $same = $false
        break
      }
    }
    if ($same) { return $true }
  }
  return $false
}

$requirements = @(
  @{ collection = 'users'; fields = @('classroomId:ASCENDING', 'displayName:ASCENDING') },
  @{ collection = 'projects'; fields = @('isSubmitted:ASCENDING', 'classroomId:ASCENDING') },
  @{ collection = 'projects'; fields = @('isSubmitted:ASCENDING', 'userId:ASCENDING') },
  @{ collection = 'projects'; fields = @('classroomId:ASCENDING', 'updatedAt:DESCENDING') },
  @{ collection = 'projects'; fields = @('userId:ASCENDING', 'updatedAt:DESCENDING') },
  @{ collection = 'videos'; fields = @('status:ASCENDING', 'createdAt:DESCENDING') },
  @{ collection = 'videos'; fields = @('status:ASCENDING', 'classroomId:ASCENDING', 'createdAt:DESCENDING') }
)

$failed = $false
foreach ($req in $requirements) {
  $label = "$($req.collection) :: $([string]::Join(', ', $req.fields))"
  if (Has-Index -collectionGroup $req.collection -parts $req.fields) {
    Write-Output "[OK] $label"
  } else {
    Write-Output "[NG] $label"
    $failed = $true
  }
}

if ($failed) {
  Write-Output "Index coverage: FAILED"
  exit 1
}

Write-Output "Index coverage: PASSED"
exit 0
