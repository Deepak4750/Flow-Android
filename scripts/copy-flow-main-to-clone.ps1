# Copies the main Flow app's Keep Data backup into the Dual Apps clone profile.
# Read-only on the main app. Clears clone app data so restore runs on next launch.
#
# Requires USB debugging and the same APK on both profiles (com.deepak.flow).

$ErrorActionPreference = "Stop"

$adb = "C:\Users\dammy\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$package = "com.deepak.flow"
$cloneUserId = 999
$mainKeepDir = "/storage/emulated/0/Documents/Flow"
$cloneKeepDir = "/storage/emulated/999/Documents/Flow"
$tmpDir = Join-Path $env:TEMP "flow-clone-copy"
$tmpDb = Join-Path $tmpDir "flow-keep.db"
$tmpMeta = Join-Path $tmpDir "flow-keep.meta"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb"
}

$deviceLines = @(
    & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
)
if (-not $deviceLines -or $deviceLines.Count -eq 0) {
    Write-Error "No adb device connected."
}

$serials = @($deviceLines | ForEach-Object { ($_ -split "\t")[0].Trim() } | Where-Object { $_ })
$preferred = $serials | Where-Object { $_ -eq "P122BT001107" } | Select-Object -First 1
if ($serials.Count -gt 1 -and -not $preferred) {
    Write-Error "Multiple devices connected ($($serials -join ', ')); disconnect extras or use A063."
}
$serial = if ($preferred) { $preferred } else { $serials[0] }
$adbArgs = @("-s", $serial)

function Invoke-AdbShell([string]$Command) {
    & $adb @adbArgs shell $Command
    if ($LASTEXITCODE -ne 0) {
        throw "adb shell failed: $Command"
    }
}

Write-Host "Using device: $($adbArgs[1])"
Write-Host "Finding newest main Keep Data backup..."

$newestRemote = (& $adb @adbArgs shell "ls -t $mainKeepDir/flow-keep*.db $mainKeepDir/flow-keep.db 2>/dev/null | head -1").Trim()
if (-not $newestRemote) {
    Write-Error "No flow-keep backup found under $mainKeepDir on the main profile."
}

Write-Host "Main backup: $newestRemote"

New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
Remove-Item $tmpDb -ErrorAction SilentlyContinue
Remove-Item $tmpMeta -ErrorAction SilentlyContinue

Write-Host "Pulling main backup (read-only)..."
& $adb @adbArgs pull $newestRemote $tmpDb
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $tmpDb)) {
    Write-Error "Failed to pull main Keep Data backup."
}

$size = (Get-Item $tmpDb).Length
Write-Host "Pulled $size bytes."

@{"ownerUserId" = $cloneUserId} | ConvertTo-Json -Compress | Set-Content -Path $tmpMeta -Encoding ascii -NoNewline

Write-Host "Stopping clone Flow and clearing clone app data..."
Invoke-AdbShell "am force-stop --user $cloneUserId $package"
Invoke-AdbShell "pm clear --user $cloneUserId $package"

Write-Host "Writing Keep Data into clone Documents/Flow..."
Invoke-AdbShell "mkdir -p $cloneKeepDir"

& $adb @adbArgs push $tmpDb "$cloneKeepDir/flow-keep.db"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push flow-keep.db to clone profile."
}

& $adb @adbArgs push $tmpMeta "$cloneKeepDir/flow-keep.meta"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push flow-keep.meta to clone profile."
}

Write-Host "Scanning clone Keep Data into MediaStore..."
Invoke-AdbShell "am broadcast --user $cloneUserId -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$cloneKeepDir/flow-keep.db"
Invoke-AdbShell "am broadcast --user $cloneUserId -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$cloneKeepDir/flow-keep.meta"
Start-Sleep -Seconds 2

Write-Host "Launching clone Flow to restore..."
Invoke-AdbShell "am start --user $cloneUserId -n $package/.MainActivity"

Write-Host "Done. Open the clone Flow app and confirm tasks, gym, and history match main."
Write-Host "Main app data was not modified."
