# Деплой в Dokploy

Руководство по развёртыванию всего стека клиники через [Dokploy](https://dokploy.com/) (Docker Compose).

## Что поднимается

| Сервис | Порт в контейнере | Назначение |
|--------|-------------------|------------|
| `db` | 5432 | PostgreSQL |
| `redis` | 6379 | Redis (очереди, кэш Directus) |
| `directus` | 8055 | CMS |
| `backend` | 8085 | Spring Boot API + WebSocket |
| `landing` | 3000 | Публичный сайт (Next.js) |
| `frontend` | 80 | Админка (React + nginx) |
| `qwen-proxy` | 3264 | AI-прокси (опционально) |

Файл стека: **`docker-compose.dokploy.yml`**  
Шаблон переменных: **`.env.dokploy.example`**

## 1. Подготовка сервера

1. Установите Dokploy на VPS (Ubuntu 22.04+, 4 GB RAM, 40 GB SSD).
2. Создайте DNS **A-записи** на IP сервера (пример):

| Домен | Сервис в Dokploy |
|-------|------------------|
| `clinic.example.com` | `landing` |
| `admin.example.com` | `frontend` |
| `api.example.com` | `backend` |
| `cms.example.com` | `directus` |
| `qwen.example.com` | `qwen-proxy` (если нужен) |

## 2. Исходники сервисов (submodules)

Docker-сборка для Dokploy **не зависит от git submodules**: исходники `backend`, `frontend`, `landing` и `qwen-proxy` **клонируются из GitHub прямо в Dockerfile** (`infra/docker/*.Dockerfile`).

Submodules нужны только для локальной разработки. На сервере достаточно клонировать meta-репозиторий `Medisphere`.

При необходимости зафиксируйте ветку/коммит через переменные:
- `BACKEND_GIT_REF` (по умолчанию `master`)
- `FRONTEND_GIT_REF` (по умолчанию `main`)
- `LANDING_GIT_REF` (по умолчанию `main`)
- `QWEN_GIT_REF` (по умолчанию `main`)

## 3. Создание Compose-приложения

1. Dokploy → **Project** → **Create Service** → **Compose**.
2. **Source**: GitHub / GitLab / Custom Git — ваш репозиторий, ветка `main`.
3. **Compose Path**: `docker-compose.dokploy.yml`
4. **Compose Type**: Docker Compose (не Stack — Stack не поддерживает `build`).

## 4. Переменные окружения

Откройте вкладку **Environment** и задайте переменные из `.env.dokploy.example`.

Обязательно замените все `change-me` и `example.com` на реальные значения.

| Переменная | Где взять |
|------------|-----------|
| `DIRECTUS_TOKEN` | Directus → Settings → Access Tokens → Static Token |
| `JWT_SECRET` | Случайная строка ≥ 256 бит |
| `DIRECTUS_SECRET` | Случайная строка для Directus |
| `VITE_API_URL` | `https://api.<ваш-домен>` |
| `NEXTAUTH_URL` | `https://clinic.<ваш-домен>` |

> Переменные `NEXT_PUBLIC_*` и `VITE_*` вшиваются **на этапе сборки**. После смены доменов нажмите **Redeploy** (пересборка образов).

## 5. Домены (вкладка Domains)

Рекомендуемый способ — **Dokploy Domains** (без ручных Traefik labels):

| Сервис | Container port | HTTPS |
|--------|----------------|-------|
| `landing` | 3000 | ✅ Let's Encrypt |
| `frontend` | 80 | ✅ |
| `backend` | 8085 | ✅ |
| `directus` | 8055 | ✅ |
| `qwen-proxy` | 3264 | ✅ (ограничьте доступ) |

Для **backend** включите поддержку **WebSocket** (если есть опция в Domains) — нужен для `/queue-websocket`.

После добавления доменов нажмите **Deploy** и подождите ~1–2 минуты на выпуск сертификатов.

## 6. Первый деплой

1. **Deploy** — дождитесь сборки всех образов (backend Maven ~5–10 мин).
2. Откройте Directus по домену CMS, войдите с `DIRECTUS_ADMIN_EMAIL` / `DIRECTUS_ADMIN_PASSWORD`.
3. Создайте Static Token → пропишите в `DIRECTUS_TOKEN` → **Redeploy**.
4. При необходимости наполните БД:
   ```bash
   docker exec -i <postgres-container> psql -U clinic_user -d clinic_db < infra/migrations/seed_demo.sql
   ```

## 7. Проверка

```bash
# Health backend
curl https://api.example.com/actuator/health

# Directus
curl -I https://cms.example.com/server/health

# Landing / Admin — откройте в браузере
```

## 8. Обновление

1. Push в Git → Auto Deploy (webhook) или **Deploy** вручную.
2. Dokploy пересобирает образы с `build` и перезапускает сервисы.
3. Данные Postgres/Redis/Directus сохраняются в **named volumes** (`pg_data`, `redis_data`, …).

## 9. qwen-proxy

- Сессия браузера хранится в volume `qwen_session`.
- При первом запуске может потребоваться авторизация Qwen (см. `modules/qwen-proxy/README.md`).
- В production ограничьте доступ к домену qwen (firewall / basic auth в Dokploy).

## 10. Частые проблемы

**Сборка падает: `modules/backend` not found**  
→ Submodules не инициализированы. См. раздел 2.

**403 на фото врачей**  
→ Проверьте `DIRECTUS_TOKEN` и `VITE_DIRECTUS_PUBLIC_URL`.

**CORS в Directus**  
→ Обновите `CORS_ORIGIN` — домены admin и landing через запятую.

**Backend не видит Redis**  
→ В prod используется хост `redis:6379` (внутри docker-сети), не `6380`.

**Пустая БД после деплоя**  
→ Init-скрипты из `infra/initdb/` выполняются только при **первом** создании volume `pg_data`. Для демо-данных — `seed_demo.sql`.

## Локальная проверка compose-файла

```bash
cp .env.dokploy.example .env
# отредактируйте .env
docker network create dokploy-network 2>/dev/null || true
docker compose -f docker-compose.dokploy.yml --env-file .env up -d --build
```
