# Mobile Android

Android-приложение для пациентов медицинской клиники.

**Часть монорепозитория:** см. [корневой README](../../README.md)

## Стек

- Kotlin, Android SDK
- Retrofit, Glide, Material Design

## Функции

- Авторизация и регистрация
- Врачи, услуги, запись на приём
- История приёмов, отзывы
- ИИ-помощник (Qwen Proxy)

## Сборка

1. Откройте проект в Android Studio
2. Скопируйте `local.properties.example` → `local.properties`
3. Укажите URL backend и Directus static token
4. Run или `./gradlew assembleDebug`

## Документация API

[API.MD](API.MD) — описание эндпоинтов backend, используемых приложением.

## Исторический репозиторий

https://github.com/Zeit241/kursovaya_4_kurs_mobile
