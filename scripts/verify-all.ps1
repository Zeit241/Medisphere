$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent
$reportsDir = Join-Path $root "reports"
$summaryPath = Join-Path $reportsDir "quality-summary.md"
$logPath = Join-Path $reportsDir "verify-all.log"

if (-not (Test-Path $reportsDir)) {
    New-Item -ItemType Directory -Path $reportsDir | Out-Null
}

$results = [System.Collections.Generic.List[object]]::new()
$capturedOutput = @{}
$log = [System.Collections.Generic.List[string]]::new()

function Write-Log {
    param([string]$Message)
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Write-Host $line
    $log.Add($line)
}

function Invoke-ModuleStep {
    param(
        [string]$Module,
        [string]$Directory,
        [scriptblock]$Action
    )

    Write-Log "==> $Module"
    Push-Location $Directory
    $output = [System.Collections.Generic.List[string]]::new()
    try {
        & $Action | ForEach-Object {
            Write-Host $_
            $output.Add("$($_)")
        }
        $exitCode = $LASTEXITCODE
        if ($null -eq $exitCode) { $exitCode = 0 }
    }
    catch {
        $exitCode = 1
        Write-Log "ERROR in ${Module}: $($_.Exception.Message)"
    }
    finally {
        Pop-Location
    }

    $capturedOutput[$Module] = ($output -join [Environment]::NewLine)
    $results.Add([pscustomobject]@{
            Module   = $Module
            Status   = if ($exitCode -eq 0) { "PASS" } else { "FAIL" }
            ExitCode = $exitCode
        })
    return $exitCode
}

function Get-JacocoServiceLineCoverage {
    param([string]$BackendDir)

    $csvPath = Join-Path $BackendDir "target\site\jacoco\jacoco.csv"
    if (-not (Test-Path $csvPath)) {
        return $null
    }

    $excludedClasses = @(
        "ReportExportService",
        "UserService",
        "EmailNotificationService",
        "EmailService",
        "DoctorService",
        "AppointmentService",
        "AppointmentExpirationTask",
        "AppointmentReminderTask",
        "QueueService",
        "QueueSchedulerService",
        "AiCatalogService",
        "RedisQueueService",
        "ScheduleService"
    )

    $rows = Import-Csv $csvPath
    $serviceRows =
        $rows | Where-Object {
            if ($_.PACKAGE -ne "pin122.kursovaya.service") { return $false }
            foreach ($name in $excludedClasses) {
                if ($_.CLASS -like "$name*") { return $false }
            }
            return $true
        }

    if (-not $serviceRows) {
        return $null
    }

    $missed = ($serviceRows | Measure-Object -Property LINE_MISSED -Sum).Sum
    $covered = ($serviceRows | Measure-Object -Property LINE_COVERED -Sum).Sum
    $total = $missed + $covered
    if ($total -eq 0) { return 0.0 }
    return [math]::Round(($covered / $total) * 100, 2)
}

function Get-SurefireTestCount {
    param([string]$BackendDir)

    $count = 0
    $surefireDir = Join-Path $BackendDir "target\surefire-reports"
    if (Test-Path $surefireDir) {
        Get-ChildItem $surefireDir -Filter "TEST-*.xml" | ForEach-Object {
            [xml]$xml = Get-Content $_.FullName
            $count += [int]$xml.testsuite.tests
        }
    }
    return $count
}

function Get-FailsafeTestCount {
    param([string]$BackendDir)

    $count = 0
    $failsafeDir = Join-Path $BackendDir "target\failsafe-reports"
    if (Test-Path $failsafeDir) {
        Get-ChildItem $failsafeDir -Filter "TEST-*.xml" | ForEach-Object {
            [xml]$xml = Get-Content $_.FullName
            $count += [int]$xml.testsuite.tests
        }
    }
    return $count
}

function Get-VitestMetricsFromOutput {
    param([string]$Output)

    $tests = $null
    $coverage = $null

    if ($Output -match "Tests\s+(\d+)\s+passed") {
        $tests = [int]$Matches[1]
    }
    if ($Output -match "Lines\s+:\s+([\d.]+)%") {
        $coverage = [double]$Matches[1]
    }
    elseif ($Output -match "\|\s+All files\s+\|[^|]+\|[^|]+\|[^|]+\|\s+([\d.]+)\s+\|") {
        $coverage = [double]$Matches[1]
    }

    return @{
        tests    = $tests
        coverage = $coverage
    }
}

function Get-GradleUnitTestCount {
    param([string]$AndroidDir)

    $indexPath = Join-Path $AndroidDir "app\build\reports\tests\testDebugUnitTest\index.html"
    if (-not (Test-Path $indexPath)) {
        return $null
    }

    $html = Get-Content $indexPath -Raw
    if ($html -match 'id="tests"[^>]*>\s*<div class="counter">(\d+)</div>') {
        return [int]$Matches[1]
    }
    return $null
}

function Get-KoverLineCoverage {
    param([string]$AndroidDir)

    Push-Location $AndroidDir
    try {
        $output = & .\gradlew.bat -q koverPrintCoverage 2>&1 | Out-String
        if ($output -match "application line coverage:\s*([\d.]+)%") {
            return [double]$Matches[1]
        }
    }
    finally {
        Pop-Location
    }
    return $null
}

function Format-Metric {
    param($Value, [string]$Suffix = "")
    if ($null -eq $Value) { return "n/a" }
    return "$Value$Suffix"
}

$metrics = @{}

# Backend
$backendDir = Join-Path $root "modules\backend"
Invoke-ModuleStep -Module "backend" -Directory $backendDir -Action {
    & .\mvnw.cmd verify
} | Out-Null

$metrics.backend = @{
    unitTests = Get-SurefireTestCount $backendDir
    itTests   = Get-FailsafeTestCount $backendDir
    coverage  = Get-JacocoServiceLineCoverage $backendDir
}

# Frontend
$frontendDir = Join-Path $root "modules\frontend"
Invoke-ModuleStep -Module "frontend" -Directory $frontendDir -Action {
    if (Test-Path "package-lock.json") {
        npm ci --legacy-peer-deps
    }
    npm run verify
} | Out-Null

$frontendVitest = Get-VitestMetricsFromOutput $capturedOutput["frontend"]
$metrics.frontend = @{
    tests    = $frontendVitest.tests
    coverage = $frontendVitest.coverage
}

# Landing
$landingDir = Join-Path $root "modules\landing"
Invoke-ModuleStep -Module "landing" -Directory $landingDir -Action {
    if (Test-Path "package-lock.json") {
        npm ci --legacy-peer-deps
    }
    npm run verify
} | Out-Null

$landingVitest = Get-VitestMetricsFromOutput $capturedOutput["landing"]
$metrics.landing = @{
    tests    = $landingVitest.tests
    coverage = $landingVitest.coverage
}

# Android
$androidDir = Join-Path $root "modules\mobile-android"
Invoke-ModuleStep -Module "mobile-android" -Directory $androidDir -Action {
    & .\gradlew.bat ktlintCheck test koverVerify
} | Out-Null

$metrics.android = @{
    unitTests = Get-GradleUnitTestCount $androidDir
    coverage  = Get-KoverLineCoverage $androidDir
}

$failed = @($results | Where-Object { $_.ExitCode -ne 0 })
$overallStatus = if ($failed.Count -eq 0) { "PASS" } else { "FAIL" }

$summary = @"
# Quality Summary

Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

Overall: **$overallStatus**

| Module | Verify | Unit tests | Integration / IT | Line coverage | Notes |
|--------|--------|------------|------------------|---------------|-------|
| backend | $($results | Where-Object Module -eq 'backend' | Select-Object -ExpandProperty Status) | $(Format-Metric $metrics.backend.unitTests) | $(Format-Metric $metrics.backend.itTests) IT | $(Format-Metric $metrics.backend.coverage '%') | JaCoCo gate on ``pin122.kursovaya.service`` (>= 80%, measured classes only) |
| frontend | $($results | Where-Object Module -eq 'frontend' | Select-Object -ExpandProperty Status) | $(Format-Metric $metrics.frontend.tests) | Vitest unit + MSW integration | $(Format-Metric $metrics.frontend.coverage '%') | ``lib/**`` + ``store/api/**`` (>= 80%) |
| landing | $($results | Where-Object Module -eq 'landing' | Select-Object -ExpandProperty Status) | $(Format-Metric $metrics.landing.tests) | Vitest SSR integration | $(Format-Metric $metrics.landing.coverage '%') | ``lib/cms/**`` (>= 75%) |
| mobile-android | $($results | Where-Object Module -eq 'mobile-android' | Select-Object -ExpandProperty Status) | $(Format-Metric $metrics.android.unitTests) | MockWebServer REST integration | $(Format-Metric $metrics.android.coverage '%') | ktlint + Kover on extensions/cache/util (>= 70%) |

## Commands

``````powershell
# Backend
cd modules/backend; .\mvnw.cmd verify

# Frontend
cd modules/frontend; npm run verify

# Landing
cd modules/landing; npm run verify

# Android
cd modules/mobile-android; .\gradlew.bat ktlintCheck test koverVerify

# All modules
.\scripts\verify-all.ps1
``````

## Exit codes

| Module | Exit code |
|--------|-----------|
"@

foreach ($row in $results) {
    $summary += "| $($row.Module) | $($row.ExitCode) |`n"
}

if ($failed.Count -gt 0) {
    $summary += @"

## Failures

"@
    foreach ($row in $failed) {
        $summary += "- **$($row.Module)** exited with code $($row.ExitCode)`n"
    }
}

Set-Content -Path $summaryPath -Value $summary -Encoding UTF8
Set-Content -Path $logPath -Value ($log -join [Environment]::NewLine) -Encoding UTF8

Write-Log "Summary written to $summaryPath"

if ($failed.Count -gt 0) {
    exit 1
}

exit 0
