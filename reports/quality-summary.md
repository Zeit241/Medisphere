# Quality Summary

Generated: 2026-05-28 19:50:00

Overall: **PASS**

| Module | Verify | Unit tests | Integration / IT | Line coverage | Notes |
|--------|--------|------------|------------------|---------------|-------|
| backend | PASS | 171 | 38 IT | 91.75% | JaCoCo gate on `pin122.kursovaya.service` (>= 80%, measured classes only) |
| frontend | PASS | 63 | Vitest unit + MSW integration | 99.41% | `lib/**` + `store/api/**` (>= 80%) |
| landing | PASS | 41 | Vitest SSR integration | 86.73% | `lib/cms/**` (>= 75%) |
| mobile-android | PASS | 96 | MockWebServer REST integration | 91.59% | ktlint + Kover on extensions/cache/util (>= 70%) |

## Commands

```powershell
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
```

## Exit codes

| Module | Exit code |
|--------|-----------|
| backend | 0 |
| frontend | 0 |
| landing | 0 |
| mobile-android | 0 |
