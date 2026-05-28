-- =============================
-- 1. Пользователи
-- =============================
CREATE TABLE
    users (
        id SERIAL PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        phone TEXT UNIQUE,
        password_hash TEXT,
        first_name TEXT,
        last_name TEXT,
        middle_name TEXT,
        created_at timestamptz NOT NULL DEFAULT now (),
        updated_at timestamptz NOT NULL DEFAULT now (),
        is_active BOOLEAN NOT NULL DEFAULT true
    );

-- =============================
-- 2. Роли и их связи
-- =============================
CREATE TABLE
    roles (
        id SERIAL PRIMARY KEY,
        code TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL
    );

CREATE TABLE
    user_roles (
        user_id INTEGER REFERENCES users (id) ON DELETE CASCADE,
        role_id INTEGER REFERENCES roles (id) ON DELETE CASCADE,
        PRIMARY KEY (user_id, role_id)
    );

-- =============================
-- 3. Пациенты
-- =============================
CREATE TABLE
    patients (
        id SERIAL PRIMARY KEY,
        user_id INTEGER UNIQUE REFERENCES users (id) ON DELETE CASCADE,
        birth_date DATE,
        gender SMALLINT, -- 1 = муж, 2 = жен
        insurance_number TEXT,
        created_at timestamptz NOT NULL DEFAULT now (),
        updated_at timestamptz NOT NULL DEFAULT now ()
    );

-- =============================
-- 4. Врачи
-- =============================
CREATE TABLE
    doctors (
        id SERIAL PRIMARY KEY,
        user_id INTEGER UNIQUE REFERENCES users (id),
        display_name TEXT NOT NULL,
        bio TEXT,
        experience_years INTEGER CHECK (experience_years >= 0),
        photo TEXT,
        created_at timestamptz NOT NULL DEFAULT now (),
        updated_at timestamptz NOT NULL DEFAULT now ()
    );

-- =============================
-- 5. Специализации
-- =============================
CREATE TABLE
    specializations (
        id SERIAL PRIMARY KEY,
        code TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        description TEXT
    );

CREATE TABLE
    doctor_specializations (
        doctor_id INTEGER REFERENCES doctors (id) ON DELETE CASCADE,
        specialization_id INTEGER REFERENCES specializations (id) ON DELETE CASCADE,
        PRIMARY KEY (doctor_id, specialization_id)
    );

-- =============================
-- 6. Кабинеты
-- =============================
CREATE TABLE
    rooms (
        id SERIAL PRIMARY KEY,
        code TEXT UNIQUE NOT NULL,
        name TEXT
    );

-- =============================
-- 7. Расписание приёма
-- =============================
CREATE TABLE
    doctor_schedules (
                         id SERIAL PRIMARY KEY,
                         doctor_id INTEGER REFERENCES doctors (id) NOT NULL,
                         room_id INTEGER REFERENCES rooms (id),
                         dateAt DATE,
                         start_time TIME NOT NULL,
                         end_time TIME NOT NULL CHECK (end_time > start_time),
                         slot_duration_minutes INTEGER NOT NULL CHECK (slot_duration_minutes > 0),
                         created_at timestamptz NOT NULL DEFAULT now (),
                         updated_at timestamptz NOT NULL DEFAULT now (),
                         UNIQUE (doctor_id, dateAt, start_time, end_time)
);


-- =============================
-- 8. Приёмы (записи)
-- =============================
CREATE TABLE
    appointments (
        id SERIAL PRIMARY KEY,
        schedule_id INTEGER REFERENCES doctor_schedules (id) ON DELETE SET NULL,
        doctor_id INTEGER REFERENCES doctors (id) NOT NULL,
        patient_id INTEGER REFERENCES patients (id),
        room_id INTEGER REFERENCES rooms (id),
        start_time timestamptz NOT NULL,
        end_time timestamptz NOT NULL,
        status TEXT NOT NULL CHECK (
            status IN (
                'available',
                'scheduled',
                'confirmed',
                'in_progress',
                'completed',
                'cancelled',
                'no_show'
            )
        ),
        source TEXT NOT NULL CHECK (
            source IN ('mobile', 'reception', 'web', 'admin')
        ),
        created_by INTEGER REFERENCES users (id),
        created_at timestamptz NOT NULL DEFAULT now (),
        updated_at timestamptz NOT NULL DEFAULT now (),
        cancel_reason TEXT
    );

CREATE INDEX idx_appointments_doctor_time ON appointments (doctor_id, start_time);

CREATE INDEX idx_appointments_patient ON appointments (patient_id);

-- =============================
-- 9. Электронная очередь
-- =============================
CREATE TABLE
    queue_entries (
        id SERIAL PRIMARY KEY,
        doctor_id INTEGER REFERENCES doctors (id) NOT NULL,
        appointment_id INTEGER UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
        patient_id INTEGER REFERENCES patients (id) NOT NULL,
        position INTEGER NOT NULL CHECK (position >= 0),
        last_updated timestamptz NOT NULL DEFAULT now (),
        UNIQUE (doctor_id, position)
    );

CREATE INDEX idx_queue_doctor_position ON queue_entries (doctor_id, position);

-- =============================
-- 10. Отзывы и оценки (объединённая таблица)
-- =============================
CREATE TABLE
    reviews (
        id SERIAL PRIMARY KEY,
        appointment_id INTEGER UNIQUE REFERENCES appointments (id) NOT NULL,
        doctor_id INTEGER REFERENCES doctors (id) NOT NULL,
        patient_id INTEGER REFERENCES patients (id) NOT NULL,
        rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
        review_text TEXT,
        created_at timestamptz NOT NULL DEFAULT now ()
    );

CREATE INDEX idx_reviews_doctor ON reviews (doctor_id);

-- =============================
-- 11. Уведомления
-- =============================
CREATE TABLE
    notifications (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users (id) NOT NULL,
        appointment_id INTEGER REFERENCES appointments (id),
        type TEXT NOT NULL,
        payload JSONB,
        sent_at timestamptz,
        status TEXT CHECK (status IN ('pending', 'sent', 'failed')) DEFAULT 'pending'
    );

CREATE INDEX idx_notifications_user ON notifications (user_id);
