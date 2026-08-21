# Flow Phase 6 - build, test, assembleDebug, and create delivery ZIP
$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$ZipPath = Join-Path (Split-Path $ProjectRoot -Parent) "Flow-Phase6-Claude.zip"
$GradleHome = "C:\Users\dammy\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0"
$GradleBat = Join-Path $GradleHome "bin\gradle.bat"
$WrapperJar = Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.jar"
$SdkDir = "C:\Users\dammy\AppData\Local\Android\Sdk"
$LocalProps = Join-Path $ProjectRoot "local.properties"

Set-Location $ProjectRoot

if (-not (Test-Path $WrapperJar)) {
    Write-Host "gradle-wrapper.jar missing - generating wrapper..."
    if (Test-Path $GradleBat) {
        & $GradleBat wrapper --gradle-version 9.5
    } else {
        $url = "https://github.com/gradle/gradle/raw/v9.5.0/gradle/wrapper/gradle-wrapper.jar"
        New-Item -ItemType Directory -Force -Path (Split-Path $WrapperJar) | Out-Null
        Invoke-WebRequest -Uri $url -OutFile $WrapperJar
    }
}

if (-not (Test-Path $LocalProps)) {
    $sdkEscaped = $SdkDir -replace '\\', '/'
    Set-Content -Path $LocalProps -Value "sdk.dir=$sdkEscaped" -Encoding UTF8
}

Write-Host "Running unit tests..."
& .\gradlew.bat test --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Building debug APK..."
& .\gradlew.bat assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Creating ZIP at $ZipPath ..."
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }

$staging = Join-Path $env:TEMP "Flow-Phase6-staging"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
New-Item -ItemType Directory -Path $staging | Out-Null

$excludeDirs = @('.gradle', 'build', 'app\build')
Get-ChildItem -Path $ProjectRoot -Force | Where-Object {
    $_.Name -notin @('.gradle', 'build', 'local.properties', 'build-and-package.ps1')
} | ForEach-Object {
    if ($_.PSIsContainer -and $_.Name -eq 'app') {
        $destApp = Join-Path $staging 'app'
        New-Item -ItemType Directory -Path $destApp | Out-Null
        Get-ChildItem -Path $_.FullName -Force | Where-Object { $_.Name -ne 'build' } | Copy-Item -Destination $destApp -Recurse -Force
    } elseif (-not $_.PSIsContainer) {
        Copy-Item $_.FullName -Destination $staging -Force
    } elseif ($_.Name -ne 'app') {
        Copy-Item $_.FullName -Destination (Join-Path $staging $_.Name) -Recurse -Force
    }
}

Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $ZipPath -Force
Remove-Item $staging -Recurse -Force

Write-Host "ZIP created: $ZipPath"
Write-Host "DONE"
