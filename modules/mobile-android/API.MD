# API Endpoints Documentation

Базовый URL: `http://localhost:8085`

## 🔐 Авторизация

Все эндпоинты, кроме `/api/auth/**` и `/api/users/create`, требуют JWT токен в заголовке:
```
Authorization: Bearer <your_jwt_token>
```

---

## 📋 Содержание

1. [Авторизация](#авторизация-1)
2. [Пользователи](#пользователи)
3. [Врачи](#врачи)
4. [Специализации](#специализации)
5. [Пациенты](#пациенты)
6. [Записи на прием](#записи-на-прием)
7. [Расписание](#расписание)
8. [Очередь](#очередь)
9. [Отзывы](#отзывы)
10. [Сводные отчёты](#сводные-отчёты)

---

## 🔑 Авторизация

### POST `/api/auth/login`
Вход в систему и получение JWT токена.

**Требует авторизации:** ❌ Нет

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Успешный вход",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "email": "user@example.com",
    "message": "Успешный вход в систему",
    "roleCode": "patient"
  }
}
```

**Поля ответа:**
- `token` (String) - JWT токен для авторизации
- `email` (String) - Email пользователя
- `message` (String) - Сообщение об успешном входе
- `roleCode` (String, optional) - Код роли пользователя (например: "patient", "doctor", "admin")

**Response 400 Bad Request:**
```json
{
  "success": false,
  "message": "Неверный email или пароль",
  "data": null
}
```

---

### POST `/api/auth/register`
Регистрация нового пользователя (создает пользователя и базового пациента).

**Требует авторизации:** ❌ Нет

**Request Body:**
```json
{
  "email": "user@example.com",
  "phone": "+1234567890",
  "password": "password123",
  "confirmPassword": "password123",
  "fio": "Иванов Иван Иванович"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Пользователь успешно зарегистрирован",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "phone": "+1234567890",
      "firstName": "Иван",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "message": "Пользователь успешно зарегистрирован"
  }
}
```

**Response 400 Bad Request:**
```json
{
  "success": false,
  "message": "Пользователь с таким email или телефоном уже существует",
  "data": null
}
```

---

### POST `/api/auth/register-with-patient`
Регистрация нового пользователя с полными данными пациента в одном запросе.

**Требует авторизации:** ❌ Нет

**Request Body:**
```json
{
  "email": "user@example.com",
  "phone": "+1234567890",
  "password": "password123",
  "confirmPassword": "password123",
  "fio": "Иванов Иван Иванович",
  "birthDate": "1990-01-15",
  "gender": 1,
  "insuranceNumber": "123-456-789 01"
}
```

**Поля:**
- `email` (String, required) - Email пользователя
- `phone` (String, required) - Телефон пользователя
- `password` (String, required) - Пароль
- `confirmPassword` (String, required) - Подтверждение пароля
- `fio` (String, required) - ФИО (может быть 1, 2 или 3+ слова)
- `birthDate` (LocalDate, required) - Дата рождения (формат: YYYY-MM-DD)
- `gender` (Short, required) - Пол: 1 = мужской, 2 = женский
- `insuranceNumber` (String, optional) - Номер страхового полиса (СНИЛС)

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Пользователь и пациент успешно зарегистрированы",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "patient": {
      "id": 1,
      "user": {
        "id": 1,
        "email": "user@example.com",
        "phone": "+1234567890",
        "firstName": "Иван",
        "lastName": "Иванов",
        "middleName": "Иванович",
        "createdAt": "2024-01-01T12:00:00Z",
        "updatedAt": "2024-01-01T12:00:00Z",
        "active": true
      },
      "birthDate": "1990-01-15",
      "gender": 1,
      "insuranceNumber": "123-456-789 01",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z"
    },
    "message": "Пользователь и пациент успешно зарегистрированы"
  }
}
```

**Response 400 Bad Request:**
```json
{
  "success": false,
  "message": "Пользователь с таким email или телефоном уже существует",
  "data": null
}
```

Или при ошибке валидации:
```json
{
  "success": false,
  "message": "Ошибка валидации",
  "data": [...список ошибок валидации...]
}
```

---

## 👥 Пользователи

### GET `/api/users`
Получить список всех пользователей.

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "phone": "+1234567890",
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Иванович",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "active": true
  }
]
```

---

### GET `/api/users/me`
Получить информацию о текущем авторизованном пользователе.

**Требует авторизации:** ✅ Да

* "status": 200,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "phone": "+1234567890",
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Иванович",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "active": true,
    "patientId": 1,
    "doctorId": null,
    "patient": {
      "id": 1,
      "birthDate": "1990-01-15",
      "gender": 1,
      "insuranceNumber": "1234567890123456",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z"
    },
    "doctor": null
  }
}
```

**Поля ответа:**
- `id` (Long) - ID пользователя
- `email` (String) - Email пользователя
- `phone` (String) - Телефон пользователя
- `firstName` (String) - Имя
- `lastName` (String) - Фамилия
- `middleName` (String, optional) - Отчество
- `createdAt` (OffsetDateTime) - Дата создания
- `updatedAt` (OffsetDateTime) - Дата обновления
- `active` (Boolean) - Активен ли пользователь
- `patientId` (Long, optional) - ID пациента, если пользователь является пациентом (null если нет)
- `doctorId` (Long, optional) - ID врача, если пользователь является врачом (null если нет)
- `patient` (Object, optional) - Данные пациента, если пользователь является пациентом (null если нет)
  - `id` (Long) - ID пациента
  - `birthDate` (LocalDate, optional) - Дата рождения
  - `gender` (Short, optional) - Пол (1 = мужской, 2 = женский)
  - `insuranceNumber` (String, optional) - Номер страхового полиса
  - `createdAt` (OffsetDateTime) - Дата создания
  - `updatedAt` (OffsetDateTime) - Дата обновления
- `doctor` (Object, optional) - Данные врача, если пользователь является врачом (null если нет)
  - `id` (Long) - ID врача
  - `displayName` (String) - Отображаемое имя врача
  - `bio` (String, optional) - Биография врача
  - `experienceYears` (Integer, optional) - Опыт работы в годах
  - `photo` (byte[], optional) - Фото врача
  - `specializations` (Array[SpecializationDto]) - Список специализаций врача
    - `id` (Long) - ID специализации
    - `code` (String) - Код специализации
    - `name` (String) - Название специализации
    - `description` (String, optional) - Описание специализации
  - `createdAt` (OffsetDateTime) - Дата создания
  - `updatedAt` (OffsetDateTime) - Дата обновления

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Пользователь не найден",
  "status": 404,
  "data": null
}
```
*Response 200 OK:**
```json
{
  "success": true,
  "message": "Данные пользователя успешно получены",
 
---

### GET `/api/users/userStats`
Получить статистику текущего авторизованного пользователя (количество записей, оценок и очередей).

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Статистика пользователя успешно получена",
  "data": {
    "appointmentsCount": 5,
    "reviewsCount": 3,
    "queueEntriesCount": 2
  }
}
```

**Поля ответа:**
- `appointmentsCount` (Long) - Количество записей на прием для текущего пользователя
- `reviewsCount` (Long) - Количество отзывов (оценок), оставленных текущим пользователем
- `queueEntriesCount` (Long) - Количество записей в очередях для текущего пользователя

**Примечание:** Если пользователь не является пациентом (не имеет `patientId`), все значения будут равны 0.

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Пользователь не найден",
  "data": null
}
```

---

### GET `/api/users/{id}`
Получить пользователя по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пользователя

**Response 200 OK:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "phone": "+1234567890",
  "firstName": "Иван",
  "lastName": "Иванов",
  "middleName": "Иванович",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z",
  "active": true
}
```

**Response 404 Not Found:** Пользователь не найден

---

### POST `/api/users/create`
Создать нового пользователя.

**Требует авторизации:** ❌ Нет

**Request Body:**
```json
{
  "email": "user@example.com",
  "phone": "+1234567890",
  "password": "password123",
  "confirmPassword": "password123",
  "fio": "Иванов Иван Иванович"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "phone": "+1234567890",
  "firstName": "Иван",
  "lastName": "Иванов",
  "middleName": "Иванович",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z",
  "active": true
}
```

---

### PUT `/api/users/{id}`
Обновить данные пользователя.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пользователя

**Request Body:**
```json
{
  "email": "newemail@example.com",
  "phone": "+9876543210",
  "firstName": "Петр",
  "lastName": "Петров",
  "middleName": "Петрович"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "email": "newemail@example.com",
  "phone": "+9876543210",
  "firstName": "Петр",
  "lastName": "Петров",
  "middleName": "Петрович",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T13:00:00Z",
  "active": true
}
```

---

### DELETE `/api/users/{id}`
Удалить пользователя.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пользователя

**Response 204 No Content:** Пользователь успешно удален

---

## 👨‍⚕️ Врачи

### GET `/api/doctors`
Получить список всех врачей или выполнить поиск с поддержкой пагинации и сортировки.

**Требует авторизации:** ✅ Да

**Query Parameters:**
- `q` (String, optional) - Поисковый запрос (поиск по имени, фамилии, специализации)
- `limit` (Integer, optional) - Максимальное количество записей для возврата
- `offset` (Integer, optional) - Количество записей для пропуска (используется вместе с limit)
- `sortBy` (String, optional) - Поле для сортировки. Доступные значения:
  - `firstName` или `first_name` - по имени
  - `lastName` или `last_name` - по фамилии
  - `experience` или `experience_years` или `experienceYears` - по опыту работы
  - `rating` - по среднему рейтингу из отзывов
  - `created` или `created_at` или `createdAt` - по дате создания
  - `updated` или `updated_at` или `updatedAt` - по дате обновления
- `sortOrder` (String, optional, default: `asc`) - Направление сортировки: `asc` (по возрастанию) или `desc` (по убыванию)

**Примеры:**
- `GET /api/doctors` - получить всех врачей
- `GET /api/doctors?q=терапевт` - найти врачей по запросу "терапевт"
- `GET /api/doctors?limit=10` - получить первые 10 врачей
- `GET /api/doctors?limit=10&offset=0` - получить первые 10 врачей (начиная с 0)
- `GET /api/doctors?limit=10&offset=10` - получить следующие 10 врачей (пропустить первые 10)
- `GET /api/doctors?sortBy=experience&sortOrder=desc` - отсортировать по опыту работы (от большего к меньшему)
- `GET /api/doctors?sortBy=lastName&sortOrder=asc` - отсортировать по фамилии (по алфавиту)
- `GET /api/doctors?sortBy=rating&sortOrder=desc` - отсортировать по рейтингу (от большего к меньшему)
- `GET /api/doctors?q=терапевт&limit=5&offset=0&sortBy=experience&sortOrder=desc` - найти врачей с пагинацией и сортировкой

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Список врачей успешно получен",
  "status": 200,
  "data": [
    {
      "id": 1,
      "user": {
        "id": 1,
        "email": "doctor@example.com",
        "phone": "+1234567890",
        "firstName": "Доктор",
        "lastName": "Иванов",
        "middleName": "Иванович",
        "createdAt": "2024-01-01T12:00:00Z",
        "updatedAt": "2024-01-01T12:00:00Z",
        "active": true
      },
      "displayName": "Доктор Иванов",
      "bio": "Опытный врач-терапевт",
      "experienceYears": 10,
      "photo": null,
      "rating": 4.5,
      "reviewCount": 12,
      "specializations": [
        {
          "id": 1,
          "code": "therapist",
          "name": "Терапевт",
          "description": "Врач общей практики"
        }
      ],
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z"
    }
  ]
}
```

---

### GET `/api/doctors/{id}`
Получить врача по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID врача

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Врач успешно получен",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "doctor@example.com",
      "phone": "+1234567890",
      "firstName": "Доктор",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "displayName": "Доктор Иванов",
    "bio": "Опытный врач-терапевт",
    "experienceYears": 10,
    "photo": null,
    "rating": 4.5,
    "reviewCount": 12,
    "specializations": [
      {
        "id": 1,
        "code": "therapist",
        "name": "Терапевт",
        "description": "Врач общей практики"
      }
    ],
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
}
```

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Врач не найден",
  "status": 404,
  "data": null
}
```

---

### POST `/api/doctors/create`
Создать нового врача с возможностью указания специализаций.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "user": {
    "email": "doctor@example.com",
    "phone": "+1234567890",
    "firstName": "Доктор",
    "lastName": "Иванов",
    "middleName": "Иванович"
  },
  "displayName": "Доктор Иванов",
  "bio": "Опытный врач-терапевт",
  "experienceYears": 10,
  "photo": null,
  "specializationIds": [1, 2]
}
```

**Поля запроса:**
- `user` (object, required) - Данные пользователя врача
  - `email` (String, required) - Email врача
  - `phone` (String, optional) - Телефон врача
  - `firstName` (String, required) - Имя
  - `lastName` (String, required) - Фамилия
  - `middleName` (String, optional) - Отчество
- `displayName` (String, required) - Отображаемое имя врача
- `bio` (String, optional) - Описание/биография (до 50 символов)
- `experienceYears` (Integer, optional) - Стаж работы в годах (0-80)
- `photo` (String, optional) - Base64-кодированное изображение
- `specializationIds` (Array<Long>, optional) - Список ID специализаций для назначения врачу

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Врач успешно создан",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "doctor@example.com",
      "phone": "+1234567890",
      "firstName": "Доктор",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "displayName": "Доктор Иванов",
    "bio": "Опытный врач-терапевт",
    "experienceYears": 10,
    "photo": null,
    "rating": null,
    "reviewCount": 0,
    "specializations": [
      {
        "id": 1,
        "code": "therapist",
        "name": "Терапевт",
        "description": "Врач общей практики"
      },
      {
        "id": 2,
        "code": "cardiologist",
        "name": "Кардиолог",
        "description": "Специалист по сердечно-сосудистой системе"
      }
    ],
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
}
```

**Response 400 Bad Request (ошибка валидации):**
```json
{
  "success": false,
  "message": "Данные не прошли валидацию.",
  "status": 400,
  "data": {
    "code": "VALIDATION_ERROR",
    "errors": {
      "displayName": "Отображаемое имя не может быть пустым",
      "user.email": "Некорректный формат email"
    }
  }
}
```

**Response 400 Bad Request (дубликат email):**
```json
{
  "success": false,
  "message": "Пользователь с таким email уже существует.",
  "status": 400,
  "data": {
    "rawMessage": "ERROR: duplicate key value violates unique constraint \"users_email_key\" ...",
    "code": "DATA_INTEGRITY_VIOLATION"
  }
}
```

---

### PUT `/api/doctors/{id}`
Обновить данные врача с возможностью изменения специализаций.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID врача

**Request Body:**
```json
{
  "user": {
    "email": "updated_doctor@example.com",
    "phone": "+9876543210",
    "firstName": "Пётр",
    "lastName": "Петров",
    "middleName": "Петрович"
  },
  "displayName": "Доктор Петров (обновлено)",
  "bio": "Обновленная биография",
  "experienceYears": 15,
  "photo": null,
  "specializationIds": [1, 3]
}
```

**Поля запроса:**
- `user` (object, optional) - Данные пользователя врача (обновляются только переданные поля)
  - `email` (String, optional) - Email врача
  - `phone` (String, optional) - Телефон врача
  - `firstName` (String, optional) - Имя
  - `lastName` (String, optional) - Фамилия
  - `middleName` (String, optional) - Отчество
- `displayName` (String, required) - Отображаемое имя врача
- `bio` (String, optional) - Описание/биография (до 50 символов)
- `experienceYears` (Integer, optional) - Стаж работы в годах (0-80)
- `photo` (String, optional) - Base64-кодированное изображение (пустая строка для удаления)
- `specializationIds` (Array<Long>, optional) - Список ID специализаций:
  - `null` - специализации не изменяются
  - `[]` (пустой массив) - все специализации удаляются
  - `[1, 2]` - устанавливаются указанные специализации

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Врач успешно обновлен",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "updated_doctor@example.com",
      "phone": "+9876543210",
      "firstName": "Пётр",
      "lastName": "Петров",
      "middleName": "Петрович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T14:00:00Z",
      "active": true
    },
    "displayName": "Доктор Петров (обновлено)",
    "bio": "Обновленная биография",
    "experienceYears": 15,
    "photo": null,
    "rating": 4.5,
    "reviewCount": 12,
    "specializations": [
      {
        "id": 1,
        "code": "therapist",
        "name": "Терапевт",
        "description": "Врач общей практики"
      },
      {
        "id": 3,
        "code": "neurologist",
        "name": "Невролог",
        "description": "Специалист по нервной системе"
      }
    ],
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T14:00:00Z"
  }
}
```

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Врач не найден",
  "status": 404,
  "data": null
}
```

**Response 400 Bad Request (ошибка валидации):**
```json
{
  "success": false,
  "message": "Данные не прошли валидацию.",
  "status": 400,
  "data": {
    "code": "VALIDATION_ERROR",
    "errors": {
      "displayName": "Отображаемое имя не может быть пустым"
    }
  }
}
```

---

### DELETE `/api/doctors/{id}`
Удалить врача.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID врача

**Response 204 No Content:** Врач успешно удален

---

## 🏥 Специализации

### GET `/api/specializations`
Получить список всех специализаций.

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "code": "therapist",
    "name": "Терапевт",
    "description": "Врач общей практики"
  },
  {
    "id": 2,
    "code": "cardiologist",
    "name": "Кардиолог",
    "description": "Врач, специализирующийся на заболеваниях сердца"
  }
]
```

---

### GET `/api/specializations/{id}`
Получить специализацию по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID специализации

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "therapist",
  "name": "Терапевт",
  "description": "Врач общей практики"
}
```

**Response 404 Not Found:** Специализация не найдена

---

### GET `/api/specializations/code/{code}`
Получить специализацию по коду.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `code` (String) - Код специализации

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "therapist",
  "name": "Терапевт",
  "description": "Врач общей практики"
}
```

**Response 404 Not Found:** Специализация не найдена

---

### POST `/api/specializations`
Создать новую специализацию.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "code": "neurologist",
  "name": "Невролог",
  "description": "Врач, специализирующийся на заболеваниях нервной системы"
}
```

**Response 200 OK:**
```json
{
  "id": 3,
  "code": "neurologist",
  "name": "Невролог",
  "description": "Врач, специализирующийся на заболеваниях нервной системы"
}
```

---

### PUT `/api/specializations/{id}`
Обновить специализацию.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID специализации

**Request Body:**
```json
{
  "code": "neurologist",
  "name": "Невролог (обновлено)",
  "description": "Обновленное описание"
}
```

**Response 200 OK:**
```json
{
  "id": 3,
  "code": "neurologist",
  "name": "Невролог (обновлено)",
  "description": "Обновленное описание"
}
```

**Response 404 Not Found:** Специализация не найдена

---

### DELETE `/api/specializations/{id}`
Удалить специализацию.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID специализации

**Response 204 No Content:** Специализация успешно удалена

---

## 🏥 Пациенты

### GET `/api/patients`
Получить список всех пациентов.

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "user": { ... },
    "birthDate": "1990-01-01",
    "gender": 1,
    "insuranceNumber": "123-456-789 01",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
]
```

---

### GET `/api/patients/{id}`
Получить пациента по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пациента

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Пациент успешно получен",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "patient@example.com",
      "phone": "+1234567890",
      "firstName": "Иван",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "birthDate": "1990-01-01",
    "gender": 1,
    "insuranceNumber": "123-456-789 01",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
}
```

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Пациент не найден",
  "status": 404,
  "data": null
}
```

---

### POST `/api/patients`
Создать нового пациента.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "user": {
    "email": "patient@example.com",
    "phone": "+1234567890",
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Иванович"
  },
  "birthDate": "1990-01-01",
  "gender": 1,
  "insuranceNumber": "123-456-789 01"
}
```

**Поля запроса:**
- `user` (object, required) - Данные пользователя пациента
  - `email` (String, required) - Email пациента
  - `phone` (String, optional) - Телефон пациента
  - `firstName` (String, required) - Имя
  - `lastName` (String, required) - Фамилия
  - `middleName` (String, optional) - Отчество
- `birthDate` (LocalDate, optional) - Дата рождения (формат: YYYY-MM-DD)
- `gender` (Short, optional) - Пол: 1 = мужской, 2 = женский
- `insuranceNumber` (String, optional) - Номер страхового полиса (СНИЛС)

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Пациент успешно создан",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "patient@example.com",
      "phone": "+1234567890",
      "firstName": "Иван",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "birthDate": "1990-01-01",
    "gender": 1,
    "insuranceNumber": "123-456-789 01",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
}
```

**Response 400 Bad Request (ошибка валидации):**
```json
{
  "success": false,
  "message": "Данные не прошли валидацию.",
  "status": 400,
  "data": {
    "code": "VALIDATION_ERROR",
    "errors": {
      "user.email": "Некорректный формат email",
      "user.firstName": "Имя не может быть пустым"
    }
  }
}
```

**Response 400 Bad Request (дубликат email):**
```json
{
  "success": false,
  "message": "Пользователь с таким email уже существует.",
  "status": 400,
  "data": {
    "rawMessage": "ERROR: duplicate key value violates unique constraint \"users_email_key\" ...",
    "code": "DATA_INTEGRITY_VIOLATION"
  }
}
```

---

### PUT `/api/patients/{id}`
Обновить данные пациента.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пациента

**Request Body:**
```json
{
  "user": {
    "email": "newemail@example.com",
    "phone": "+9876543210",
    "firstName": "Петр",
    "lastName": "Петров",
    "middleName": "Петрович"
  },
  "birthDate": "1990-01-01",
  "gender": 1,
  "insuranceNumber": "123-456-789 02"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Пациент успешно обновлен",
  "status": 200,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "newemail@example.com",
      "phone": "+9876543210",
      "firstName": "Петр",
      "lastName": "Петров",
      "middleName": "Петрович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T15:00:00Z",
      "active": true
    },
    "birthDate": "1990-01-01",
    "gender": 1,
    "insuranceNumber": "123-456-789 02",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T15:00:00Z"
  }
}
```

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Пациент не найден",
  "status": 404,
  "data": null
}
```

**Response 400 Bad Request (ошибка валидации):**
```json
{
  "success": false,
  "message": "Данные не прошли валидацию.",
  "status": 400,
  "data": {
    "code": "VALIDATION_ERROR",
    "errors": {
      "user.email": "Некорректный формат email"
    }
  }
}
```

---

### DELETE `/api/patients/{id}`
Удалить пациента.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID пациента

**Response 204 No Content:** Пациент успешно удален

---

## 📅 Записи на прием

### GET `/api/appointments`
Получить список всех записей на прием.

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": 1,
    "roomId": 1,
    "startTime": "2024-01-15T10:00:00Z",
    "endTime": "2024-01-15T10:30:00Z",
    "status": "scheduled",
    "source": "web",
    "createdBy": 1,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "cancelReason": null
  }
]
```

**Поля ответа:**
- `id` (Long) - ID записи
- `scheduleId` (Long, optional) - ID расписания, к которому относится запись
- `doctorId` (Long) - ID врача
- `patientId` (Long) - ID пациента
- `roomId` (Long, optional) - ID кабинета
- `startTime` (OffsetDateTime) - Время начала приема
- `endTime` (OffsetDateTime) - Время окончания приема
- `status` (String) - Статус записи: `scheduled`, `confirmed`, `in_progress`, `completed`, `cancelled`, `no_show`
- `source` (String) - Источник записи: `mobile`, `reception`, `web`, `admin`
- `createdBy` (Long, optional) - ID пользователя, создавшего запись
- `createdAt` (OffsetDateTime) - Дата создания
- `updatedAt` (OffsetDateTime) - Дата обновления
- `cancelReason` (String, optional) - Причина отмены

---

### GET `/api/appointments/check`
Проверка доступности эндпоинта (дублирует `/api/appointments`).

**Требует авторизации:** ✅ Да

**Response 200 OK:** Список всех записей

---

### GET `/api/appointments/doctor/{doctorId}`
Получить все записи на прием для конкретного врача.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": 1,
    "roomId": 1,
    "startTime": "2024-01-15T10:00:00Z",
    "endTime": "2024-01-15T10:30:00Z",
    "isBooked": true,
    "status": "scheduled",
    "source": "web",
    "createdBy": 1,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "cancelReason": null,
    "diagnosis": null
  }
]
```

**Примечание:** Поле `isBooked` = `true` если слот занят (есть `patientId`) **ИЛИ** если время слота (`startTime`) уже прошло.

---

### GET `/api/appointments/patient/{patientId}`
Получить все записи на прием для конкретного пациента.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `patientId` (Long) - ID пациента

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": 1,
    "roomId": 1,
    "startTime": "2024-01-15T10:00:00Z",
    "endTime": "2024-01-15T10:30:00Z",
    "isBooked": true,
    "status": "scheduled",
    "source": "web",
    "createdBy": 1,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "cancelReason": null,
    "diagnosis": null
  }
]
```

---

### GET `/api/appointments/{id}`
Получить запись на прием по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID записи

**Response 200 OK:**
```json
{
  "id": 1,
  "scheduleId": 1,
  "doctorId": 1,
  "patientId": 1,
  "roomId": 1,
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "isBooked": true,
  "status": "scheduled",
  "source": "web",
  "createdBy": 1,
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z",
  "cancelReason": null,
  "diagnosis": "ОРВИ, легкая форма",
  "doctor": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "doctor@example.com",
      "phone": "+1234567890",
      "firstName": "Доктор",
      "lastName": "Иванов",
      "middleName": "Иванович",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "displayName": "Доктор Иванов",
    "bio": "Опытный врач-терапевт",
    "experienceYears": 10,
    "photo": null,
    "rating": 4.5,
    "reviewCount": 12,
    "specializations": [
      {
        "id": 1,
        "code": "therapist",
        "name": "Терапевт",
        "description": "Врач общей практики"
      }
    ],
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  },
  "patient": {
    "id": 1,
    "user": {
      "id": 2,
      "email": "patient@example.com",
      "phone": "+9876543210",
      "firstName": "Иван",
      "lastName": "Петров",
      "middleName": "Сергеевич",
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "active": true
    },
    "birthDate": "1990-01-15",
    "gender": 1,
    "insuranceNumber": "123-456-789 01",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  },
  "room": {
    "id": 1,
    "code": "101",
    "name": "Кабинет терапевта №1"
  }
}
```

**Поля ответа:**
- `id` (Long) - ID записи
- `scheduleId` (Long, optional) - ID расписания, к которому относится запись
- `doctorId` (Long) - ID врача
- `patientId` (Long, optional) - ID пациента
- `roomId` (Long, optional) - ID кабинета
- `startTime` (OffsetDateTime) - Время начала приема
- `endTime` (OffsetDateTime) - Время окончания приема
- `status` (String) - Статус записи: `scheduled`, `confirmed`, `in_progress`, `completed`, `cancelled`, `no_show`
- `source` (String) - Источник записи: `mobile`, `reception`, `web`, `admin`
- `createdBy` (Long, optional) - ID пользователя, создавшего запись
- `createdAt` (OffsetDateTime) - Дата создания
- `updatedAt` (OffsetDateTime) - Дата обновления
- `cancelReason` (String, optional) - Причина отмены
- `diagnosis` (String, optional) - Диагноз, поставленный врачом
- `doctor` (Object, optional) - Полная информация о враче:
  - `id` (Long) - ID врача
  - `user` (Object) - Информация о пользователе врача
  - `displayName` (String) - Отображаемое имя врача
  - `bio` (String, optional) - Биография врача
  - `experienceYears` (Integer, optional) - Опыт работы в годах
  - `photo` (byte[], optional) - Фото врача
  - `rating` (Double, optional) - Средний рейтинг из отзывов
  - `reviewCount` (Integer, optional) - Количество отзывов
  - `specializations` (Array, optional) - Список специализаций врача
  - `createdAt` (OffsetDateTime) - Дата создания
  - `updatedAt` (OffsetDateTime) - Дата обновления
- `patient` (Object, optional) - Полная информация о пациенте:
  - `id` (Long) - ID пациента
  - `user` (Object) - Информация о пользователе пациента
  - `birthDate` (LocalDate) - Дата рождения
  - `gender` (Short) - Пол: 1 = мужской, 2 = женский
  - `insuranceNumber` (String, optional) - Номер страхового полиса (СНИЛС)
  - `createdAt` (OffsetDateTime) - Дата создания
  - `updatedAt` (OffsetDateTime) - Дата обновления
- `room` (Object, optional) - Полная информация о кабинете:
  - `id` (Long) - ID кабинета
  - `code` (String) - Код кабинета
  - `name` (String, optional) - Название кабинета

**Response 404 Not Found:** Запись не найдена

---

### GET `/api/appointments/available`
Получить список доступного времени записи для врача на указанную дату.

**Требует авторизации:** ✅ Да

**Query Parameters:**
- `doctorId` (Long, required) - ID врача
- `date` (LocalDate, required) - Дата для получения доступного времени (формат: YYYY-MM-DD)

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": null,
    "roomId": 1,
    "startTime": "2024-01-15T08:00:00Z",
    "endTime": "2024-01-15T08:30:00Z",
    "isBooked": false,
    "status": "available",
    "source": "schedule",
    "createdBy": null,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "cancelReason": null,
    "diagnosis": null
  },
  {
    "id": 2,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": 1,
    "roomId": 1,
    "startTime": "2024-01-15T08:30:00Z",
    "endTime": "2024-01-15T09:00:00Z",
    "isBooked": true,
    "status": "scheduled",
    "source": "schedule",
    "createdBy": null,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "cancelReason": null,
    "diagnosis": null
  }
]
```

**Поля ответа:**
- `id` (Long) - ID записи
- `scheduleId` (Long, optional) - ID расписания
- `doctorId` (Long) - ID врача
- `patientId` (Long, optional) - ID пациента (null если слот свободен)
- `roomId` (Long, optional) - ID кабинета
- `startTime` (OffsetDateTime) - Время начала приема
- `endTime` (OffsetDateTime) - Время окончания приема
- `isBooked` (Boolean) - Флаг занятости слота (true если есть patient_id **ИЛИ** если время слота уже прошло - нельзя записаться в прошедший слот)
- `status` (String) - Статус записи
- `source` (String) - Источник записи
- `createdBy` (Long, optional) - ID пользователя, создавшего запись
- `createdAt` (OffsetDateTime) - Дата создания
- `updatedAt` (OffsetDateTime) - Дата обновления
- `cancelReason` (String, optional) - Причина отмены
- `diagnosis` (String, optional) - Диагноз, поставленный врачом

---

### POST `/api/appointments/book`
Забронировать запись на прием (создать запись).

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "appointmentId": 1,
  "userId": 1
}
```

**Поля запроса:**
- `appointmentId` (Long, required) - ID записи (appointment), которую нужно забронировать
- `userId` (Long, required) - ID пользователя (user), который бронирует запись

**Response 200 OK:**
```json
{
  "id": 1,
  "scheduleId": 1,
  "doctorId": 1,
  "patientId": 1,
  "roomId": 1,
  "startTime": "2024-01-15T08:00:00Z",
  "endTime": "2024-01-15T08:30:00Z",
  "status": "scheduled",
  "source": "schedule",
  "createdBy": null,
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z",
  "cancelReason": null
}
```

**Response 400 Bad Request:** 
- Запись не найдена
- Запись уже занята
- Пациент не найден по user_id

---

### POST `/api/appointments`
Создать новую запись на прием.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "schedule": {
    "id": 1
  },
  "doctor": {
    "id": 1
  },
  "patient": {
    "id": 1
  },
  "room": {
    "id": 1
  },
  "startTime": "2024-01-15T10:00:00Z",
  "endTime": "2024-01-15T10:30:00Z",
  "status": "scheduled",
  "source": "web",
  "createdBy": {
    "id": 1
  }
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "doctor": { ... },
  "patient": { ... },
  "appointmentDate": "2024-01-15T10:00:00Z",
  "status": "SCHEDULED",
  "notes": "Плановый осмотр",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

---

### DELETE `/api/appointments/{id}`
Удалить запись на прием.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID записи

**Response 204 No Content:** Запись успешно удалена

---

### POST `/api/appointments/{id}/cancel`
Отменить запись на прием (устанавливает статус "cancelled").

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID записи на прием

**Request Body (опционально):**
```json
{
  "cancelReason": "Пациент не смог прийти"
}
```

**Поля запроса:**
- `cancelReason` (String, optional) - Причина отмены записи

**Примечание:** 
- При отмене записи автоматически удаляется пациент из очереди к врачу (если он был в очереди)
- Если запись уже отменена, возвращается текущее состояние записи без изменений

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Запись на прием успешно отменена",
  "appointment": {
    "id": 1,
    "scheduleId": 1,
    "doctorId": 1,
    "patientId": 1,
    "roomId": 1,
    "startTime": "2024-01-15T10:00:00Z",
    "endTime": "2024-01-15T10:30:00Z",
    "status": "cancelled",
    "source": "web",
    "createdBy": 1,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-15T14:00:00Z",
    "cancelReason": "Пациент не смог прийти"
  },
  "queue": [
    {
      "id": 1,
      "doctorId": 1,
      "appointmentId": 2,
      "patientId": 2,
      "position": 1,
      "lastUpdated": "2024-01-01T12:00:00Z"
    }
  ]
}
```

**Поля ответа:**
- `success` (Boolean) - Успешность операции
- `message` (String) - Сообщение о результате операции
- `appointment` (Object) - Обновленная запись на прием:
  - `id` (Long) - ID записи
  - `scheduleId` (Long, optional) - ID расписания
  - `doctorId` (Long) - ID врача
  - `patientId` (Long, optional) - ID пациента
  - `roomId` (Long, optional) - ID кабинета
  - `startTime` (OffsetDateTime) - Время начала приема
  - `endTime` (OffsetDateTime) - Время окончания приема
  - `status` (String) - Статус записи (будет "cancelled")
  - `source` (String) - Источник записи
  - `createdBy` (Long, optional) - ID пользователя, создавшего запись
  - `createdAt` (OffsetDateTime) - Дата создания
  - `updatedAt` (OffsetDateTime) - Дата обновления
  - `cancelReason` (String, optional) - Причина отмены
- `queue` (Array) - Обновленная очередь к врачу после отмены записи

**Response 404 Not Found:**
```json
{
  "error": "Запись на прием не найдена"
}
```

---

## 📆 Расписание

### GET `/api/schedules`
Получить список всех расписаний.

**Требует авторизации:** ✅ Да

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "doctorId": 1,
    "roomId": 1,
    "dateAt": "2024-01-15",
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
]
```

**Поля ответа:**
- `id` (Long) - ID расписания
- `doctorId` (Long) - ID врача
- `roomId` (Long, optional) - ID кабинета
- `dateAt` (LocalDate) - Дата расписания (формат: YYYY-MM-DD)
- `startTime` (LocalTime) - Время начала приема (формат: HH:mm:ss)
- `endTime` (LocalTime) - Время окончания приема (формат: HH:mm:ss)
- `slotDurationMinutes` (Integer) - Длительность слота в минутах
- `createdAt` (OffsetDateTime) - Дата создания
- `updatedAt` (OffsetDateTime) - Дата обновления

---

### GET `/api/schedules/doctor/{doctorId}`
Получить расписание для конкретного врача.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "doctorId": 1,
    "roomId": 1,
    "dateAt": "2024-01-15",
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
]
```

---

### GET `/api/schedules/{id}`
Получить расписание по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID расписания

**Response 200 OK:**
```json
{
  "id": 1,
  "doctorId": 1,
  "roomId": 1,
  "dateAt": "2024-01-15",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotDurationMinutes": 30,
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

**Response 404 Not Found:** Расписание не найдено

---

### POST `/api/schedules`
Создать новое расписание.

**Требует авторизации:** ✅ Да

**Примечание:** При создании расписания автоматически создаются записи (appointments) с пустым `patient_id` для всех временных слотов от `startTime` до `endTime` с интервалом `slotDurationMinutes`. Например, если время работы с 08:00 до 10:00 и длительность приема 30 минут, будет создано 4 записи: 08:00-08:30, 08:30-09:00, 09:00-09:30, 09:30-10:00.

**Request Body:**
```json
{
  "doctor": {
    "id": 1
  },
  "room": {
    "id": 1
  },
  "dateAt": "2024-01-15",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotDurationMinutes": 30
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "doctorId": 1,
  "roomId": 1,
  "dateAt": "2024-01-15",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotDurationMinutes": 30,
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

---

### DELETE `/api/schedules/{id}`
Удалить расписание.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID расписания

**Response 204 No Content:** Расписание успешно удалено

---

## 🎫 Очередь

### GET `/api/queue/doctor/{doctorId}`
Получить очередь для конкретного врача.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "doctorId": 1,
    "appointmentId": 1,
    "patientId": 1,
    "position": 1,
    "lastUpdated": "2024-01-01T12:00:00Z"
  }
]
```

---

### GET `/api/queue/{id}`
Получить запись очереди по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID записи очереди

**Response 200 OK:**
```json
{
  "id": 1,
  "doctorId": 1,
  "appointmentId": 1,
  "patientId": 1,
  "position": 1,
  "lastUpdated": "2024-01-01T12:00:00Z"
}
```

**Response 404 Not Found:** Запись очереди не найдена

---

### POST `/api/queue`
Добавить пациента в очередь.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "doctor": {
    "id": 1
  },
  "appointment": {
    "id": 1
  },
  "patient": {
    "id": 1
  },
  "position": 1
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "doctorId": 1,
  "appointmentId": 1,
  "patientId": 1,
  "position": 1,
  "lastUpdated": "2024-01-01T12:00:00Z"
}
```

---

### DELETE `/api/queue/{id}`
Удалить запись из очереди.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID записи очереди

**Response 204 No Content:** Запись успешно удалена из очереди

---

## ⭐ Отзывы

### GET `/api/reviews/doctor/{doctorId}`
Получить все отзывы для конкретного врача.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "appointmentId": 1,
    "doctorId": 1,
    "patientId": 1,
    "patientName": "Иванов Иван Иванович",
    "rating": 5,
    "reviewText": "Отличный врач, очень внимательный",
    "createdAt": "2024-01-01T12:00:00Z"
  }
]
```

**Поля ответа:**
- `id` (Long) - ID отзыва
- `appointmentId` (Long) - ID записи на прием
- `doctorId` (Long) - ID врача
- `patientId` (Long) - ID пациента
- `patientName` (String, optional) - Полное имя пациента (Фамилия Имя Отчество)
- `rating` (Short) - Оценка от 1 до 5
- `reviewText` (String, optional) - Текст отзыва
- `createdAt` (OffsetDateTime) - Дата создания отзыва

---

### GET `/api/reviews/{id}`
Получить отзыв по ID.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID отзыва

**Response 200 OK:**
```json
{
  "id": 1,
  "appointmentId": 1,
  "doctorId": 1,
  "patientId": 1,
  "patientName": "Иванов Иван Иванович",
  "rating": 5,
  "reviewText": "Отличный врач, очень внимательный",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

**Поля ответа:**
- `id` (Long) - ID отзыва
- `appointmentId` (Long) - ID записи на прием
- `doctorId` (Long) - ID врача
- `patientId` (Long) - ID пациента
- `patientName` (String, optional) - Полное имя пациента (Фамилия Имя Отчество)
- `rating` (Short) - Оценка от 1 до 5
- `reviewText` (String, optional) - Текст отзыва
- `createdAt` (OffsetDateTime) - Дата создания отзыва

**Response 404 Not Found:** Отзыв не найден

---

### GET `/api/reviews/appointment/{appointmentId}`
Получить отзыв по ID записи на прием.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `appointmentId` (Long) - ID записи на прием

**Response 200 OK:**
```json
{
  "id": 1,
  "appointmentId": 1,
  "doctorId": 1,
  "patientId": 1,
  "patientName": "Иванов Иван Иванович",
  "rating": 5,
  "reviewText": "Отличный врач, очень внимательный",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

**Поля ответа:**
- `id` (Long) - ID отзыва
- `appointmentId` (Long) - ID записи на прием
- `doctorId` (Long) - ID врача
- `patientId` (Long) - ID пациента
- `patientName` (String, optional) - Полное имя пациента (Фамилия Имя Отчество)
- `rating` (Short) - Оценка от 1 до 5
- `reviewText` (String, optional) - Текст отзыва
- `createdAt` (OffsetDateTime) - Дата создания отзыва

**Response 404 Not Found:** Отзыв не найден

---

### POST `/api/reviews`
Создать новый отзыв.

**Требует авторизации:** ✅ Да

**Request Body:**
```json
{
  "appointment": {
    "id": 1
  },
  "doctor": {
    "id": 1
  },
  "patient": {
    "id": 1
  },
  "rating": 5,
  "reviewText": "Отличный врач, очень внимательный"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "appointmentId": 1,
  "doctorId": 1,
  "patientId": 1,
  "patientName": "Иванов Иван Иванович",
  "rating": 5,
  "reviewText": "Отличный врач, очень внимательный",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

---

### PUT `/api/reviews/{id}`
Обновить отзыв.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID отзыва

**Request Body:**
```json
{
  "rating": 4,
  "reviewText": "Хороший врач, но можно было бы лучше"
}
```

**Поля запроса:**
- `rating` (Short, optional) - Оценка от 1 до 5
- `reviewText` (String, optional) - Текст отзыва

**Примечание:** Обновляются только поля `rating` и `reviewText`. Связи с `appointment`, `doctor` и `patient` не изменяются. Поле `createdAt` остается неизменным.

**Response 200 OK:**
```json
{
  "id": 1,
  "appointmentId": 1,
  "doctorId": 1,
  "patientId": 1,
  "patientName": "Иванов Иван Иванович",
  "rating": 4,
  "reviewText": "Хороший врач, но можно было бы лучше",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

**Поля ответа:**
- `id` (Long) - ID отзыва
- `appointmentId` (Long) - ID записи на прием
- `doctorId` (Long) - ID врача
- `patientId` (Long) - ID пациента
- `patientName` (String, optional) - Полное имя пациента (Фамилия Имя Отчество)
- `rating` (Short) - Оценка от 1 до 5
- `reviewText` (String, optional) - Текст отзыва
- `createdAt` (OffsetDateTime) - Дата создания отзыва (не изменяется при обновлении)

**Response 404 Not Found:** Отзыв не найден

---

### DELETE `/api/reviews/{id}`
Удалить отзыв.

**Требует авторизации:** ✅ Да

**Path Parameters:**
- `id` (Long) - ID отзыва

**Response 204 No Content:** Отзыв успешно удален

---

## 📊 Сводные отчёты

Эндпоинты для получения сводных отчётов на основе информации из БД. 

**Доступ:** Только для пользователей с ролью `admin`.

### GET `/api/reports/daily`
Получить перечень всех записанных пациентов на определённую дату.

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily?date=2024-01-15
```

**Response 200 OK:**
```json
{
  "date": "2024-01-15",
  "totalAppointments": 12,
  "scheduledCount": 5,
  "completedCount": 4,
  "cancelledCount": 2,
  "noShowCount": 1,
  "doctorId": null,
  "doctorDisplayName": null,
  "appointments": [
    {
      "appointmentId": 1,
      "startTime": "2024-01-15T08:00:00Z",
      "endTime": "2024-01-15T08:30:00Z",
      "status": "completed",
      "patientId": 1,
      "patientFullName": "Иванов Иван Иванович",
      "patientPhone": "+79001234567",
      "patientEmail": "ivanov@example.com",
      "patientBirthDate": "1990-01-15",
      "patientGender": "Мужской",
      "patientInsuranceNumber": "123-456-789 01",
      "doctorId": 1,
      "doctorDisplayName": "Доктор Петров",
      "doctorPhone": "+79009876543",
      "doctorEmail": "petrov@clinic.com",
      "roomId": 1,
      "roomNumber": "101",
      "diagnosis": "ОРВИ, легкая форма",
      "cancelReason": null,
      "createdAt": "2024-01-10T10:00:00Z"
    }
  ]
}
```

**Поля ответа:**
- `date` (LocalDate) - Дата отчёта
- `totalAppointments` (Integer) - Общее количество записей с пациентами
- `scheduledCount` (Integer) - Количество запланированных/подтверждённых записей
- `completedCount` (Integer) - Количество завершённых приёмов
- `cancelledCount` (Integer) - Количество отменённых записей
- `noShowCount` (Integer) - Количество неявок
- `doctorId` (Long, optional) - ID врача (null для общего отчёта)
- `doctorDisplayName` (String, optional) - Имя врача (null для общего отчёта)
- `appointments` (Array) - Список записей на приём:
  - `appointmentId` (Long) - ID записи
  - `startTime` (OffsetDateTime) - Время начала приёма
  - `endTime` (OffsetDateTime) - Время окончания приёма
  - `status` (String) - Статус записи
  - `patientId` (Long) - ID пациента
  - `patientFullName` (String) - Полное имя пациента (Фамилия Имя Отчество)
  - `patientPhone` (String, optional) - Телефон пациента
  - `patientEmail` (String) - Email пациента
  - `patientBirthDate` (LocalDate, optional) - Дата рождения пациента
  - `patientGender` (String, optional) - Пол пациента ("Мужской" или "Женский")
  - `patientInsuranceNumber` (String, optional) - Номер страхового полиса
  - `doctorId` (Long) - ID врача
  - `doctorDisplayName` (String) - Отображаемое имя врача
  - `doctorPhone` (String, optional) - Телефон врача
  - `doctorEmail` (String) - Email врача
  - `roomId` (Long, optional) - ID кабинета
  - `roomNumber` (String, optional) - Номер кабинета
  - `diagnosis` (String, optional) - Диагноз
  - `cancelReason` (String, optional) - Причина отмены
  - `createdAt` (OffsetDateTime) - Дата создания записи

**Response 403 Forbidden:**
```json
{
  "error": "Доступ запрещён. Требуется роль администратора."
}
```

---

### GET `/api/reports/daily/doctor/{doctorId}`
Получить перечень записанных пациентов на определённую дату к определённому врачу.

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily/doctor/1?date=2024-01-15
```

**Response 200 OK:**
```json
{
  "date": "2024-01-15",
  "totalAppointments": 6,
  "scheduledCount": 2,
  "completedCount": 3,
  "cancelledCount": 1,
  "noShowCount": 0,
  "doctorId": 1,
  "doctorDisplayName": "Доктор Петров",
  "appointments": [
    {
      "appointmentId": 1,
      "startTime": "2024-01-15T08:00:00Z",
      "endTime": "2024-01-15T08:30:00Z",
      "status": "completed",
      "patientId": 1,
      "patientFullName": "Иванов Иван Иванович",
      "patientPhone": "+79001234567",
      "patientEmail": "ivanov@example.com",
      "patientBirthDate": "1990-01-15",
      "patientGender": "Мужской",
      "patientInsuranceNumber": "123-456-789 01",
      "doctorId": 1,
      "doctorDisplayName": "Доктор Петров",
      "doctorPhone": "+79009876543",
      "doctorEmail": "petrov@clinic.com",
      "roomId": 1,
      "roomNumber": "101",
      "diagnosis": "ОРВИ, легкая форма",
      "cancelReason": null,
      "createdAt": "2024-01-10T10:00:00Z"
    }
  ]
}
```

**Response 403 Forbidden:**
```json
{
  "error": "Доступ запрещён. Требуется роль администратора."
}
```

---

### GET `/api/reports/range`
Получить перечень записей за период (несколько дней).

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
```json
{
  "date": "2024-01-01",
  "totalAppointments": 150,
  "scheduledCount": 50,
  "completedCount": 80,
  "cancelledCount": 15,
  "noShowCount": 5,
  "doctorId": null,
  "doctorDisplayName": null,
  "appointments": [...]
}
```

**Response 400 Bad Request:**
```json
{
  "error": "Дата начала не может быть позже даты окончания."
}
```

**Response 403 Forbidden:**
```json
{
  "error": "Доступ запрещён. Требуется роль администратора."
}
```

---

### GET `/api/reports/range/doctor/{doctorId}`
Получить перечень записей к определённому врачу за период.

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range/doctor/1?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
```json
{
  "date": "2024-01-01",
  "totalAppointments": 25,
  "scheduledCount": 8,
  "completedCount": 15,
  "cancelledCount": 2,
  "noShowCount": 0,
  "doctorId": 1,
  "doctorDisplayName": "Доктор Петров",
  "appointments": [...]
}
```

**Response 400 Bad Request:**
```json
{
  "error": "Дата начала не может быть позже даты окончания."
}
```

**Response 403 Forbidden:**
```json
{
  "error": "Доступ запрещён. Требуется роль администратора."
}
```

---

### 📥 Экспорт отчётов в файлы

#### GET `/api/reports/daily/excel`
Скачать отчёт за дату в формате Excel (.xlsx).

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily/excel?date=2024-01-15
```

**Response 200 OK:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="report_2024-01-15.xlsx"`
- Body: Бинарный файл Excel

---

#### GET `/api/reports/daily/pdf`
Скачать отчёт за дату в формате PDF.

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily/pdf?date=2024-01-15
```

**Response 200 OK:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="report_2024-01-15.pdf"`
- Body: Бинарный файл PDF

---

#### GET `/api/reports/daily/doctor/{doctorId}/excel`
Скачать отчёт по врачу за дату в формате Excel (.xlsx).

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily/doctor/1/excel?date=2024-01-15
```

**Response 200 OK:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="report_doctor1_2024-01-15.xlsx"`
- Body: Бинарный файл Excel

---

#### GET `/api/reports/daily/doctor/{doctorId}/pdf`
Скачать отчёт по врачу за дату в формате PDF.

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `date` (LocalDate, required) - Дата для отчёта (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/daily/doctor/1/pdf?date=2024-01-15
```

**Response 200 OK:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="report_doctor1_2024-01-15.pdf"`
- Body: Бинарный файл PDF

---

#### GET `/api/reports/range/excel`
Скачать отчёт за период в формате Excel (.xlsx).

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range/excel?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="report_2024-01-01_2024-01-31.xlsx"`
- Body: Бинарный файл Excel

---

#### GET `/api/reports/range/pdf`
Скачать отчёт за период в формате PDF.

**Требует авторизации:** ✅ Да (только admin)

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range/pdf?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="report_2024-01-01_2024-01-31.pdf"`
- Body: Бинарный файл PDF

---

#### GET `/api/reports/range/doctor/{doctorId}/excel`
Скачать отчёт по врачу за период в формате Excel (.xlsx).

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range/doctor/1/excel?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="report_doctor1_2024-01-01_2024-01-31.xlsx"`
- Body: Бинарный файл Excel

---

#### GET `/api/reports/range/doctor/{doctorId}/pdf`
Скачать отчёт по врачу за период в формате PDF.

**Требует авторизации:** ✅ Да (только admin)

**Path Parameters:**
- `doctorId` (Long) - ID врача

**Query Parameters:**
- `startDate` (LocalDate, required) - Дата начала периода (формат: YYYY-MM-DD)
- `endDate` (LocalDate, required) - Дата окончания периода (формат: YYYY-MM-DD)

**Пример запроса:**
```
GET /api/reports/range/doctor/1/pdf?startDate=2024-01-01&endDate=2024-01-31
```

**Response 200 OK:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="report_doctor1_2024-01-01_2024-01-31.pdf"`
- Body: Бинарный файл PDF

---

### Общие ошибки экспорта

**Response 403 Forbidden:**
```json
{
  "error": "Доступ запрещён. Требуется роль администратора."
}
```

**Response 500 Internal Server Error:**
```json
{
  "error": "Ошибка при формировании отчёта: <описание ошибки>"
}
```

---

## 📝 Примечания

1. **Формат даты:** Все даты и время в формате ISO 8601 (например: `2024-01-01T12:00:00Z`)

2. **Ошибки валидации:** При ошибках валидации возвращается статус `400 Bad Request` с описанием ошибок

3. **JWT токен:** Токен действителен 24 часа (86400000 мс). После истечения срока действия необходимо выполнить повторный вход

4. **CORS:** Настроен для всех источников. В production рекомендуется ограничить разрешенные источники

5. **Статусы ответов:**
   - `200 OK` - Успешный запрос
   - `204 No Content` - Успешное удаление
   - `400 Bad Request` - Ошибка валидации или некорректные данные
   - `401 Unauthorized` - Требуется авторизация (отсутствует или невалидный JWT токен)
   - `404 Not Found` - Ресурс не найден

