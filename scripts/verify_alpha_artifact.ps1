[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedPackage,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedVersionCode,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedVersionName,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedCertificateSha256,

    [string]$ExpectedApkSha256,
    [string]$AndroidBuildToolsVersion = "35.0.0"
)

$ErrorActionPreference = "Stop"

function Normalize-Sha256 {
    param(
        [Parameter(Mandatory = $true)][string]$Value,
        [Parameter(Mandatory = $true)][string]$Label
    )
    $normalized = $Value.Replace(":", "").Trim().ToUpperInvariant()
    if ($normalized -notmatch '^[0-9A-F]{64}$') {
        throw "$Label must contain exactly 64 hexadecimal characters."
    }
    return $normalized
}

function Resolve-AndroidTool {
    param([Parameter(Mandatory = $true)][string]$Name)

    $androidHome = $env:ANDROID_HOME
    if ([string]::IsNullOrWhiteSpace($androidHome)) {
        $androidHome = $env:ANDROID_SDK_ROOT
    }
    if ([string]::IsNullOrWhiteSpace($androidHome) -and -not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $androidHome = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
    if ([string]::IsNullOrWhiteSpace($androidHome)) {
        throw "Set ANDROID_HOME or ANDROID_SDK_ROOT before APK verification."
    }

    $toolRoot = Join-Path $androidHome "build-tools\$AndroidBuildToolsVersion"
    foreach ($candidate in @($Name, "$Name.exe", "$Name.bat")) {
        $path = Join-Path $toolRoot $candidate
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            return $path
        }
    }
    throw "Android build-tools $AndroidBuildToolsVersion does not contain $Name."
}

$apk = (Resolve-Path -LiteralPath $ApkPath).Path
$expectedCertificate = Normalize-Sha256 $ExpectedCertificateSha256 "ExpectedCertificateSha256"
$actualApkSha256 = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToUpperInvariant()
if (-not [string]::IsNullOrWhiteSpace($ExpectedApkSha256)) {
    $expectedApk = Normalize-Sha256 $ExpectedApkSha256 "ExpectedApkSha256"
    if ($actualApkSha256 -ne $expectedApk) {
        throw "APK SHA-256 mismatch. Expected $expectedApk, found $actualApkSha256."
    }
}

$aapt = Resolve-AndroidTool "aapt"
$apksigner = Resolve-AndroidTool "apksigner"
$zipalign = Resolve-AndroidTool "zipalign"

$badgingOutput = (& $aapt dump badging $apk 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) {
    throw "aapt dump badging failed for $apk`n$badgingOutput"
}
$packageMatch = [regex]::Match(
    $badgingOutput,
    "(?m)^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'"
)
if (-not $packageMatch.Success) {
    throw "Could not parse package metadata from aapt badging output."
}
$actualPackage = $packageMatch.Groups[1].Value
$actualVersionCode = $packageMatch.Groups[2].Value
$actualVersionName = $packageMatch.Groups[3].Value
if ($actualPackage -ne $ExpectedPackage) {
    throw "Package mismatch. Expected $ExpectedPackage, found $actualPackage."
}
if ($actualVersionCode -ne $ExpectedVersionCode) {
    throw "Version code mismatch. Expected $ExpectedVersionCode, found $actualVersionCode."
}
if ($actualVersionName -ne $ExpectedVersionName) {
    throw "Version name mismatch. Expected $ExpectedVersionName, found $actualVersionName."
}

$manifestOutput = (& $aapt dump xmltree $apk AndroidManifest.xml 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) {
    throw "aapt could not inspect AndroidManifest.xml.`n$manifestOutput"
}
if ($manifestOutput -notmatch 'android:extractNativeLibs\([^\)]*\)=\(type 0x12\)0x0') {
    throw "AndroidManifest.xml must explicitly package native libraries with extractNativeLibs=false."
}

$signatureOutput = (& $apksigner verify --verbose --print-certs $apk 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed.`n$signatureOutput"
}
if ($signatureOutput -notmatch '(?m)^Verified using v2 scheme \(APK Signature Scheme v2\): true\s*$') {
    throw "APK Signature Scheme v2 verification did not pass."
}
$signerMatch = [regex]::Match($signatureOutput, '(?m)^Number of signers: ([0-9]+)\s*$')
if (-not $signerMatch.Success -or $signerMatch.Groups[1].Value -ne "1") {
    throw "The APK must contain exactly one signer."
}
$certificateMatch = [regex]::Match(
    $signatureOutput,
    '(?im)^(?:Signer #1|V2 Signer:) certificate SHA-256 digest:\s*([0-9a-f:]+)\s*$'
)
if (-not $certificateMatch.Success) {
    throw "Could not parse the APK signing certificate SHA-256."
}
$actualCertificate = Normalize-Sha256 $certificateMatch.Groups[1].Value "APK certificate SHA-256"
if ($actualCertificate -ne $expectedCertificate) {
    throw "APK signer mismatch. Expected $expectedCertificate, found $actualCertificate."
}

$zipalignOutput = (& $zipalign -c -P 16 -v 4 $apk 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) {
    throw "zipalign 4K/16K verification failed.`n$zipalignOutput"
}

$pythonCommand = $null
$pythonNames = if ([System.IO.Path]::DirectorySeparatorChar -eq "\") {
    @("python", "python3")
} else {
    @("python3", "python")
}
foreach ($pythonName in $pythonNames) {
    $candidate = Get-Command $pythonName -ErrorAction SilentlyContinue
    if ($null -eq $candidate) {
        continue
    }
    try {
        & $candidate.Source --version *> $null
        if ($LASTEXITCODE -eq 0) {
            $pythonCommand = $candidate
            break
        }
    } catch {
        continue
    }
}
if ($null -eq $pythonCommand) {
    throw "Python 3 is required for native-library ELF alignment verification."
}
$nativeScript = Join-Path $PSScriptRoot "inspect_apk_native_alignment.py"
& $pythonCommand.Source $nativeScript $apk --page-size 16384
if ($LASTEXITCODE -ne 0) {
    throw "Native-library compression or 16K alignment verification failed."
}

$apkBytes = (Get-Item -LiteralPath $apk).Length
Write-Output "Alpha APK verification passed"
Write-Output "apk=$apk"
Write-Output "apkBytes=$apkBytes"
Write-Output "apkSha256=$actualApkSha256"
Write-Output "package=$actualPackage"
Write-Output "versionCode=$actualVersionCode"
Write-Output "versionName=$actualVersionName"
Write-Output "certificateSha256=$actualCertificate"
Write-Output "signerCount=1"
Write-Output "signatureSchemeV2=true"
Write-Output "extractNativeLibs=false"
Write-Output "zipAlignment=4K/16K"
