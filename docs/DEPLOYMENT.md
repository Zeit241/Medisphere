# Развёртывание на сервере

Руководство по production-развёртыванию информационной системы медицинской клиники на Linux-сервере (Ubuntu 22.04 / Debian 12).

## Обзор

На сервере поднимаются:

1. **PostgreSQL + Redis + Directus** — через Docker Compose
2. **Backend (Spring Boot)** — JAR + systemd
3. **Frontend (React)** — статика за Nginx или `vite preview`
4. **Landing (Next.js)** — Docker или `next start` за Nginx
5. **Qwen Proxy** (опционально) — Docker
6. **Nginx** — reverse proxy и TLS

Рекомендуемые домены:

| Сервис | Пример URL |
|--------|------------|
| Landing | `https://clinic.example.com` |
| Admin (frontend) | `https://admin.clinic.example.com` |
| API | `https://api.clinic.example.com` |
| Directus | `https://cms.clinic.example.com` |

## Требования к серверу

- 4+ GB RAM, 2+ CPU
- 40+ GB SSD
- Ubuntu 22.04 LTS или Debian 12
- Домен с DNS A-записями
- Открытые порты: 80, 443 (остальное — только localhost)

## 1. Подготовка сервера

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl nginx certbot python3-certbot-nginx

# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Java 21
sudo apt install -y openjdk-21-jdk

# Node.js 20 LTS
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

Клонируйте монорепозиторий:

```bash
git clone https://github.com/Zeit241/kursovaya.git /opt/kursovaya
cd /opt/kursovaya
```

## 2. Переменные окружения

Создайте файл `/opt/kursovaya/.env.production` (не коммитьте в git):

```bash
# PostgreSQL
POSTGRES_USER=clinic_user
POSTGRES_PASSWORD=<strong_password>
POSTGRES_DB=clinic_db

# Directus
DIRECTUS_SECRET=<random_64_chars>
DIRECTUS_ADMIN_EMAIL=admin@yourdomain.com
DIRECTUS_ADMIN_PASSWORD=<strong_password>
DIRECTUS_PUBLIC_URL=https://cms.clinic.example.com
DIRECTUS_TOKEN=<static_token_from_directus>

# JWT (backend)
JWT_SECRET=<random_256_bit_secret>

# URLs
API_PUBLIC_URL=https://api.clinic.example.com
FRONTEND_URL=https://admin.clinic.example.com
LANDING_URL=https://clinic.example.com

# SMTP (опционально)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
```

## 3. Docker Compose (инфраструктура)

Отредактируйте `docker-compose.yml` для production:

- Замените пароли и секреты
- Уберите проброс портов Postgres/Redis наружу (оставьте только `127.0.0.1`)
- Обновите `CORS_ORIGIN` и `PUBLIC_URL` для Directus
- Задайте `DIRECTUS_TOKEN` для landing

Запуск:

```bash
cd /opt/kursovaya
docker compose up -d db redis directus landing qwen-proxy
```

Проверка:

```bash
docker compose ps
docker compose logs -f directus
```

### Первичная настройка Directus

1. Откройте `https://cms.clinic.example.com`
2. Войдите с admin-учётными данными
3. Создайте **Static Token** (Settings → Access Tokens)
4. Пропишите токен в `.env` landing и frontend
5. Примените миграции CMS из `infra/migrations/` (см. `modules/landing/directus/SETUP.txt`)

### Схема БД

При первом запуске Postgres выполнит скрипты из `infra/initdb/`. Дополнительные миграции — вручную через psql или pgAdmin:

```bash
docker exec -i clinic_postgres psql -U clinic_user -d clinic_db < infra/migrations/seed.sql
```

## 4. Backend (Spring Boot)

### Сборка JAR

```bash
cd /opt/kursovaya/modules/backend
./mvnw -DskipTests package
```

Артефакт: `target/kursovaya-0.0.1-SNAPSHOT.jar`

### Production-конфигурация

Создайте `application-prod.properties` или используйте переменные окружения:

```properties
server.port=8085
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/clinic_db
spring.datasource.username=clinic_user
spring.datasource.password=<password>
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6380
jwt.secret=<JWT_SECRET>
app.directus.public-url=https://cms.clinic.example.com
spring.mail.host=${MAIL_HOST}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

> **Важно:** не храните пароли в git. Используйте `application-prod.properties` на сервере или переменные окружения Spring (`SPRING_DATASOURCE_PASSWORD` и т.д.).

### systemd-сервис

```ini
# /etc/systemd/system/clinic-backend.service
[Unit]
Description=Clinic Backend API
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/kursovaya/modules/backend
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=TZ=Europe/Moscow
ExecStart=/usr/bin/java -jar /opt/kursovaya/modules/backend/target/kursovaya-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable clinic-backend
sudo systemctl start clinic-backend
sudo systemctl status clinic-backend
```

## 5. Frontend (React админка)

### Сборка

```bash
cd /opt/kursovaya/modules/frontend
cp .env.example .env
# VITE_API_URL=https://api.clinic.example.com
# VITE_DIRECTUS_PUBLIC_URL=https://cms.clinic.example.com
# VITE_DIRECTUS_STATIC_TOKEN=<token>
npm ci
npm run build
```

Статика: `dist/`

### Раздача через Nginx

```nginx
server {
    listen 443 ssl http2;
    server_name admin.clinic.example.com;

    ssl_certificate /etc/letsencrypt/live/admin.clinic.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.clinic.example.com/privkey.pem;

    root /opt/kursovaya/modules/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /__directus/ {
        proxy_pass https://cms.clinic.example.com/;
        proxy_set_header Host cms.clinic.example.com;
    }
}
```

## 6. Landing (Next.js)

Landing уже описан в `docker-compose.yml`. Для production обновите переменные:

```yaml
environment:
  NEXT_PUBLIC_DIRECTUS_URL: "https://cms.clinic.example.com"
  DIRECTUS_INTERNAL_URL: "http://directus:8055"
  DIRECTUS_TOKEN: "<static_token>"
  NEXT_PUBLIC_SITE_URL: "https://clinic.example.com"
```

Nginx перед контейнером:

```nginx
server {
    listen 443 ssl http2;
    server_name clinic.example.com;

    ssl_certificate /etc/letsencrypt/live/clinic.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/clinic.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 7. API (Nginx reverse proxy)

```nginx
server {
    listen 443 ssl http2;
    server_name api.clinic.example.com;

    ssl_certificate /etc/letsencrypt/live/api.clinic.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.clinic.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8085;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws {
        proxy_pass http://127.0.0.1:8085;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 8. TLS (Let's Encrypt)

```bash
sudo certbot --nginx -d clinic.example.com -d admin.clinic.example.com -d api.clinic.example.com -d cms.clinic.example.com
sudo certbot renew --dry-run
```

## 9. Мобильное приложение

В `local.properties` (сборка release) укажите production URL:

```properties
api.base.url=https://api.clinic.example.com
directus.static.token=<token>
qwen.proxy.url=https://qwen.clinic.example.com
```

Соберите signed APK/AAB в Android Studio и распространите через Play Store или прямую установку.

## 10. Qwen Proxy (опционально)

```bash
docker compose up -d qwen-proxy
```

Проксируйте через Nginx с ограничением доступа (только backend/mobile IP или auth).

## 11. Наполнение БД

После первого запуска Postgres:

```bash
cd /opt/kursovaya/modules/db-seeder
npm ci
cp .env.example .env
# PGHOST=127.0.0.1, PGPORT=5432, ...
npm run verify
npm start
```

## 12. Обновление

```bash
cd /opt/kursovaya
git pull

# Backend
cd modules/backend && ./mvnw -DskipTests package
sudo systemctl restart clinic-backend

# Frontend
cd ../frontend && npm ci && npm run build

# Landing + infra
cd /opt/kursovaya
docker compose build landing
docker compose up -d landing directus
```

## 13. Мониторинг и логи

```bash
# Backend
sudo journalctl -u clinic-backend -f

# Docker
docker compose logs -f

# Nginx
sudo tail -f /var/log/nginx/error.log
```

## 14. Резервное копирование

```bash
# PostgreSQL dump
docker exec clinic_postgres pg_dump -U clinic_user clinic_db > backup_$(date +%F).sql

# Directus uploads
docker run --rm -v kursovaya_directus_uploads:/data -v $(pwd):/backup alpine tar czf /backup/directus_uploads.tar.gz /data
```

Рекомендуется cron-задача на ежедневный бэкап.

## 15. Чеклист безопасности

- [ ] Сменить все пароли по умолчанию (Postgres, Directus, pgAdmin)
- [ ] Не пробрасывать Postgres/Redis на публичный интерфейс
- [ ] Использовать сильный `JWT_SECRET` (256+ бит)
- [ ] Включить HTTPS на всех доменах
- [ ] Удалить/закрыть pgAdmin в production
- [ ] Не коммитить `.env`, `local.properties`, пароли SMTP
- [ ] Настроить firewall (`ufw allow 80,443`)

## Порты (справочник)

| Сервис | Внутренний | Внешний (dev) |
|--------|------------|---------------|
| PostgreSQL | 5432 | 5432 |
| Redis | 6379 | 6380 |
| Directus | 8055 | 8055 |
| Backend | 8085 | 8085 |
| Frontend (dev) | 5173 | 5173 |
| Landing | 3000 | 3000 |
| Qwen Proxy | 3264 | 3264 |
| pgAdmin | 80 | 8080 |

## Устранение неполадок

**Backend не подключается к Redis** — проверьте порт: в Docker Redis на хосте доступен как `6380`, внутри docker-сети — `6379`.

**403 на фото врачей** — проверьте `VITE_DIRECTUS_STATIC_TOKEN` (frontend) и `directus.static.token` (mobile).

**CORS ошибки** — добавьте домен frontend в `CORS_ORIGIN` Directus и настройте CORS в Spring Security backend.

**Пустая очередь приёмов** — очередь в Redis строится только для будущих `start_time`; см. [`modules/db-seeder/README.md`](../modules/db-seeder/README.md).
