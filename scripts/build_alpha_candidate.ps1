[CmdletBinding()]
param(
    [string]$Root,

    [Parameter(Mandatory = $true)]
    [string]$KeystorePath,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,

    [string]$ReleaseTag,

    [string]$Alias = "heimdall-alpha"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $PSScriptRoot
}
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$keystore = (Resolve-Path -LiteralPath $KeystorePath).Path
$output = [System.IO.Path]::GetFullPath($OutputDirectory)
$rootPrefix = $rootPath.TrimEnd("\", "/") + [System.IO.Path]::DirectorySeparatorChar

if ($keystore.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "The release keystore must remain outside the Heimdall repository."
}
if (
    $output.Equals($rootPath, [System.StringComparison]::OrdinalIgnoreCase) -or
    $output.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)
) {
    throw "The signed candidate output must remain outside the Heimdall repository."
}
if ($Alias -notmatch '^[A-Za-z0-9._-]+$') {
    throw "The signing alias contains unsupported characters."
}

$versionValues = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $rootPath "version.properties") -Raw)
$releaseValues = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $rootPath "release.properties") -Raw)
$versionCode = [int]$versionValues.VERSION_CODE
$versionName = $versionValues.VERSION_NAME
$applicationId = $releaseValues.APPLICATION_ID
$expectedCertificate = $releaseValues.RELEASE_CERT_SHA256.Replace(":", "").ToUpperInvariant()
if ([string]::IsNullOrWhiteSpace($ReleaseTag)) {
    $ReleaseTag = "v$versionName"
}

$readinessScript = Join-Path $rootPath "scripts\verify_release_readiness.ps1"
& $readinessScript `
    -Root $rootPath `
    -ReleaseTag $ReleaseTag `
    -RequireCleanTree

$verifyBoundaryScript = Join-Path $rootPath "scripts\verify_source_boundary.ps1"
& $verifyBoundaryScript -Root $rootPath

$assetName = "heimdall-$ReleaseTag.apk"
$candidateApk = Join-Path $output $assetName
$checksumFile = Join-Path $output "SHA256SUMS.txt"
$metadataFile = Join-Path $output "candidate-metadata.txt"
$notesCopy = Join-Path $output "$ReleaseTag-release-notes.md"
foreach ($candidateOutput in @($candidateApk, $checksumFile, $metadataFile, $notesCopy)) {
    if (Test-Path -LiteralPath $candidateOutput) {
        throw "Refusing to overwrite an existing candidate output: $candidateOutput"
    }
}

$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
}
$androidHome = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($androidHome)) {
    $androidHome = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not (Test-Path -LiteralPath (Join-Path $javaHome "bin\java.exe") -PathType Leaf)) {
    throw "JDK 17 was not found. Set JAVA_HOME before building."
}
if (-not (Test-Path -LiteralPath $androidHome -PathType Container)) {
    throw "Android SDK was not found. Set ANDROID_HOME before building."
}

$aapt = Get-ChildItem -Path (Join-Path $androidHome "build-tools") -Recurse -Filter "aapt.exe" |
    Sort-Object FullName -Descending |
    Select-Object -First 1
$apksigner = Get-ChildItem -Path (Join-Path $androidHome "build-tools") -Recurse -Filter "apksigner.bat" |
    Sort-Object FullName -Descending |
    Select-Object -First 1
$zipalign = Get-ChildItem -Path (Join-Path $androidHome "build-tools") -Recurse -Filter "zipalign.exe" |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if ($null -eq $aapt -or $null -eq $apksigner -or $null -eq $zipalign) {
    throw "Android aapt, apksigner, or zipalign was not found."
}

$password = Read-Host "Enter the Heimdall Alpha signing password" -AsSecureString
$passwordPointer = [IntPtr]::Zero
$plainPassword = $null
$validationDirectory = $null
$previousJavaHome = $env:JAVA_HOME
$previousAndroidHome = $env:ANDROID_HOME
$previousAndroidSdkRoot = $env:ANDROID_SDK_ROOT

try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if ([string]::IsNullOrWhiteSpace($plainPassword)) {
        throw "Signing password cannot be empty."
    }

    $env:JAVA_HOME = $javaHome
    $env:ANDROID_HOME = $androidHome
    $env:ANDROID_SDK_ROOT = $androidHome
    $env:HEIMDALL_RELEASE_KEYSTORE = $keystore
    $env:HEIMDALL_RELEASE_STORE_PASSWORD = $plainPassword
    $env:HEIMDALL_RELEASE_KEY_ALIAS = $Alias
    $env:HEIMDALL_RELEASE_KEY_PASSWORD = $plainPassword

    Push-Location $rootPath
    try {
        & ".\gradlew.bat" `
            ":assistant:assembleRelease" `
            ":assistant:lintRelease" `
            "-PheimdallRequireReleaseSigning=true" `
            "--no-daemon"
        if ($LASTEXITCODE -ne 0) {
            throw "The real-signed candidate Build/Lint failed."
        }
    } finally {
        Pop-Location
    }

    $builtApk = Join-Path $rootPath "assistant\build\outputs\apk\release\heimdall-release.apk"
    if (-not (Test-Path -LiteralPath $builtApk -PathType Leaf)) {
        throw "Gradle completed without producing the expected release APK."
    }

    $validationRootCandidates = @($env:RUNNER_TEMP, $env:TEMP, $env:TMP)
    if (-not [string]::IsNullOrWhiteSpace($env:SystemRoot)) {
        $validationRootCandidates += (Join-Path $env:SystemRoot "Temp")
    }
    $validationRoot = $validationRootCandidates |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_) -and
            $_ -notmatch '[^\x00-\x7F]' -and
            (Test-Path -LiteralPath $_ -PathType Container)
        } |
        Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($validationRoot)) {
        throw "No existing ASCII-only temporary directory is available for APK validation."
    }

    $validationDirectory = Join-Path `
        $validationRoot `
        ("heimdall-candidate-validation-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $validationDirectory | Out-Null
    $validatedApk = Join-Path $validationDirectory "heimdall-release.apk"
    Copy-Item -LiteralPath $builtApk -Destination $validatedApk

    $badgingOutput = & $aapt.FullName dump badging $validatedApk
    $aaptExitCode = $LASTEXITCODE
    $badging = $badgingOutput |
        Where-Object { $_ -like "package: name=*" } |
        Select-Object -First 1
    if ($aaptExitCode -ne 0 -or $badging -notmatch "package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'") {
        throw "Could not validate candidate package metadata."
    }
    $actualPackage = $Matches[1]
    $actualVersionCode = [int]$Matches[2]
    $actualVersionName = $Matches[3]
    if (
        $actualPackage -ne $applicationId -or
        $actualVersionCode -ne $versionCode -or
        $actualVersionName -ne $versionName
    ) {
        throw "Candidate package/version does not match version.properties and release.properties."
    }

    & $apksigner.FullName verify --verbose $validatedApk
    if ($LASTEXITCODE -ne 0) {
        throw "Candidate APK signature verification failed."
    }
    $certificateOutput = (& $apksigner.FullName verify --print-certs $validatedApk) -join "`n"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not read the candidate signing certificate."
    }
    $certificateMatch = [regex]::Match(
        $certificateOutput,
        'certificate SHA-256 digest:\s*([0-9a-fA-F:]{64,95})'
    )
    if (-not $certificateMatch.Success) {
        throw "Could not parse the candidate signing certificate SHA-256."
    }
    $actualCertificate = $certificateMatch.Groups[1].Value.Replace(":", "").ToUpperInvariant()
    if ($actualCertificate -ne $expectedCertificate) {
        throw "Candidate signer does not match the approved Alpha certificate."
    }

    & $zipalign.FullName -c -P 16 -v 4 $validatedApk *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Candidate APK zip alignment verification failed."
    }

    if (-not (Test-Path -LiteralPath $output -PathType Container)) {
        New-Item -ItemType Directory -Path $output | Out-Null
    }
    Copy-Item -LiteralPath $validatedApk -Destination $candidateApk

    $apkSha256 = (Get-FileHash -LiteralPath $candidateApk -Algorithm SHA256).Hash
    $sourceCommit = (& git -C $rootPath rev-parse HEAD).Trim()
    [System.IO.File]::WriteAllText(
        $checksumFile,
        "$apkSha256  $assetName`r`n",
        [System.Text.UTF8Encoding]::new($false)
    )
    $metadata = @(
        "status=LOCAL_REAL_SIGNED_CANDIDATE_NOT_PUBLISHED",
        "releaseTag=$ReleaseTag",
        "sourceCommit=$sourceCommit",
        "package=$actualPackage",
        "versionCode=$actualVersionCode",
        "versionName=$actualVersionName",
        "certificateSha256=$actualCertificate",
        "apkSha256=$apkSha256",
        "builtAtUtc=$([DateTime]::UtcNow.ToString('o'))"
    ) -join "`r`n"
    [System.IO.File]::WriteAllText(
        $metadataFile,
        $metadata + "`r`n",
        [System.Text.UTF8Encoding]::new($false)
    )
    Copy-Item `
        -LiteralPath (Join-Path $rootPath "docs\releases\$ReleaseTag.md") `
        -Destination $notesCopy

    Write-Output ""
    Write-Output "REAL-SIGNED LOCAL CANDIDATE VERIFIED"
    Write-Output "APK: $candidateApk"
    Write-Output "APK SHA-256: $apkSha256"
    Write-Output "Certificate SHA-256: $actualCertificate"
    Write-Output "Metadata: $metadataFile"
    Write-Output "This candidate is not published and must not be tagged yet."
} finally {
    Remove-Item `
        Env:HEIMDALL_RELEASE_KEYSTORE, `
        Env:HEIMDALL_RELEASE_STORE_PASSWORD, `
        Env:HEIMDALL_RELEASE_KEY_ALIAS, `
        Env:HEIMDALL_RELEASE_KEY_PASSWORD `
        -ErrorAction SilentlyContinue
    $env:JAVA_HOME = $previousJavaHome
    $env:ANDROID_HOME = $previousAndroidHome
    $env:ANDROID_SDK_ROOT = $previousAndroidSdkRoot
    $plainPassword = $null
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    if ($null -ne $validationDirectory -and (Test-Path -LiteralPath $validationDirectory)) {
        Remove-Item -LiteralPath $validationDirectory -Recurse -Force
    }
}
