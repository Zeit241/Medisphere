# Полная проверка качества всех модулей в modules/
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ReportDir = Join-Path $Root "reports"
$SummaryFile = Join-Path $ReportDir "quality-summary.md"
$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

$results = @()

function Invoke-Step {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [scriptblock]$Action
    )
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    Push-Location $WorkingDirectory
    try {
        & $Action
        $results += [PSCustomObject]@{ Module = $Name; Status = "PASS" }
        Write-Host "OK: $Name" -ForegroundColor Green
    }
    catch {
        $results += [PSCustomObject]@{ Module = $Name; Status = "FAIL"; Error = $_.Exception.Message }
        Write-Host "FAIL: $Name — $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
    finally {
        Pop-Location
    }
}

try {
    Invoke-Step "Backend (mvn verify)" (Join-Path $Root "modules\backend") {
        .\mvnw.cmd verify -q
    }

    Invoke-Step "Frontend (npm verify)" (Join-Path $Root "modules\frontend") {
        npm run verify
    }

    Invoke-Step "Landing (npm verify)" (Join-Path $Root "modules\landing") {
        npm run verify
    }

    Invoke-Step "Android (ktlint + test + kover)" (Join-Path $Root "modules\mobile-android") {
        .\gradlew.bat ktlintCheck test koverVerify
    }
}
catch {
    Write-Host "`nVerify-all завершён с ошибками." -ForegroundColor Red
    exit 1
}

Write-Host "`nВсе модули прошли проверку." -ForegroundColor Green
Write-Host "Сводка: $SummaryFile"
