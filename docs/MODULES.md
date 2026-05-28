# Описание модулей

Четыре основных модуля подключены как **git submodules** — код живёт в отдельных репозиториях, meta-репозиторий [Medisphere](https://github.com/Zeit241/Medisphere) хранит ссылки на конкретные коммиты.

| Submodule | Репозиторий |
|-----------|-------------|
| `modules/backend` | https://github.com/Zeit241/kursovaya_4_kurs_backend |
| `modules/frontend` | https://github.com/Zeit241/kursovaya_4_kurs_frontend |
| `modules/landing` | https://github.com/Zeit241/Kursovaya_3kurs_web |
| `modules/mobile-android` | https://github.com/Zeit241/kursovaya_4_kurs_mobile |

`db-seeder`, `qwen-proxy` и `infra` — часть meta-репозитория.

## modules/backend — REST API

**Стек:** Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security (JWT), Redis, WebSocket, PostgreSQL.

**Назначение:** центральный сервер бизнес-логики клиники.

**Основные возможности:**
- Аутентификация и авторизация (JWT, роли: patient, doctor, admin)
- CRUD: врачи, пациенты, специализации, услуги, диагнозы
- Запись на приём, управление расписанием
- Живая очередь приёмов (Redis + WebSocket)
- Отзывы и рейтинги
- Сводные отчёты (dashboard)
- Email-уведомления
- Интеграция с Directus (фото врачей через `/assets`)

**Порт:** 8085

**Документация:**
- [`API_ENDPOINTS.md`](../modules/backend/API_ENDPOINTS.md)
- [`WEBSOCKET_API.md`](../modules/backend/WEBSOCKET_API.md)

**Запуск:**
```bash
cd modules/backend
./mvnw spring-boot:run
```

---

## modules/frontend — Веб-админка

**Стек:** React 19, TypeScript, Vite, Redux Toolkit, TanStack Query, shadcn/ui, Tailwind CSS 4.

**Назначение:** интерфейс для администратора, регистратуры и врача.

**Разделы:**
- Админ: врачи, пациенты, услуги, специализации, отчёты
- Врач: расписание, очередь, приёмы, диагнозы
- Регистратура: запись пациентов, управление приёмами

**Порт (dev):** 5173

**Переменные окружения:** см. `.env.example`

**Запуск:**
```bash
cd modules/frontend
npm install && npm run dev
```

---

## modules/landing — Публичный сайт

**Стек:** Next.js 16 (App Router), TypeScript, Tailwind CSS, Directus SDK.

**Назначение:** маркетинговый сайт клиники с CMS-контентом из Directus.

**Страницы:**
- Главная (hero, услуги, команда, отзывы)
- Услуги, врачи, о клинике
- Политика конфиденциальности, условия использования

**Порт:** 3000

**CMS:** контент управляется через Directus (`cms_site_settings`, views для врачей/услуг). Настройка: [`directus/SETUP.txt`](../modules/landing/directus/SETUP.txt)

**Запуск:**
```bash
cd modules/landing
npm install && npm run dev
```

**Docker:** включён в корневой `docker-compose.yml` как сервис `landing`.

---

## modules/mobile-android — Android-приложение

**Стек:** Kotlin, Android SDK, Retrofit, Glide, Material Design.

**Назначение:** мобильный клиент для пациентов.

**Функции:**
- Авторизация и регистрация
- Просмотр врачей и услуг
- Запись на приём
- История приёмов и отзывы
- ИИ-помощник (интеграция с Qwen Proxy)

**Сборка:** Android Studio или `./gradlew assembleDebug`

**Конфигурация:** `local.properties` (см. `local.properties.example`)

**Документация API:** [`API.MD`](../modules/mobile-android/API.MD)

**Исторический репозиторий:** https://github.com/Zeit241/kursovaya_4_kurs_mobile

---

## modules/db-seeder — Наполнение БД

**Стек:** Node.js, Faker.js, pg, Inquirer.

**Назначение:** интерактивная генерация тестовых данных для PostgreSQL.

**Режимы:**
- Последовательное наполнение таблиц (users, doctors, appointments и др.)
- Массовое создание приёмов врача на дату
- Проверка подключения без изменений (`npm run verify`)

**Запуск:**
```bash
cd modules/db-seeder
npm install && npm start
```

---

## modules/qwen-proxy — ИИ-прокси

**Стек:** Node.js, Express, Puppeteer, OpenAI-compatible API.

**Назначение:** прокси-сервер для доступа к Qwen AI (чат-помощник в мобильном приложении).

**Порт:** 3264

**Запуск:**
```bash
cd modules/qwen-proxy
npm install && npm start
```

**Docker:** сервис `qwen-proxy` в `docker-compose.yml`.

Подробнее: [`README.md`](../modules/qwen-proxy/README.md)

---

## infra — Инфраструктура БД

**Содержимое:**
- `initdb/` — скрипты первичной инициализации PostgreSQL (схема, тестовые данные, Directus DB)
- `migrations/` — SQL-миграции для CMS overlay, фото врачей, настроек сайта

**Использование:** монтируется в Docker Postgres как `/docker-entrypoint-initdb.d`.

---

## Связи между модулями

```
Landing ──reads──> Directus (CMS контент, фото)
Frontend ──reads──> Directus (фото, файлы)
Frontend ──API──> Backend (JWT)
Mobile ──API──> Backend (JWT)
Mobile ──API──> Qwen Proxy (ИИ)
Backend ──reads/writes──> PostgreSQL
Backend ──cache/queue──> Redis
Directus ──reads/writes──> PostgreSQL
DB Seeder ──writes──> PostgreSQL
```

## Порты и зависимости

| Модуль | Зависит от | Порт |
|--------|------------|------|
| backend | PostgreSQL, Redis | 8085 |
| frontend | backend, Directus | 5173 |
| landing | Directus, PostgreSQL | 3000 |
| mobile-android | backend, Directus, qwen-proxy | — |
| db-seeder | PostgreSQL | — |
| qwen-proxy | — | 3264 |
| directus (docker) | PostgreSQL, Redis | 8055 |
