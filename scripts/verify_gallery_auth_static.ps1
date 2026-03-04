Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$failed = $false

function Write-CheckOk([string]$message) {
  Write-Output "[OK] $message"
}

function Write-CheckNg([string]$message) {
  Write-Output "[NG] $message"
  $script:failed = $true
}

function Assert-NoInlineHandlers([string[]]$files) {
  $pattern = 'on(click|change|input|submit|error)='
  foreach ($file in $files) {
    $hits = Select-String -Path $file -Pattern $pattern -AllMatches -CaseSensitive
    if ($hits) {
      $details = ($hits | ForEach-Object { "{0}:{1}" -f $_.LineNumber, $_.Line.Trim() }) -join [Environment]::NewLine
      Write-CheckNg "$file has inline handlers`n$details"
    } else {
      Write-CheckOk "$file no inline handlers"
    }
  }
}

function Assert-NoOnclickAssignments([string[]]$files) {
  $pattern = '\.onclick\s*='
  foreach ($file in $files) {
    $hits = Select-String -Path $file -Pattern $pattern -AllMatches
    if ($hits) {
      $details = ($hits | ForEach-Object { "{0}:{1}" -f $_.LineNumber, $_.Line.Trim() }) -join [Environment]::NewLine
      Write-CheckNg "$file has .onclick assignments`n$details"
    } else {
      Write-CheckOk "$file no .onclick assignments"
    }
  }
}

function Assert-NoDomOnAssignments([string[]]$files) {
  $pattern = '\.on(click|change|input|submit|error|load|keydown|keyup|mousedown|mouseup)\s*='
  foreach ($file in $files) {
    $hits = Select-String -Path $file -Pattern $pattern -AllMatches -CaseSensitive
    if ($hits) {
      $details = ($hits | ForEach-Object { "{0}:{1}" -f $_.LineNumber, $_.Line.Trim() }) -join [Environment]::NewLine
      Write-CheckNg "$file has .on* assignments`n$details"
    } else {
      Write-CheckOk "$file no .on* assignments"
    }
  }
}

function Assert-NodeCheck([string[]]$files) {
  foreach ($file in $files) {
    node --check $file 1>$null 2>$null
    if ($?) {
      Write-CheckOk "$file JS syntax OK"
    } else {
      Write-CheckNg "$file JS syntax error"
    }
  }
}

function Assert-JsonValid([string[]]$files) {
  foreach ($file in $files) {
    try {
      Get-Content $file -Raw | ConvertFrom-Json | Out-Null
      Write-CheckOk "$file JSON valid"
    } catch {
      Write-CheckNg "$file JSON invalid: $($_.Exception.Message)"
    }
  }
}

function Assert-ContainsPattern([string]$file, [string]$pattern, [string]$label) {
  $hit = Select-String -Path $file -Pattern $pattern -AllMatches
  if ($hit) {
    Write-CheckOk "$file $label"
  } else {
    Write-CheckNg "$file missing $label"
  }
}

function Assert-NoPattern([string[]]$files, [string]$pattern, [string]$label) {
  foreach ($file in $files) {
    $hits = Select-String -Path $file -Pattern $pattern -AllMatches
    if ($hits) {
      $details = ($hits | ForEach-Object { "{0}:{1}" -f $_.LineNumber, $_.Line.Trim() }) -join [Environment]::NewLine
      Write-CheckNg "$file has forbidden pattern: $label`n$details"
    } else {
      Write-CheckOk "$file no forbidden pattern: $label"
    }
  }
}

Assert-NoInlineHandlers @(
  'gallery.html',
  'admin.html',
  'admin-mobile.html',
  'firebase-integration.js',
  'admin.js'
)

Assert-NoOnclickAssignments @(
  'gallery.html',
  'admin.js',
  'firebase-integration.js'
)

Assert-NoDomOnAssignments @(
  'gallery.html',
  'admin.js',
  'firebase-integration.js',
  'admin-mobile.html',
  'admin.html'
)

Assert-NodeCheck @(
  'admin.js',
  'firebase-integration.js',
  'functions/index.js'
)

Assert-JsonValid @(
  'firebase.json',
  'firestore.indexes.json'
)

Assert-ContainsPattern 'gallery.html' 'function normalizeUserId\(' 'has normalizeUserId()'
Assert-ContainsPattern 'gallery.html' 'function buildLoginEmail\(' 'has buildLoginEmail()'
Assert-ContainsPattern 'gallery.html' 'buildLoginEmail\(' 'uses buildLoginEmail()'

Assert-ContainsPattern 'admin-mobile.html' 'function normalizeUserId\(' 'has normalizeUserId()'
Assert-ContainsPattern 'admin-mobile.html' 'function buildLoginEmail\(' 'has buildLoginEmail()'
Assert-ContainsPattern 'admin-mobile.html' 'buildLoginEmail\(' 'uses buildLoginEmail()'

Assert-ContainsPattern 'firebase-integration.js' 'normalizeUserId\(' 'has normalizeUserId()'
Assert-ContainsPattern 'firebase-integration.js' 'userIdToEmail\(' 'has userIdToEmail()'

Assert-NoPattern @(
  'gallery.html',
  'admin-mobile.html',
  'firebase-integration.js'
) 'toLowerCase\(\)\s*\+\s*(EMAIL_DOMAIN|LOGIN_EMAIL_DOMAIN)' 'raw email concat to domain constants after toLowerCase()'

Assert-NoPattern @(
  'gallery.html',
  'admin-mobile.html',
  'firebase-integration.js'
) 'toLowerCase\(\)\s*\+\s*[''"]@laughtale\.local[''"]' 'raw email concat to literal domain after toLowerCase()'

powershell -ExecutionPolicy Bypass -File .\scripts\verify_firestore_indexes.ps1 1>$null 2>$null
if ($?) {
  Write-CheckOk "firestore index coverage OK"
} else {
  Write-CheckNg "firestore index coverage FAILED"
}

powershell -ExecutionPolicy Bypass -File .\scripts\verify_firebase_config_wiring.ps1 1>$null 2>$null
if ($?) {
  Write-CheckOk "firebase config wiring OK"
} else {
  Write-CheckNg "firebase config wiring FAILED"
}

powershell -ExecutionPolicy Bypass -File .\scripts\verify_rules_contract.ps1 1>$null 2>$null
if ($?) {
  Write-CheckOk "rules contract OK"
} else {
  Write-CheckNg "rules contract FAILED"
}

powershell -ExecutionPolicy Bypass -File .\scripts\verify_powershell_scripts_syntax.ps1 1>$null 2>$null
if ($?) {
  Write-CheckOk "powershell scripts syntax OK"
} else {
  Write-CheckNg "powershell scripts syntax FAILED"
}

powershell -ExecutionPolicy Bypass -File .\scripts\verify_html_id_uniqueness.ps1 1>$null 2>$null
if ($?) {
  Write-CheckOk "html id uniqueness OK"
} else {
  Write-CheckNg "html id uniqueness FAILED"
}

if ($failed) {
  Write-Output "Static verification: FAILED"
  exit 1
}

Write-Output "Static verification: PASSED"
exit 0
