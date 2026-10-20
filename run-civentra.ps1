$ErrorActionPreference = 'Stop'

$pomPath = Join-Path $PSScriptRoot 'pom.xml'
if (-not (Test-Path -LiteralPath $pomPath)) { throw 'The project pom.xml is missing.' }
[xml]$projectPom = Get-Content -LiteralPath $pomPath
$jarName = '{0}-{1}.jar' -f $projectPom.project.artifactId, $projectPom.project.version
$jarPath = Join-Path $PSScriptRoot (Join-Path 'target' $jarName)
$passwordPath = Join-Path $PSScriptRoot '.civentra-local\oracle-password.secure'

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw 'Build the application first with: mvn -DskipTests package'
}
if (-not (Test-Path -LiteralPath $passwordPath)) {
    throw 'The local Oracle credential is missing. Reconfigure CIVENTRA_USER before running this script.'
}

$encryptedPassword = Get-Content -LiteralPath $passwordPath -Raw
try {
    $securePassword = ConvertTo-SecureString -String $encryptedPassword
} catch {
    throw 'The saved Oracle password cannot be decrypted by this Windows account. Recreate it in this account with: Read-Host "Oracle password" -AsSecureString | ConvertFrom-SecureString | Set-Content -NoNewline .civentra-local\oracle-password.secure'
}
$env:CIVENTRA_DB_URL = 'jdbc:oracle:thin:@//localhost:1521/XEPDB1'
$env:CIVENTRA_DB_USERNAME = 'CIVENTRA_USER'
$env:CIVENTRA_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$port = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { '8081' }

try {
    & java -jar $jarPath "--server.port=$port"
} finally {
    $env:CIVENTRA_DB_PASSWORD = $null
    $securePassword.Dispose()
}
