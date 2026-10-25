# CIVENTRA - Secure Account Provisioning & Password Recovery Script
# Allows administrators/operators to create or reset ADMIN, OFFICER, and PROVIDER accounts.
# Privileged passwords are typed interactively as SecureString and never stored in plain text.

$ErrorActionPreference = 'Stop'

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   CIVENTRA - Secure Account Provisioning Tool    " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

$email = Read-Host "Enter Email Address"
if ([string]::IsNullOrWhiteSpace($email)) { throw "Email cannot be empty." }
$email = $email.Trim().ToLowerInvariant()

$name = Read-Host "Enter Full Name"
if ([string]::IsNullOrWhiteSpace($name)) { throw "Full name cannot be empty." }

$phone = Read-Host "Enter 10-digit Indian Mobile Number"
if (-not ($phone -match '^\d{10}$')) { throw "Phone must be a valid 10-digit number." }

Write-Host "Select Account Role:" -ForegroundColor Yellow
Write-Host "  1) CITIZEN"
Write-Host "  2) OFFICER (Municipal Officer)"
Write-Host "  3) PROVIDER (Service Provider)"
Write-Host "  4) ADMIN (System Administrator)"
$roleChoice = Read-Host "Choose role (1-4)"

$roleId = 1
$roleName = "CITIZEN"
switch ($roleChoice) {
    "1" { $roleId = 1; $roleName = "CITIZEN" }
    "2" { $roleId = 2; $roleName = "MUNICIPAL_OFFICER" }
    "3" { $roleId = 3; $roleName = "SERVICE_PROVIDER" }
    "4" { $roleId = 4; $roleName = "ADMIN" }
    default { throw "Invalid role selection." }
}

$securePassword = Read-Host "Enter Password (min 8 chars)" -AsSecureString
$plainPassword = [System.Net.NetworkCredential]::new('', $securePassword).Password
if ($plainPassword.Length -lt 8) {
    $securePassword.Dispose()
    throw "Password must be at least 8 characters long."
}

# Generate BCrypt Hash using project's Spring Security BCrypt encoder
$pomPath = Join-Path $PSScriptRoot 'pom.xml'
$jarPath = Join-Path $PSScriptRoot 'target\civentra-1.0.0-SNAPSHOT.jar'

if (-not (Test-Path -LiteralPath $jarPath)) {
    Write-Host "Application jar not found. Building target..." -ForegroundColor DarkYellow
    mvn '-Dmaven.test.skip=true' package
}

# Run Java BCrypt encoder
$hashProcess = Start-Process -FilePath "java" -ArgumentList "-cp", "$jarPath", "com.civentra.util.SecurityUtil" -NoNewWindow -PassThru -Wait
# Or generate directly using Spring Boot / Java snippet
$escapedPwd = $plainPassword.Replace('\', '\\').Replace('"', '\"')
$javaCode = @"
public class HashGen {
    public static void main(String[] args) {
        System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(args[0]));
    }
}
"@

$tempJava = [System.IO.Path]::GetTempFileName() + ".java"
[System.IO.File]::WriteAllText($tempJava, $javaCode)
try {
    $bcryptHash = & java -cp "$jarPath" $tempJava "$escapedPwd"
} finally {
    Remove-Item -Force $tempJava -ErrorAction SilentlyContinue
    $plainPassword = $null
    $securePassword.Dispose()
}

$bcryptHash = $bcryptHash.Trim()
if (-not ($bcryptHash -match '^\$2[aby]\$')) {
    throw "Failed to generate BCrypt hash."
}

Write-Host "Applying secure credentials to Oracle XE schema CIVENTRA_USER..." -ForegroundColor Yellow

$sqlCommand = @"
SET ECHO OFF;
SET FEEDBACK OFF;
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = CIVENTRA_USER;

MERGE INTO USERS u
USING (SELECT '$email' AS EMAIL FROM DUAL) s
ON (LOWER(TRIM(u.EMAIL)) = s.EMAIL)
WHEN MATCHED THEN
  UPDATE SET u.FULL_NAME = '$name', u.PHONE_NUMBER = '$phone', u.PASSWORD_HASH = '$bcryptHash', u.ROLE_ID = $roleId, u.STATUS = 'ACTIVE'
WHEN NOT MATCHED THEN
  INSERT (USER_ID, FULL_NAME, EMAIL, PHONE_NUMBER, PASSWORD_HASH, ROLE_ID, STATUS)
  VALUES (SEQ_USERS.NEXTVAL, '$name', '$email', '$phone', '$bcryptHash', $roleId, 'ACTIVE');

COMMIT;
EXIT;
"@

$sqlCommand | sqlplus -s / as sysdba

Write-Host ""
Write-Host "SUCCESS: Account provisioned successfully!" -ForegroundColor Green
Write-Host "  Email:  $email"
Write-Host "  Role:   $roleName (ID: $roleId)"
Write-Host "  Status: ACTIVE"
Write-Host "  Hash:   Stored as BCrypt"
Write-Host ""
