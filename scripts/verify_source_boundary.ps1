[CmdletBinding()]
param(
    [string]$Root
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $PSScriptRoot
}

$rootPath = (Resolve-Path -LiteralPath $Root).Path
$errors = New-Object System.Collections.Generic.List[string]

$requiredPaths = @(
    "LICENSE",
    "NOTICE",
    "README.md",
    "version.properties",
    "release.properties",
    "settings.gradle",
    "assistant/build.gradle",
    "assistant/src",
    ".github/workflows/android-ci.yml",
    ".github/workflows/alpha-release.yml",
    ".github/workflows/tester-debug.yml",
    "scripts/verify_release_readiness.ps1",
    "scripts/build_alpha_candidate.ps1"
)

foreach ($relativePath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath (Join-Path $rootPath $relativePath))) {
        $errors.Add("Missing required public path: $relativePath")
    }
}

$forbiddenPaths = @(
    ".agents",
    ".codex",
    "app",
    "artifacts",
    "diagnostics",
    "skills",
    "visual reference",
    "yuyan-thor-adaptation",
    "AGENTS.md",
    "CHINESE_INPUT_STRATEGY.md",
    "MODULE_PRIORITY_MATRIX.md",
    "UI_AUDIT.md",
    "UI_AUDIT_2026-06-27.md",
    "UI_FOCUS_RULES.md",
    "UI_LAYER_HIERARCHY_SPEC.md"
)

foreach ($relativePath in $forbiddenPaths) {
    if (Test-Path -LiteralPath (Join-Path $rootPath $relativePath)) {
        $errors.Add("Forbidden private or inactive path is present: $relativePath")
    }
}

function Get-PublicSourceFiles {
    $pendingDirectories = New-Object System.Collections.Generic.Stack[System.IO.DirectoryInfo]
    $pendingDirectories.Push((Get-Item -LiteralPath $rootPath))
    $excludedDirectoryNames = @(".git", ".gradle", ".cxx", "build")

    while ($pendingDirectories.Count -gt 0) {
        $directory = $pendingDirectories.Pop()
        foreach ($entry in Get-ChildItem -LiteralPath $directory.FullName -Force) {
            if ($entry.PSIsContainer) {
                if ($entry.Name -notin $excludedDirectoryNames) {
                    $pendingDirectories.Push($entry)
                }
                continue
            }
            Write-Output $entry
        }
    }
}

$sourceFiles = @(Get-PublicSourceFiles)

$forbiddenExtensions = @(".apk", ".aab", ".keystore", ".jks", ".p12", ".pfx")
$forbiddenFiles = $sourceFiles |
    Where-Object { $forbiddenExtensions -contains $_.Extension.ToLowerInvariant() }
foreach ($file in $forbiddenFiles) {
    $errors.Add("Forbidden binary or signing material is present: $($file.FullName.Substring($rootPath.Length + 1))")
}

$settingsPath = Join-Path $rootPath "settings.gradle"
if (Test-Path -LiteralPath $settingsPath) {
    $settingsText = Get-Content -LiteralPath $settingsPath -Raw
    if ($settingsText -match '(?m)^\s*include\s+["'']?:app') {
        $errors.Add("settings.gradle includes the inactive :app module.")
    }
    if ($settingsText -match 'AnyThorKeyboard') {
        $errors.Add("settings.gradle retains the private workspace project name.")
    }
}

$textExtensions = @(
    ".gradle", ".properties", ".xml", ".java", ".cpp", ".h", ".md",
    ".yml", ".yaml", ".ps1", ".txt", ".json", ".gitignore"
)
$textFiles = $sourceFiles |
    Where-Object {
        $textExtensions -contains $_.Extension.ToLowerInvariant() -or
        $_.Name -in @("LICENSE", "NOTICE", "gradlew")
    }

$supersededIdentityPattern =
    'com\.' + 'anythor\.assistant|com/' + 'anythor/assistant|Java_com_' + 'anythor_assistant'

foreach ($file in $textFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($null -eq $text) {
        continue
    }
    $relativePath = $file.FullName.Substring($rootPath.Length + 1)
    if ($text -match '-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----') {
        $errors.Add("Private-key material pattern found in: $relativePath")
    }
    if ($text -match '(?i)\bgh[pousr]_[A-Za-z0-9_]{20,}\b') {
        $errors.Add("GitHub token pattern found in: $relativePath")
    }
    if ($text -match '(?i)\bandroiddebugkey\b|heimdall-debug\.keystore') {
        $errors.Add("Legacy debug-signing identity found in: $relativePath")
    }
    if ($text -match $supersededIdentityPattern) {
        $errors.Add("Superseded Heimdall package identity found in: $relativePath")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

$fileCount = $sourceFiles.Count
Write-Output "Public source boundary verified: $fileCount files under $rootPath"
