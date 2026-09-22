param(
    # Kunci service_role Supabase. TIDAK disimpan di repo; berikan lewat parameter
    # atau env var SUPABASE_SERVICE_ROLE_KEY.
    [string]$ServiceRoleKey = $env:SUPABASE_SERVICE_ROLE_KEY,
    [Parameter(Mandatory = $true)][int]$NewVersionCode,
    [Parameter(Mandatory = $true)][string]$NewVersionName,
    # Versi basis yang dibuatkan patch, mis. 21,20,19. Patch-nya harus sudah ada
    # di root repo sebagai superapp-patch-B-to-N.fbf (dibuat create-archive-patch.ps1).
    [Parameter(Mandatory = $true)][int[]]$BaseVersionCodes,
    [string]$Notes = "",
    [switch]$Mandatory,
    # Hanya cetak manifest tanpa mengunggah apa pun.
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
# Cadangan: baca dari .env di root repo (file ini di-gitignore).
if ([string]::IsNullOrWhiteSpace($ServiceRoleKey)) {
    $envFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.env'
    if (Test-Path -LiteralPath $envFile) {
        $line = Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^\s*SUPABASE_SERVICE_ROLE_KEY\s*=' } | Select-Object -First 1
        if ($line) { $ServiceRoleKey = ($line -split '=', 2)[1].Trim().Trim('"') }
    }
}
if ([string]::IsNullOrWhiteSpace($ServiceRoleKey) -and -not $DryRun) {
    throw 'ServiceRoleKey kosong. Isi -ServiceRoleKey atau env SUPABASE_SERVICE_ROLE_KEY.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$baseUrl     = "https://khpkoreaaucvyqfhynfq.supabase.co"
$bucketPath  = "app-releases/superapp"

$apkPath = Join-Path $projectRoot "superapp-$NewVersionCode.apk"
if (-not (Test-Path -LiteralPath $apkPath)) { throw "APK tidak ditemukan: $apkPath" }

$headers = @{
    "apikey"        = $ServiceRoleKey
    "Authorization" = "Bearer $ServiceRoleKey"
    "Content-Type"  = "application/json"
}

function Upload-File([string]$Path, [string]$ContentType) {
    $name = Split-Path $Path -Leaf
    $url  = "$baseUrl/storage/v1/object/$bucketPath/$name"
    $hdr  = $headers.Clone()
    $hdr["Content-Type"] = $ContentType
    $hdr["x-upsert"]     = "true"
    Write-Host "Unggah $name ($([math]::Round((Get-Item -LiteralPath $Path).Length / 1MB, 1)) MB)..."
    Invoke-RestMethod -Uri $url -Method Post -Headers $hdr -Body ([System.IO.File]::ReadAllBytes($Path)) -UseBasicParsing | Out-Null
    return "$baseUrl/storage/v1/object/public/$bucketPath/$name"
}

function Assert-Public([string]$Url, [string]$ExpectedSha) {
    $tmp = [System.IO.Path]::GetTempFileName()
    Invoke-WebRequest -Uri $Url -OutFile $tmp -UseBasicParsing
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $tmp).Hash.ToLower()
    Remove-Item -LiteralPath $tmp -Force
    if ($actual -ne $ExpectedSha) { throw "SHA-256 di storage tidak cocok untuk $Url" }
    Write-Host "  OK publik + SHA cocok: $Url"
}

# --- Kumpulkan patch ---
$deltas = @()
foreach ($base in $BaseVersionCodes) {
    $patchPath = Join-Path $projectRoot "superapp-patch-$base-to-$NewVersionCode.fbf"
    if (-not (Test-Path -LiteralPath $patchPath)) { throw "Patch tidak ditemukan: $patchPath" }
    if ((Get-Item -LiteralPath $patchPath).Length -eq 0) { throw "Patch kosong: $patchPath" }
    $deltas += @{
        path              = $patchPath
        base_version_code = $base
        patch_sha256      = (Get-FileHash -Algorithm SHA256 -LiteralPath $patchPath).Hash.ToLower()
        patch_size_bytes  = (Get-Item -LiteralPath $patchPath).Length
    }
}

$apkSha  = (Get-FileHash -Algorithm SHA256 -LiteralPath $apkPath).Hash.ToLower()
$apkSize = (Get-Item -LiteralPath $apkPath).Length

# --- Unggah ---
$apkUrl = "$baseUrl/storage/v1/object/public/$bucketPath/superapp-$NewVersionCode.apk"
if (-not $DryRun) {
    $apkUrl = Upload-File $apkPath "application/vnd.android.package-archive"
    Assert-Public $apkUrl $apkSha
}
$deltaJson = @()
foreach ($d in $deltas) {
    $url = "$baseUrl/storage/v1/object/public/$bucketPath/$(Split-Path $d.path -Leaf)"
    if (-not $DryRun) {
        $url = Upload-File $d.path "application/octet-stream"
        Assert-Public $url $d.patch_sha256
    }
    $deltaJson += [ordered]@{
        base_version_code = $d.base_version_code
        patch_url         = $url
        patch_sha256      = $d.patch_sha256
        patch_size_bytes  = $d.patch_size_bytes
    }
}

# --- Manifest global_settings ---
$manifest = [ordered]@{
    version_code   = $NewVersionCode
    version_name   = $NewVersionName
    apk_url        = $apkUrl
    apk_sha256     = $apkSha
    apk_size_bytes = $apkSize
    delta          = $deltaJson[0]
    deltas         = $deltaJson
    notes          = $Notes
    mandatory      = [bool]$Mandatory
}
$body = @{ value = $manifest; updated_at = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ") } | ConvertTo-Json -Depth 6 -Compress
Write-Host "Manifest:"; Write-Host ($manifest | ConvertTo-Json -Depth 6)

if ($DryRun) { Write-Host "DryRun: tidak ada yang diunggah/diubah."; exit 0 }

$hdr = $headers.Clone(); $hdr["Prefer"] = "return=representation"
$res = Invoke-RestMethod -Uri "$baseUrl/rest/v1/global_settings?key=eq.superapp_update" -Method Patch -Headers $hdr -Body $body -UseBasicParsing
if (-not $res -or $res.Count -eq 0) { throw "PATCH global_settings tidak mengubah baris apa pun (key superapp_update tidak ada?)" }
Write-Host "global_settings.superapp_update -> versi $NewVersionName ($NewVersionCode). Selesai."
