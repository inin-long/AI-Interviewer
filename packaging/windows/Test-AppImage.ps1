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
$database = Join-Path $smokeRoot 'database\app.db'
$log = Join-Path $smokeRoot 'logs\app.log'
$sentinel = Join-Path $smokeRoot 'users\release-smoke-retention.txt'

function Start-AndVerifyApplication {
    param([Parameter(Mandatory)][int]$ExpectedStartupCount)

    $process = $null
    try {
        $process = Start-Process -FilePath $executable -WorkingDirectory $appImagePath -PassThru
        $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
        $started = $false
        $windowShown = $false
        while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
            if ((Test-Path -LiteralPath $database) -and (Test-Path -LiteralPath $log)) {
                $content = Get-Content -LiteralPath $log -Raw -ErrorAction SilentlyContinue
                $startupCount = [regex]::Matches($content, 'Started application').Count
                if ($startupCount -ge $ExpectedStartupCount) {
                    $started = $true
                }
            }
            $process.Refresh()
            $candidateProcessIds = @($process.Id)
            $candidateProcessIds += @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($process.Id)" |
                Select-Object -ExpandProperty ProcessId)
            foreach ($candidateProcessId in $candidateProcessIds) {
                $candidateProcess = Get-Process -Id $candidateProcessId -ErrorAction SilentlyContinue
                if ($null -ne $candidateProcess) {
                    $candidateProcess.Refresh()
                    if ($candidateProcess.MainWindowTitle -eq 'AI Interviewer') {
                        $windowShown = $true
                        break
                    }
                }
            }
            if ($started -and $windowShown) {
                break
            }
            Start-Sleep -Milliseconds 500
        }

        if (-not $started) {
            throw "Packaged application did not finish startup #$ExpectedStartupCount. Inspect: $smokeRoot"
        }
        if (-not $windowShown) {
            throw "Packaged application did not show its JavaFX main window during startup #$ExpectedStartupCount. Inspect: $smokeRoot"
        }
        return (Get-Content -LiteralPath $log -Raw)
    } finally {
        if ($null -ne $process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
            $process.WaitForExit()
        }
    }
}

try {
    $unexpectedRuntimeFiles = @(Get-ChildItem -LiteralPath $appImagePath -Recurse -File -Force |
        Where-Object { $_.Name -in @('app.db', 'app.log', 'application-local.yml') })
    if ($unexpectedRuntimeFiles.Count -gt 0) {
        throw "Application image contains user data or local configuration: $($unexpectedRuntimeFiles.FullName -join ', ')"
    }

    $firstLog = Start-AndVerifyApplication -ExpectedStartupCount 1
    if ($firstLog -notmatch 'Started 2 background task worker') {
        throw "Background task workers did not start. Inspect: $smokeRoot"
    }
    if ($firstLog -notmatch 'Successfully applied \d+ migrations') {
        throw "Fresh database migrations did not complete. Inspect: $smokeRoot"
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $sentinel) | Out-Null
    [System.IO.File]::WriteAllText($sentinel, 'preserve-across-restart', [System.Text.UTF8Encoding]::new($false))

    $secondLog = Start-AndVerifyApplication -ExpectedStartupCount 2
    if ($secondLog -match '(?im)^.*(?:migration|flyway).*(?:error|failed).*$') {
        throw "Database validation failed during restart. Inspect: $smokeRoot"
    }
    $sentinelRetained = (Test-Path -LiteralPath $sentinel) -and
        ((Get-Content -LiteralPath $sentinel -Raw) -eq 'preserve-across-restart')
    if (-not $sentinelRetained) {
        throw "External user data was not retained across restart. Inspect: $smokeRoot"
    }
    if ((Get-Item -LiteralPath $database).Length -le 0) {
        throw "Database file is empty after restart. Inspect: $smokeRoot"
    }
    Write-Host "App image first-start and restart-retention smoke test passed. Runtime data: $smokeRoot"
} finally {
    $env:AI_INTERVIEWER_HOME = $previousHome
}
