[CmdletBinding()]
param(
    [ValidateSet('app-image', 'portable', 'exe', 'msi')]
    [string]$Type = 'app-image',

    [switch]$SkipMavenBuild,

    [switch]$DownloadWix,

    [string]$OutputDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..\..'))
$targetDirectory = Join-Path $projectRoot 'target'
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $targetDirectory 'dist'
}
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)

function Reset-WorkspaceDirectory {
    param([Parameter(Mandatory)][string]$Path)
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $allowedRoot = [System.IO.Path]::GetFullPath($targetDirectory) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $fullPath.StartsWith($allowedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to reset a directory outside target/: $fullPath"
    }
    if (Test-Path -LiteralPath $fullPath) {
        Remove-Item -LiteralPath $fullPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $fullPath | Out-Null
    return $fullPath
}

$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    $javaHome = 'C:\Program Files\Java\jdk-21.0.2'
}
$jpackage = Join-Path $javaHome 'bin\jpackage.exe'
$java = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $jpackage)) {
    throw "JDK 21 jpackage was not found: $jpackage"
}
$javaVersion = & $java --version | Select-Object -First 1
if ($javaVersion -notmatch '(^|\s)21\.') {
    throw "Packaging requires JDK 21, current runtime: $javaVersion"
}

Push-Location $projectRoot
try {
    if (-not $SkipMavenBuild) {
        $env:JAVA_HOME = $javaHome
        $env:Path = "$(Join-Path $javaHome 'bin');$env:Path"
        & '.\mvnw.cmd' clean package
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE" }
    }

    $jar = Get-ChildItem -LiteralPath $targetDirectory -Filter 'ai-interviewer-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' -and $_.Name -notlike '*-sources.jar' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $jar) { throw 'Packaged Spring Boot JAR was not found under target/.' }

    [xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
    $namespaces = [System.Xml.XmlNamespaceManager]::new($pom.NameTable)
    $namespaces.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
    $versionNode = $pom.SelectSingleNode('/m:project/m:version', $namespaces)
    if ($null -eq $versionNode) { throw 'Cannot determine application version from pom.xml.' }
    $appVersion = $versionNode.InnerText -replace '-SNAPSHOT$', ''

    $workRoot = Reset-WorkspaceDirectory (Join-Path $targetDirectory 'jpackage-work')
    $inputDirectory = Reset-WorkspaceDirectory (Join-Path $workRoot 'input')
    Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $inputDirectory 'ai-interviewer.jar')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSE-NOTICE.md') -Destination $inputDirectory

    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $imageOutput = if ($Type -eq 'portable') {
        Reset-WorkspaceDirectory (Join-Path $workRoot 'portable-image')
    } else {
        $OutputDirectory
    }
    if ($Type -eq 'app-image') {
        $existingImage = Join-Path $imageOutput 'AI Interviewer'
        if (Test-Path -LiteralPath $existingImage) {
            $fullImage = [System.IO.Path]::GetFullPath($existingImage)
            $allowedOutput = [System.IO.Path]::GetFullPath($OutputDirectory) + [System.IO.Path]::DirectorySeparatorChar
            if (-not $fullImage.StartsWith($allowedOutput, [System.StringComparison]::OrdinalIgnoreCase)) {
                throw "Refusing to remove app image outside output directory: $fullImage"
            }
            Remove-Item -LiteralPath $fullImage -Recurse -Force
        }
    }

    $packageType = if ($Type -eq 'portable') { 'app-image' } else { $Type }
    if ($packageType -in @('exe', 'msi')) {
        $localWix = Join-Path $targetDirectory 'packaging-tools\wix-3.14.1\tools'
        if ((-not (Get-Command 'candle.exe' -ErrorAction SilentlyContinue)) -and $DownloadWix) {
            & (Join-Path $scriptDirectory 'Initialize-Wix.ps1')
        }
        if (Test-Path -LiteralPath (Join-Path $localWix 'candle.exe')) {
            $env:Path = "$localWix;$env:Path"
        }
        $candle = Get-Command 'candle.exe' -ErrorAction SilentlyContinue
        $light = Get-Command 'light.exe' -ErrorAction SilentlyContinue
        if ($null -eq $candle -or $null -eq $light) {
            throw 'EXE/MSI packaging requires WiX Toolset (candle.exe and light.exe). Use -DownloadWix for a verified workspace-local copy, or install WiX on PATH.'
        }
    }

    $runtimeModules = @(
        'java.base', 'java.desktop', 'java.logging', 'java.management', 'java.naming',
        'java.net.http', 'java.prefs', 'java.scripting', 'java.security.jgss', 'java.sql', 'java.xml',
        'jdk.charsets', 'jdk.crypto.ec', 'jdk.jsobject', 'jdk.unsupported', 'jdk.zipfs'
    ) -join ','

    $jpackageArguments = @(
        '--type', $packageType,
        '--name', 'AI Interviewer',
        '--dest', $imageOutput,
        '--input', $inputDirectory,
        '--main-jar', 'ai-interviewer.jar',
        '--main-class', 'org.springframework.boot.loader.launch.JarLauncher',
        '--app-version', $appVersion,
        '--vendor', 'inin',
        '--description', 'Local AI technical interview desktop assistant',
        '--copyright', 'Copyright (c) 2026 inin',
        '--java-options', '-Dfile.encoding=UTF-8',
        '--java-options', '-Dprism.order=sw',
        '--add-modules', $runtimeModules
    )

    $icon = Join-Path $scriptDirectory 'AI Interviewer.ico'
    if (Test-Path -LiteralPath $icon) {
        $jpackageArguments += @('--icon', $icon)
    }
    if ($packageType -in @('exe', 'msi')) {
        $jpackageArguments += @(
            '--install-dir', 'AI Interviewer',
            '--win-per-user-install',
            '--win-dir-chooser',
            '--win-menu',
            '--win-menu-group', 'AI Interviewer',
            '--win-shortcut',
            '--win-upgrade-uuid', 'ecbbaa06-b63b-4be3-a175-f1a5a9711443'
        )
    }

    & $jpackage @jpackageArguments
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

    # jpackage marks Windows launcher binaries read-only. Clear that flag so a later
    # `mvn clean` can remove target/ without requiring manual intervention.
    Get-ChildItem -LiteralPath $imageOutput -Recurse -File -Force |
        Where-Object { $_.IsReadOnly } |
        ForEach-Object { $_.IsReadOnly = $false }

    if ($Type -eq 'portable') {
        $image = Join-Path $imageOutput 'AI Interviewer'
        $archive = Join-Path $OutputDirectory "AI-Interviewer-$appVersion-windows-x64.zip"
        if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive -Force }
        Compress-Archive -LiteralPath $image -DestinationPath $archive -CompressionLevel Optimal
        Write-Host "Portable package created: $archive"
    } else {
        Write-Host "Package created under: $OutputDirectory"
    }

    $artifacts = @(Get-ChildItem -LiteralPath $OutputDirectory -File |
        Where-Object { $_.Extension -in @('.exe', '.msi', '.zip') } |
        Sort-Object Name)
    if ($artifacts.Count -gt 0) {
        $checksumLines = foreach ($artifact in $artifacts) {
            $hash = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "$hash *$($artifact.Name)"
        }
        $checksumFile = Join-Path $OutputDirectory 'SHA256SUMS.txt'
        [System.IO.File]::WriteAllLines(
            $checksumFile,
            [string[]]$checksumLines,
            [System.Text.UTF8Encoding]::new($false))
        Write-Host "Checksums updated: $checksumFile"
    }
} finally {
    Pop-Location
}
