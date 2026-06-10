$ErrorActionPreference = "Stop"

$Root = Split-Path $PSScriptRoot -Parent
$ProxyDir = Join-Path $Root "modules\qwen-proxy"
$PatchDir = Join-Path $Root "infra\qwen-proxy\patches"

if (-not (Test-Path $ProxyDir)) {
    throw "Submodule not found: $ProxyDir. Run: git submodule update --init modules/qwen-proxy"
}

Push-Location $ProxyDir
try {
    $patches = Get-ChildItem -Path $PatchDir -Filter "*.patch" | Sort-Object Name
    if ($patches.Count -eq 0) {
        Write-Host "No qwen-proxy patches found in $PatchDir"
        exit 0
    }

    foreach ($patch in $patches) {
        Write-Host "Applying $($patch.Name)..."
        git apply --check $patch.FullName 2>$null
        if ($LASTEXITCODE -ne 0) {
            git apply --reverse --check $patch.FullName 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  skip: already applied"
                continue
            }
            git apply --3way $patch.FullName
        } else {
            git apply $patch.FullName
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to apply $($patch.Name)"
        }
    }

    Write-Host "qwen-proxy patches applied."
} finally {
    Pop-Location
}
