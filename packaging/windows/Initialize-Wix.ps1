[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$version = '3.14.1'
$expectedSha256 = '15d50463c73dce31fbea5440ac33af47e92d54d4188166d207e9e39577b8fe0f'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "target\packaging-tools\wix-$version"))
$toolsDirectory = Join-Path $targetRoot 'tools'

if ((Test-Path -LiteralPath (Join-Path $toolsDirectory 'candle.exe')) -and
    (Test-Path -LiteralPath (Join-Path $toolsDirectory 'light.exe'))) {
    Write-Host "WiX $version is already available: $toolsDirectory"
    exit 0
}

$allowedRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'target')) +
    [System.IO.Path]::DirectorySeparatorChar
if (-not $targetRoot.StartsWith($allowedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to initialize WiX outside target/: $targetRoot"
}
if (Test-Path -LiteralPath $targetRoot) {
    Remove-Item -LiteralPath $targetRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $targetRoot | Out-Null

$package = Join-Path $targetRoot "wix.$version.nupkg"
$archive = Join-Path $targetRoot 'wix.zip'
$extract = Join-Path $targetRoot 'expanded'
$uri = "https://www.nuget.org/api/v2/package/wix/$version"

Write-Host "Downloading WiX $version from NuGet..."
Invoke-WebRequest -Uri $uri -OutFile $package
$actualSha256 = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
    throw "WiX package checksum mismatch. Expected $expectedSha256, got $actualSha256"
}

Copy-Item -LiteralPath $package -Destination $archive
Expand-Archive -LiteralPath $archive -DestinationPath $extract
Move-Item -LiteralPath (Join-Path $extract 'tools') -Destination $toolsDirectory
if (-not (Test-Path -LiteralPath (Join-Path $toolsDirectory 'candle.exe')) -or
    -not (Test-Path -LiteralPath (Join-Path $toolsDirectory 'light.exe'))) {
    throw 'The verified WiX package does not contain candle.exe and light.exe.'
}

Write-Host "Verified WiX $version initialized: $toolsDirectory"
