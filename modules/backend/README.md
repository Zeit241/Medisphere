# Backend API

Spring Boot REST API для информационной системы медицинской клиники.

**Часть монорепозитория:** см. [корневой README](../../README.md)

## Стек

- Java 21, Spring Boot 3.5
- PostgreSQL, Redis
- JWT, WebSocket

## Запуск

```bash
./mvnw spring-boot:run
```

API: http://localhost:8085

## Конфигурация

Файл: `src/main/resources/application.properties`

- PostgreSQL: `localhost:5432/clinic_db` (login/pass)
- Redis: `localhost:6379` (при Docker Compose с хоста — порт **6380**)
- JWT secret, SMTP — задайте для production через профиль `prod`

## Документация

- [API Endpoints](API_ENDPOINTS.md)
- [WebSocket API](WEBSOCKET_API.md)

## Тесты

```bash
./mvnw test
```

## Исторический репозиторий

https://github.com/Zeit241/kursovaya_4_kurs_backend
