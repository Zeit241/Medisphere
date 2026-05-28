# Модули

## Git submodules (отдельные репозитории)

| Путь | Репозиторий | Ветка |
|------|-------------|-------|
| [backend](backend) | https://github.com/Zeit241/kursovaya_4_kurs_backend | `master` |
| [frontend](frontend) | https://github.com/Zeit241/kursovaya_4_kurs_frontend | `main` |
| [landing](landing) | https://github.com/Zeit241/Kursovaya_3kurs_web | `main` |
| [mobile-android](mobile-android) | https://github.com/Zeit241/kursovaya_4_kurs_mobile | `master` |

Инициализация после клонирования:

```bash
git submodule update --init --recursive
```

## В этом репозитории

| Модуль | Описание |
|--------|----------|
| [db-seeder](db-seeder) | Наполнение PostgreSQL тестовыми данными |
| [qwen-proxy](qwen-proxy) | Прокси для ИИ-помощника Qwen |

Подробное описание: [docs/MODULES.md](../docs/MODULES.md)
