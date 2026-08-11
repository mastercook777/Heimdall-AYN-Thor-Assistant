[CmdletBinding()]
param(
    [string]$Root,
    [string]$ReleaseTag,
    [switch]$AllowDraftNotes,
    [switch]$RequireCleanTree,
    [switch]$RequireExistingTag
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $PSScriptRoot
}
$rootPath = (Resolve-Path -LiteralPath $Root).Path

function Read-StrictProperties {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$RequiredKeys
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing properties file: $Path"
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        if ($trimmed -notmatch '^([A-Z][A-Z0-9_]*)=(.+)$') {
            throw "Malformed properties line in $Path`: $line"
        }
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($values.ContainsKey($key)) {
            throw "Duplicate property $key in $Path"
        }
        $values[$key] = $value
    }

    foreach ($key in $RequiredKeys) {
        if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
            throw "Missing required property $key in $Path"
        }
    }
    return $values
}

$versionValues = Read-StrictProperties `
    -Path (Join-Path $rootPath "version.properties") `
    -RequiredKeys @("VERSION_CODE", "VERSION_NAME")
$releaseValues = Read-StrictProperties `
    -Path (Join-Path $rootPath "release.properties") `
    -RequiredKeys @("APPLICATION_ID", "RELEASE_CERT_SHA256")

$versionCodeText = $versionValues["VERSION_CODE"]
$versionName = $versionValues["VERSION_NAME"]
$applicationId = $releaseValues["APPLICATION_ID"]
$certificateSha256 = $releaseValues["RELEASE_CERT_SHA256"].Replace(":", "").ToUpperInvariant()

if ($versionCodeText -notmatch '^[1-9][0-9]*$') {
    throw "VERSION_CODE must be a positive integer."
}
$versionCode = [int]$versionCodeText
if ($versionName -notmatch '^[0-9]+\.[0-9]+\.[0-9]+-alpha\.[0-9]+$') {
    throw "VERSION_NAME must match X.Y.Z-alpha.N."
}
if ($applicationId -ne "com.mastercook777.heimdall") {
    throw "APPLICATION_ID must remain com.mastercook777.heimdall."
}
if ($certificateSha256 -notmatch '^[0-9A-F]{64}$') {
    throw "RELEASE_CERT_SHA256 must contain exactly 64 hexadecimal characters."
}

$expectedTag = "v$versionName"
if ([string]::IsNullOrWhiteSpace($ReleaseTag)) {
    $ReleaseTag = $expectedTag
}
if ($ReleaseTag -notmatch '^v[0-9]+\.[0-9]+\.[0-9]+-alpha\.[0-9]+$') {
    throw "ReleaseTag must match vX.Y.Z-alpha.N."
}
if ($ReleaseTag -ne $expectedTag) {
    throw "ReleaseTag $ReleaseTag does not match VERSION_NAME $versionName."
}

$notesPath = Join-Path $rootPath "docs\releases\$ReleaseTag.md"
if (-not (Test-Path -LiteralPath $notesPath -PathType Leaf)) {
    throw "Missing release notes: docs/releases/$ReleaseTag.md"
}
if (-not $AllowDraftNotes) {
    $notesText = Get-Content -LiteralPath $notesPath -Raw
    if ($notesText -match '(?i)\b(?:draft|tbd)\b') {
        throw "Release notes still contain Draft or TBD: docs/releases/$ReleaseTag.md"
    }
}

if ($RequireCleanTree -or $RequireExistingTag) {
    & git -C $rootPath rev-parse --is-inside-work-tree *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Release checks require a Git work tree: $rootPath"
    }
}
if ($RequireCleanTree) {
    $dirtyEntries = @(& git -C $rootPath status --porcelain=v1 --untracked-files=all)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect Git work-tree state."
    }
    if ($dirtyEntries.Count -gt 0) {
        throw "Release candidate source must be clean."
    }
}
if ($RequireExistingTag) {
    & git -C $rootPath show-ref --verify --quiet "refs/tags/$ReleaseTag"
    if ($LASTEXITCODE -ne 0) {
        throw "The requested release tag does not exist: $ReleaseTag"
    }
    $tagCommit = (& git -C $rootPath rev-parse "refs/tags/$ReleaseTag^{commit}").Trim()
    $headCommit = (& git -C $rootPath rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $tagCommit -ne $headCommit) {
        throw "HEAD does not match the peeled commit for $ReleaseTag."
    }
}

Write-Output "Release readiness verified"
Write-Output "tag=$ReleaseTag"
Write-Output "versionCode=$versionCode"
Write-Output "versionName=$versionName"
Write-Output "applicationId=$applicationId"
Write-Output "certificateSha256=$certificateSha256"
Write-Output "releaseNotes=docs/releases/$ReleaseTag.md"
