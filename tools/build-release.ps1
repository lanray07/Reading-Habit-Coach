param(
    [string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$SigningDirectory = "$env:LOCALAPPDATA\ReadingHabitCoach\signing",
    [string]$OutputDirectory = '.release\production'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release build or verification failed.' }
    $buildTools = Join-Path $SdkPath 'build-tools\36.0.0'
    $keyFile = Join-Path $SigningDirectory 'reading-habit-coach-release.p12'
    $passwordFile = Join-Path $SigningDirectory 'password.dpapi'
    if ((Test-Path -LiteralPath $keyFile) -xor (Test-Path -LiteralPath $passwordFile)) {
        throw 'Incomplete signing material: restore the existing key/password pair; do not replace the key.'
    }
    New-Item -ItemType Directory -Force -Path $SigningDirectory | Out-Null
    # Limit this directory to the current Windows identity and SYSTEM.
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $SigningDirectory /inheritance:r /grant:r "${identity}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not protect signing directory.' }
    if (!(Test-Path -LiteralPath $keyFile)) {
        $random = New-Object byte[] 48
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($random) } finally { $rng.Dispose() }
        $securePassword = ConvertTo-SecureString ([Convert]::ToBase64String($random)) -AsPlainText -Force
        $securePassword | ConvertFrom-SecureString | Set-Content -LiteralPath $passwordFile
    } else {
        $securePassword = Get-Content -LiteralPath $passwordFile -Raw | ConvertTo-SecureString
    }
    $credential = New-Object System.Management.Automation.PSCredential('release', $securePassword)
    $env:RHC_RELEASE_STORE_PASSWORD = $credential.GetNetworkCredential().Password
    try {
        if (!(Test-Path -LiteralPath $keyFile)) {
            & keytool.exe -genkeypair -keystore $keyFile -storetype PKCS12 -alias reading-habit-coach -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 10000 -dname 'CN=O. Bankole, OU=Reading Habit Coach' -storepass:env RHC_RELEASE_STORE_PASSWORD -keypass:env RHC_RELEASE_STORE_PASSWORD
            if ($LASTEXITCODE -ne 0) { throw 'Release key generation failed. Preserve signing files for recovery.' }
        }
        New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
        $aligned = Join-Path $OutputDirectory 'reading-habit-coach-aligned-unsigned.apk'
        $signed = Join-Path $OutputDirectory 'reading-habit-coach-0.2.0-release.apk'
        & "$buildTools\zipalign.exe" -P 16 -f 4 'app\build\outputs\apk\release\app-release-unsigned.apk' $aligned
        if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed.' }
        & "$buildTools\apksigner.bat" sign --ks $keyFile --ks-key-alias reading-habit-coach --ks-pass env:RHC_RELEASE_STORE_PASSWORD --key-pass env:RHC_RELEASE_STORE_PASSWORD --out $signed $aligned
        if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
        & "$buildTools\apksigner.bat" verify --verbose --print-certs $signed
        if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
        & "$buildTools\zipalign.exe" -c -P 16 -v 4 $signed
        if ($LASTEXITCODE -ne 0) { throw 'Signed APK alignment verification failed.' }
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $signed).Hash.ToLowerInvariant()
        "$hash  $([IO.Path]::GetFileName($signed))" | Set-Content -LiteralPath (Join-Path $OutputDirectory 'SHA256SUMS.txt')
        Write-Output "Signed APK: $signed"
        Write-Output "Signing key retained outside Git: $keyFile"
        Write-Output 'Password protected with Windows DPAPI for this Windows user. Back up and securely escrow the key before publishing.'
    } finally { Remove-Item Env:\RHC_RELEASE_STORE_PASSWORD -ErrorAction SilentlyContinue }
} finally { Pop-Location }
