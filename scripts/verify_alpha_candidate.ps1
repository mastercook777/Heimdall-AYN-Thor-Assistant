[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$CandidateDirectory,

    [Parameter(Mandatory = $true)]
    [string]$ReleaseTag,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedSourceCommit,

    [Parameter(Mandatory = $true)]
    [string]$ApprovedApkSha256,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedWorkflowRunId,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedArtifactName,

    [string]$Root,
    [string]$OutputDirectory,
    [string]$AndroidBuildToolsVersion = "35.0.0"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $PSScriptRoot
}
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$candidatePath = (Resolve-Path -LiteralPath $CandidateDirectory).Path

function Read-StrictCandidateProperties {
    param([Parameter(Mandatory = $true)][string]$Path)

    $requiredKeys = @(
        "schemaVersion",
        "status",
        "releaseTag",
        "sourceCommit",
        "package",
        "versionCode",
        "versionName",
        "certificateSha256",
        "apkFile",
        "apkBytes",
        "apkSha256",
        "workflowRepository",
        "workflowRunId",
        "workflowRunAttempt",
        "artifactName",
        "builtAtUtc"
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing candidate metadata: $Path"
    }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }
        if ($line -notmatch '^([A-Za-z][A-Za-z0-9]*)=(.*)$') {
            throw "Malformed candidate metadata line: $line"
        }
        $key = $Matches[1]
        if ($values.ContainsKey($key)) {
            throw "Duplicate candidate metadata property: $key"
        }
        $values[$key] = $Matches[2].Trim()
    }
    foreach ($key in $requiredKeys) {
        if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
            throw "Missing candidate metadata property: $key"
        }
    }
    return $values
}

function Normalize-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Value)
    $normalized = $Value.Replace(":", "").Trim().ToUpperInvariant()
    if ($normalized -notmatch '^[0-9A-F]{64}$') {
        throw "Expected a 64-character SHA-256 value, found: $Value"
    }
    return $normalized
}

if ($ReleaseTag -notmatch '^v[0-9]+\.[0-9]+\.[0-9]+-alpha\.[0-9]+$') {
    throw "ReleaseTag must match vX.Y.Z-alpha.N."
}
if ($ExpectedSourceCommit -notmatch '^[0-9a-fA-F]{40}$') {
    throw "ExpectedSourceCommit must be a full 40-character Git commit."
}
if ($ExpectedWorkflowRunId -notmatch '^[1-9][0-9]*$') {
    throw "ExpectedWorkflowRunId must be a positive integer."
}
$versionName = $ReleaseTag.Substring(1)
$artifactPattern = '^heimdall-alpha-candidate-' +
    [regex]::Escape($versionName) + '-' +
    [regex]::Escape($ExpectedWorkflowRunId) + '-([1-9][0-9]*)$'
if ($ExpectedArtifactName -notmatch $artifactPattern) {
    throw "Unexpected immutable candidate artifact name: $ExpectedArtifactName"
}
$expectedWorkflowRunAttempt = $Matches[1]

$metadataPath = Join-Path $candidatePath "candidate-metadata.properties"
$metadata = Read-StrictCandidateProperties $metadataPath
$approvedSha256 = Normalize-Sha256 $ApprovedApkSha256
$sourceCommit = $ExpectedSourceCommit.ToLowerInvariant()

$versionValues = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $rootPath "version.properties") -Raw)
$releaseValues = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $rootPath "release.properties") -Raw)
$expectedCertificate = Normalize-Sha256 $releaseValues.RELEASE_CERT_SHA256

$expectedPairs = [ordered]@{
    schemaVersion = "1"
    status = "IMMUTABLE_SIGNED_CANDIDATE_NOT_PUBLISHED"
    releaseTag = $ReleaseTag
    sourceCommit = $sourceCommit
    package = $releaseValues.APPLICATION_ID
    versionCode = [string]$versionValues.VERSION_CODE
    versionName = $versionName
    certificateSha256 = $expectedCertificate
    apkSha256 = $approvedSha256
    workflowRepository = $env:GITHUB_REPOSITORY
    workflowRunId = $ExpectedWorkflowRunId
    artifactName = $ExpectedArtifactName
}
foreach ($pair in $expectedPairs.GetEnumerator()) {
    $actual = [string]$metadata[$pair.Key]
    if ($pair.Key -in @("sourceCommit", "certificateSha256", "apkSha256")) {
        $actual = $actual.ToUpperInvariant()
        $expected = ([string]$pair.Value).ToUpperInvariant()
    } else {
        $expected = [string]$pair.Value
    }
    if ($actual -ne $expected) {
        throw "Candidate metadata mismatch for $($pair.Key). Expected $expected, found $actual."
    }
}
if ($metadata.workflowRunAttempt -notmatch '^[1-9][0-9]*$') {
    throw "Candidate workflowRunAttempt must be a positive integer."
}
if ($metadata.workflowRunAttempt -ne $expectedWorkflowRunAttempt) {
    throw "Candidate artifact attempt does not match candidate metadata."
}
if ($metadata.apkBytes -notmatch '^[1-9][0-9]*$') {
    throw "Candidate apkBytes must be a positive integer."
}

$expectedApkName = "heimdall-$ReleaseTag.apk"
if ($metadata.apkFile -ne $expectedApkName) {
    throw "Candidate APK filename mismatch. Expected $expectedApkName, found $($metadata.apkFile)."
}
$allowedFiles = @(
    $expectedApkName,
    "SHA256SUMS.txt",
    "candidate-metadata.properties",
    "$ReleaseTag-release-notes.md"
)
$actualFiles = @(
    Get-ChildItem -LiteralPath $candidatePath -File -Recurse |
        ForEach-Object { [System.IO.Path]::GetRelativePath($candidatePath, $_.FullName).Replace("\", "/") }
)
$unexpectedFiles = @($actualFiles | Where-Object { $_ -notin $allowedFiles })
$missingFiles = @($allowedFiles | Where-Object { $_ -notin $actualFiles })
if ($unexpectedFiles.Count -gt 0) {
    throw "Candidate artifact contains unexpected files: $($unexpectedFiles -join ', ')"
}
if ($missingFiles.Count -gt 0) {
    throw "Candidate artifact is missing files: $($missingFiles -join ', ')"
}

$apkPath = Join-Path $candidatePath $expectedApkName
$actualBytes = (Get-Item -LiteralPath $apkPath).Length
if ([string]$actualBytes -ne $metadata.apkBytes) {
    throw "Candidate APK byte-size mismatch. Metadata says $($metadata.apkBytes), file has $actualBytes."
}
$checksumText = (Get-Content -LiteralPath (Join-Path $candidatePath "SHA256SUMS.txt") -Raw).Trim()
if ($checksumText -ne "$approvedSha256  $expectedApkName") {
    throw "SHA256SUMS.txt does not contain the approved candidate checksum."
}

$notesSource = Join-Path $rootPath "docs\releases\$ReleaseTag.md"
$notesCopy = Join-Path $candidatePath "$ReleaseTag-release-notes.md"
if ((Get-FileHash -LiteralPath $notesSource -Algorithm SHA256).Hash -ne
    (Get-FileHash -LiteralPath $notesCopy -Algorithm SHA256).Hash) {
    throw "Candidate release notes differ from the tagged source."
}

& (Join-Path $PSScriptRoot "verify_alpha_artifact.ps1") `
    -ApkPath $apkPath `
    -ExpectedPackage $releaseValues.APPLICATION_ID `
    -ExpectedVersionCode ([string]$versionValues.VERSION_CODE) `
    -ExpectedVersionName $versionName `
    -ExpectedCertificateSha256 $expectedCertificate `
    -ExpectedApkSha256 $approvedSha256 `
    -AndroidBuildToolsVersion $AndroidBuildToolsVersion

if (-not [string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
    if (Test-Path -LiteralPath $outputPath) {
        $existing = @(Get-ChildItem -LiteralPath $outputPath -Force)
        if ($existing.Count -gt 0) {
            throw "OutputDirectory must be empty: $outputPath"
        }
    } else {
        New-Item -ItemType Directory -Path $outputPath | Out-Null
    }
    Copy-Item -LiteralPath $apkPath -Destination (Join-Path $outputPath $expectedApkName)
    Copy-Item -LiteralPath (Join-Path $candidatePath "SHA256SUMS.txt") -Destination $outputPath
}

Write-Output "Immutable Alpha candidate verification passed"
Write-Output "releaseTag=$ReleaseTag"
Write-Output "sourceCommit=$sourceCommit"
Write-Output "workflowRunId=$ExpectedWorkflowRunId"
Write-Output "artifactName=$ExpectedArtifactName"
Write-Output "approvedApkSha256=$approvedSha256"
