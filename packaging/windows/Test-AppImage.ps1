[CmdletBinding()]
param(
    [string]$AppImage = "$(Join-Path $PSScriptRoot '..\..\target\dist\AI Interviewer')",
    [int]$StartupTimeoutSeconds = 15
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$appImagePath = [System.IO.Path]::GetFullPath($AppImage)
$executable = Join-Path $appImagePath 'AI Interviewer.exe'
if (-not (Test-Path -LiteralPath $executable)) {
    throw "Packaged launcher was not found: $executable"
}

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$smokeRoot = Join-Path $projectRoot ('target\package-smoke-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $smokeRoot | Out-Null
$previousHome = $env:AI_INTERVIEWER_HOME
$env:AI_INTERVIEWER_HOME = $smokeRoot

try {
    $process = Start-Process -FilePath $executable -PassThru -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    $database = Join-Path $smokeRoot 'database\app.db'
    $log = Join-Path $smokeRoot 'logs\app.log'
    while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
        if ((Test-Path -LiteralPath $database) -and (Test-Path -LiteralPath $log)) {
            $content = Get-Content -LiteralPath $log -Raw -ErrorAction SilentlyContinue
            if ($content -match 'Started application') { break }
        }
        Start-Sleep -Milliseconds 500
    }
    $running = -not $process.HasExited
    $logText = if (Test-Path -LiteralPath $log) { Get-Content -LiteralPath $log -Raw } else { '' }
    if ($running) {
        Stop-Process -Id $process.Id
        $process.WaitForExit()
    }
    if (-not $running -or $logText -notmatch 'Started application') {
        throw "Packaged application did not finish startup. Inspect: $smokeRoot"
    }
    if ($logText -notmatch 'Started 2 background task worker') {
        throw "Background task workers did not start. Inspect: $smokeRoot"
    }
    Write-Host "App image smoke test passed. Runtime data: $smokeRoot"
} finally {
    $env:AI_INTERVIEWER_HOME = $previousHome
    if (Get-Variable process -ErrorAction SilentlyContinue) {
        if ($null -ne $process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
        }
    }
}
