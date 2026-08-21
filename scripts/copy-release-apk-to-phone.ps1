# Copies the sideload APK onto phone Download and installs it over the
# current build when a device is plugged in with USB debugging.
# If the phone is not connected, skip - do not copy to Desktop or anywhere else.

$ErrorActionPreference = "Stop"

$adb = "C:\Users\dammy\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\release\app-release.apk"
$remote = "/sdcard/Download/app-release.apk"

if (-not (Test-Path $adb)) {
    Write-Host "adb not found; skip phone copy."
    exit 0
}

if (-not (Test-Path $apk)) {
    Write-Host "APK not found at $apk; skip phone copy."
    exit 0
}

$apk = (Resolve-Path $apk).Path
$devices = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }

if (-not $devices) {
    Write-Host "A063 not connected; skip phone copy."
    exit 0
}

Write-Host "Installing $apk"
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed; local APK is still at $apk"
    exit 0
}
Write-Host "App updated on phone."

Write-Host "Replacing $remote"
& $adb push $apk $remote
if ($LASTEXITCODE -ne 0) {
    Write-Host "Phone Download copy failed; app install already succeeded."
    exit 0
}
Write-Host "Phone Download updated."
