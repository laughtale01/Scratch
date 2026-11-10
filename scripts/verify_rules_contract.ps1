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
  $hit = Select-String -Path $file -Pattern $pattern -AllMatches -CaseSensitive
  if ($hit) {
    Write-Output "[OK] $file $label"
  } else {
    Write-Output "[NG] $file missing $label"
    $script:failed = $true
  }
}

# ファイル全体を 1 つの文字列として読み、複数行にまたがる形を確かめる
function Check-RawMatch([string]$file, [string]$pattern, [string]$label) {
  if (!(Test-Path $file)) {
    Write-Output "[NG] missing $file"
    $script:failed = $true
    return
  }
  $content = Get-Content -Path $file -Raw
  if ($content -cmatch $pattern) {
    Write-Output "[OK] $file $label"
  } else {
    Write-Output "[NG] $file missing $label"
    $script:failed = $true
  }
}

# 当たってはいけない形が無いか確かめる
function Check-NoMatch([string]$file, [string]$pattern, [string]$label) {
  if (!(Test-Path $file)) {
    Write-Output "[NG] missing $file"
    $script:failed = $true
    return
  }
  $content = Get-Content -Path $file -Raw
  if ($content -cmatch $pattern) {
    Write-Output "[NG] $file $label"
    $script:failed = $true
  } else {
    Write-Output "[OK] $file $label"
  }
}

# Firestore rules contract
Check-Contains 'firestore.rules' 'function canUpdateProjectFields\(' 'has canUpdateProjectFields()'
Check-Contains 'firestore.rules' 'function hasProjectImmutableFields\(' 'has hasProjectImmutableFields()'
Check-Contains 'firestore.rules' 'request\.resource\.data\.userId == resource\.data\.userId' 'protects project.userId immutability'
Check-Contains 'firestore.rules' 'request\.resource\.data\.classroomId == resource\.data\.classroomId' 'protects project.classroomId immutability'
Check-Contains 'firestore.rules' 'request\.resource\.data\.createdAt == resource\.data\.createdAt' 'protects project.createdAt immutability'
Check-RawMatch 'firestore.rules' 'function isSafeId\(id\)\s*\{\s*return id is string && id\.matches\(''\[A-Za-z0-9\]\{1,128\}''\);' 'isSafeId() allows only letters and digits'
Check-RawMatch 'firestore.rules' 'function isOwnProjectFileUrl\([^)]*\)\s*\{\s*return isSafeId\(ownerUid\) && isSafeId\(projectId\)' 'checks ids before building the project file url pattern'
Check-RawMatch 'firestore.rules' 'function isOwnThumbnailUrl\([^)]*\)\s*\{\s*return url == null \|\| \(isSafeId\(ownerUid\) && isSafeId\(projectId\)' 'checks ids before building the thumbnail url pattern'
Check-RawMatch 'firestore.rules' 'data\.classroomId == null \|\|\s*\(getUserData\(\)\.get\(''role'', null\) == ''student'' && data\.classroomId == getUserData\(\)\.get\(''classroomId'', null\)\)' 'only students create projects with a classroom'
Check-RawMatch 'firestore.rules' '(?s)function canUpdateProjectFields\([^)]*\)\s*\{\s*let keys = request\.resource\.data\.diff\(resource\.data\)\.affectedKeys\(\);\s*return keys\.hasOnly\(' 'limits affected keys (added/removed/changed) for project updates'

# Storage rules contract
# ブロックごとに中身まで確かめる（関数の定義や別のブロックの同じ行に当たって通らないように）
Check-RawMatch 'storage.rules' 'match /projects/\{ownerUid\}/\{projectId\}/\{allPaths=\*\*\}\s*\{\s*allow read, delete: if isOwner\(ownerUid\) \|\| canAccessProject\(ownerUid, projectId\);\s*\}' 'project files: teachers read/delete only (no writes under **)'
# 作品のファイルを置けるのは持ち主だけ・決まった名前（current.sb3 と versions/v<番号>.sb3）・決まった大きさと種類だけ
Check-RawMatch 'storage.rules' 'match /projects/\{ownerUid\}/\{projectId\}/current\.sb3\s*\{\s*allow create, update: if isOwner\(ownerUid\) && isProjectIdName\(projectId\) && isSb3Upload\(\);\s*\}' 'project current.sb3: owners write, size and type limited'
Check-RawMatch 'storage.rules' 'match /projects/\{ownerUid\}/\{projectId\}/versions/\{fileName\}\s*\{\s*allow create, update: if isOwner\(ownerUid\) && isProjectIdName\(projectId\) &&\s*fileName\.matches\(''v\[0-9\]\{1,9\}\[\.\]sb3''\) && isSb3Upload\(\);\s*\}' 'project versions: owners write, fixed names, size and type limited'
Check-RawMatch 'storage.rules' 'function isProjectIdName\(projectId\)\s*\{\s*return projectId\.matches\(''\[A-Za-z0-9\]\{1,128\}''\);' 'project ids for uploads are letters and digits only'
Check-RawMatch 'storage.rules' 'function isSb3Upload\(\)\s*\{\s*return request\.resource\.size < 20 \* 1024 \* 1024 &&\s*request\.resource\.contentType in \[''application/x\.scratch\.sb3'', ''application/octet-stream''\];' 'project files are under 20MB and sb3 or octet-stream only'
Check-RawMatch 'storage.rules' 'function isThumbnailUpload\(\)\s*\{\s*return request\.resource\.size < 1024 \* 1024 &&\s*request\.resource\.contentType == ''image/png'';' 'thumbnails are PNG under 1MB only'
Check-RawMatch 'storage.rules' 'match /thumbnails/\{ownerUid\}/\{fileName\}\s*\{(?:\s*//[^\n]*)*\s*allow read, delete: if isOwner\(ownerUid\) \|\| canAccessThumbnail\(ownerUid, fileName\);\s*allow create, update: if isOwner\(ownerUid\) &&\s*fileName\.matches\(''\[A-Za-z0-9\]\{1,128\}\[\.\]png''\) && isThumbnailUpload\(\);\s*\}' 'thumbnails: teachers read/delete only, owners write fixed-name PNGs'
Check-RawMatch 'storage.rules' 'match /recordings/\{ownerUid\}/\{fileName\}\s*\{\s*allow read: if false;\s*allow delete: if isOwner\(ownerUid\) \|\| isAdmin\(\);\s*allow create, update: if false;\s*\}' 'recordings: no direct client reads or writes'
Check-RawMatch 'storage.rules' 'project != null &&\s*project\.data\.userId == ownerUid &&\s*isTeacherOfClassroom\(project\.data\.classroomId\)' 'ties the storage owner to the project owner and classroom'
Check-RawMatch 'storage.rules' 'let myClassroomId = getUserData\(\)\.classroomId;\s*return isTeacher\(\) && myClassroomId is string && classroomId == myClassroomId;' 'teachers without a classroom see nothing'
Check-RawMatch 'storage.rules' 'fileName\.matches\(''\[A-Za-z0-9\]\{1,128\}\[\.\]png''\) &&\s*canAccessProject\(ownerUid, fileName\.split\(' 'thumbnails are judged by the project document (fixed name only)'
Check-NoMatch 'storage.rules' '(?<![.\w])get\(' 'no bare get() (Storage rules must use firestore.get)'

if ($failed) {
  Write-Output "Rules contract: FAILED"
  exit 1
}

Write-Output "Rules contract: PASSED"
exit 0
