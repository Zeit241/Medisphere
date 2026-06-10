# Сводка качества — kursovaya/modules

Обновлено: 2026-05-31

Команда полной проверки:

```powershell
.\scripts\verify-all.ps1
```

## Backend — `modules/backend`

| Метрика | Значение |
|---------|----------|
| Команда | `mvnw verify` |
| Checkstyle | 0 блокирующих нарушений |
| Unit-тесты (Surefire) | **207** pass |
| Integration-тесты (Failsafe) | **38** pass |
| JaCoCo (gated service layer) | ≥80% (12 классов в gate) |
| JaCoCo merged (unit+IT) | см. `modules/backend/target/site/jacoco/index.html` |

Инфраструктура: Checkstyle, JaCoCo, Surefire/Failsafe, embedded PostgreSQL/Redis + Testcontainers (при Docker).

## Frontend — `modules/frontend`

| Метрика | Значение |
|---------|----------|
| Команда | `npm run verify` |
| ESLint + tsc | pass |
| Unit (Vitest) | **34** pass |
| Integration (MSW + RTK Query) | **29** pass |
| Coverage lib + store/api | **84.4%** lines |

## Landing — `modules/landing`

| Метрика | Значение |
|---------|----------|
| Команда | `npm run verify` |
| next lint + build | pass |
| Unit (Vitest CMS) | **25** pass |
| Integration (Directus + SSR) | **16** pass |
| Coverage lib/cms | **98.5%** lines |

## Android — `modules/mobile-android`

| Метрика | Значение |
|---------|----------|
| Команда | `gradlew ktlintCheck test koverVerify` |
| ktlintCheck | 0 нарушений |
| Unit (repos/mappers) | **23** focused |
| Integration (MockWebServer + STOMP) | **24** |
| Kover (repository + mappers) | ≥80% |

## Итого

| Модуль | Тестов | Статус |
|--------|--------|--------|
| Backend | 245 | ✅ |
| Frontend | 63 | ✅ |
| Landing | 41 | ✅ |
| Android | 86+ | ✅ |
