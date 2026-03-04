Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$files = @(
  'gallery.html',
  'admin.html',
  'admin-mobile.html'
)

$failed = $false

foreach ($file in $files) {
  if (!(Test-Path $file)) {
    Write-Output "[NG] missing $file"
    $failed = $true
    continue
  }

  $matches = Select-String -Path $file -Pattern 'id="([^"]+)"' -AllMatches
  $ids = @()
  foreach ($m in $matches) {
    foreach ($cap in $m.Matches) {
      $ids += $cap.Groups[1].Value
    }
  }

  if ($ids.Count -eq 0) {
    Write-Output "[OK] $file no id attributes found"
    continue
  }

  $dups = $ids | Group-Object | Where-Object { $_.Count -gt 1 } | Sort-Object Name
  if ($dups) {
    Write-Output "[NG] $file duplicate id(s):"
    foreach ($dup in $dups) {
      Write-Output "  - $($dup.Name) x$($dup.Count)"
    }
    $failed = $true
  } else {
    Write-Output "[OK] $file id uniqueness OK"
  }
}

if ($failed) {
  Write-Output "HTML id uniqueness: FAILED"
  exit 1
}

Write-Output "HTML id uniqueness: PASSED"
exit 0
