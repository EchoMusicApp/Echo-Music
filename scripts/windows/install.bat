@echo off
REM echoMusic offline installer for Windows.
REM Double-click this file. UAC will prompt for admin (needed for cert + sideload).
REM
REM Bundle requirement (same folder as this .bat):
REM   - echomusic.crt
REM   - echomusic-<version>.msix     (any .msix matching echomusic-*.msix)
REM
REM Only x64 MSIX is shipped — Windows 11 ARM64 hosts run it under Prism
REM emulation. ARM64 native build is blocked upstream by a missing
REM androidx.sqlite-bundled windows-arm64 native binary.

setlocal EnableExtensions
cd /d "%~dp0"

REM --- 1. Self-elevate to admin -----------------------------------------------
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting admin elevation...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo === echoMusic offline installer ===
echo.

REM --- 2. Install cert to Trusted People (idempotent via certutil) -----------
if not exist "%~dp0echomusic.crt" (
    echo [ERROR] echomusic.crt not found next to install.bat
    pause
    exit /b 1
)

echo [1/3] Importing echomusic.crt to LocalMachine\TrustedPeople ...
certutil -addstore -f "TrustedPeople" "%~dp0echomusic.crt"
if %errorlevel% neq 0 (
    echo [ERROR] Certificate import failed.
    pause
    exit /b 1
)

REM --- 3. Find the bundled MSIX ----------------------------------------------
set "MSIX="
for %%f in ("%~dp0echomusic-*.msix") do set "MSIX=%%f"

if not defined MSIX (
    echo [ERROR] No echomusic-*.msix found in this folder.
    pause
    exit /b 1
)

echo.
echo [2/3] Installing/updating MSIX: "%MSIX%"
REM -ForceUpdateFromAnyVersion overwrites an existing install in place (keeps app
REM data) even when the version is identical, so no manual uninstall is needed.
REM If that still fails (e.g. signing publisher changed), fall back to removing
REM the old package and doing a clean install.
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Add-AppxPackage -Path '%MSIX%' -ForceApplicationShutdown -ForceUpdateFromAnyVersion -ErrorAction Stop; exit 0 } catch { Write-Host 'In-place update failed; removing existing echoMusic and reinstalling...'; Get-AppxPackage -Name 'echomusic' | Remove-AppxPackage -ErrorAction SilentlyContinue; try { Add-AppxPackage -Path '%MSIX%' -ForceApplicationShutdown -ErrorAction Stop; exit 0 } catch { Write-Host $_; exit 1 } }"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Add-AppxPackage failed. Common fixes:
    echo   - Enable 'Install apps from any source, including loose files'
    echo     ^(Settings -^> Privacy ^& security -^> For developers^)
    echo   - Restart Windows and run install.bat again
    pause
    exit /b 1
)

REM --- 4. Launch app ---------------------------------------------------------
echo.
echo [3/3] Launching echoMusic ...
start "" "shell:appsFolder\echomusic_ejp2bhxmz1qq6!echomusic"

echo.
echo echoMusic installed successfully.
echo To uninstall: Settings -^> Apps -^> Installed apps -^> echoMusic -^> Uninstall
echo.
pause
endlocal
