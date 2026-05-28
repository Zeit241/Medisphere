# Landing — публичный сайт клиники

Next.js сайт с контентом из Directus CMS.

**Часть монорепозитория:** см. [корневой README](../../README.md)

## Стек

- Next.js 16 (App Router), TypeScript, Tailwind CSS
- Directus SDK

## Запуск

```bash
cp .env.example .env.local
npm install
npm run dev
```

Сайт: http://localhost:3000

## Docker

Сервис `landing` в корневом `docker-compose.yml`:

```bash
docker compose up -d landing
```

## Directus

Настройка CMS: [`directus/SETUP.txt`](directus/SETUP.txt)

## Тесты

```bash
npm run test
npm run verify
```

## Исторический репозиторий

https://github.com/Zeit241/Kursovaya_3kurs_web
