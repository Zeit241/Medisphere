# Frontend — веб-админка

React SPA для администратора, регистратуры и врача.

**Часть монорепозитория:** см. [корневой README](../../README.md)

## Стек

- React 19, TypeScript, Vite
- Redux Toolkit, shadcn/ui, Tailwind CSS 4

## Запуск

```bash
cp .env.example .env
npm install
npm run dev
```

Приложение: http://localhost:5173

## Переменные окружения

| Переменная | Описание |
|------------|----------|
| `VITE_DIRECTUS_URL` | Proxy-путь к Directus (`/__directus`) |
| `VITE_DIRECTUS_PUBLIC_URL` | Публичный URL Directus для превью |
| `VITE_DIRECTUS_STATIC_TOKEN` | Static token для загрузки/чтения файлов |

## Сборка

```bash
npm run build
npm run preview
```

## Тесты

```bash
npm run test
npm run verify
```

## Исторический репозиторий

https://github.com/Zeit241/kursovaya_4_kurs_frontend
