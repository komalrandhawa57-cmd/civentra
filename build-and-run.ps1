$ErrorActionPreference = 'Stop'

$pomPath = Join-Path $PSScriptRoot 'pom.xml'
if (-not (Test-Path -LiteralPath $pomPath)) { throw 'The project pom.xml is missing.' }
[xml]$projectPom = Get-Content -LiteralPath $pomPath
$jarName = '{0}-{1}.jar' -f $projectPom.project.artifactId, $projectPom.project.version
$jarPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot (Join-Path 'target' $jarName)))
$javaProcesses = @()
try {
    $javaProcesses = @(Get-CimInstance Win32_Process -Filter "name = 'java.exe'" -ErrorAction Stop |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jarPath, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 })
} catch {
    Write-Warning 'Windows did not allow querying Java processes. Stop CIVENTRA manually if it is already running before rebuilding.'
}

foreach ($javaProcess in $javaProcesses) {
    Write-Host "Stopping CIVENTRA (PID $($javaProcess.ProcessId)) so Maven can replace its JAR..."
    try { & taskkill.exe /PID $javaProcess.ProcessId 2>$null | Out-Null } catch { }
    Start-Sleep -Seconds 2
    $stillRunning = Get-Process -Id $javaProcess.ProcessId -ErrorAction SilentlyContinue
    if ($stillRunning) {
        Stop-Process -Id $javaProcess.ProcessId -Force
    }
}

Push-Location $PSScriptRoot
try {
    Write-Host 'Building CIVENTRA...'
    $mavenSettingsPath = Join-Path $PSScriptRoot 'target\maven-settings.xml'
    if (Test-Path -LiteralPath $mavenSettingsPath) {
        & mvn.cmd -s $mavenSettingsPath '-Dmaven.test.skip=true' package
    } else {
        & mvn.cmd '-Dmaven.test.skip=true' package
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Write-Host 'Build succeeded. Starting CIVENTRA; press Ctrl+C here to stop it.'
& (Join-Path $PSScriptRoot 'run-civentra.ps1')
