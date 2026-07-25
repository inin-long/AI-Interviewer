# Windows packaging

The scripts in this directory create a self-contained Windows distribution with JDK 21 `jpackage`.
The application, bundled Java runtime, user data, logs, and configuration remain separate:

- application image or installer: the selected output directory;
- user data: `%LOCALAPPDATA%\AI-Interviewer`;
- override: `AI_INTERVIEWER_HOME`;
- AI secrets: external environment variables or `application-local.yml`, never packaged.

## Build an app image

```powershell
.\packaging\windows\Build-Package.ps1 -Type app-image
.\packaging\windows\Test-AppImage.ps1
```

The smoke test starts the packaged application twice against the same temporary `AI_INTERVIEWER_HOME`.
It verifies fresh Flyway migration, background worker startup, program/data separation, and retention of
external user files across restart.

The default output is `target\dist\AI Interviewer`. It contains the executable and a private Java runtime,
so the destination computer does not need a separately installed JDK.
`SHA256SUMS.txt` is refreshed whenever file artifacts are present in the output directory.

## Build a portable ZIP

```powershell
.\packaging\windows\Build-Package.ps1 -Type portable
```

## Build an installer

Windows EXE/MSI generation requires WiX Toolset commands `candle.exe` and `light.exe`.
The build can download a pinned WiX 3.14.1 NuGet package, verify its SHA-256, and extract it under
`target\packaging-tools` without installing or modifying the system:

```powershell
.\packaging\windows\Build-Package.ps1 -Type exe -DownloadWix
# or
.\packaging\windows\Build-Package.ps1 -Type msi -DownloadWix
```

If WiX is already on `PATH`, omit `-DownloadWix`.

Installers use per-user installation, an installation-directory chooser, Start Menu and desktop shortcuts,
and stable upgrade UUID `ecbbaa06-b63b-4be3-a175-f1a5a9711443`. Upgrades replace program files but do not
write to or remove `%LOCALAPPDATA%\AI-Interviewer`.

Use `-SkipMavenBuild` only after a successful `mvnw clean package` in the current working tree.

## Release checklist

- Verify `packaging\windows\AI Interviewer.ico` renders correctly at common Windows launcher sizes.
- Sign the EXE/MSI with the project certificate before public distribution; current development artifacts are unsigned.
- Install on a clean Windows user account and verify first launch, shortcuts, uninstall, upgrade, and retained
  `%LOCALAPPDATA%\AI-Interviewer` data before declaring a production release.
