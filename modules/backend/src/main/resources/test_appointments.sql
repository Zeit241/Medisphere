-- Скрипт для добавления тестовых записей на сегодня для тестирования WebSocket очереди
-- Выполнить после запуска приложения

-- Получаем текущую дату и время
DO $$
DECLARE
    today_date DATE := CURRENT_DATE;
    base_time TIMESTAMPTZ;
    -- Времена для записей (в UTC)
    time1 TIMESTAMPTZ;
    time2 TIMESTAMPTZ;
    time3 TIMESTAMPTZ;
    time4 TIMESTAMPTZ;
    time5 TIMESTAMPTZ;
    time6 TIMESTAMPTZ;
    
    -- ID пользователей и пациентов
    user1_id INTEGER;
    user2_id INTEGER;
    user3_id INTEGER;
    user4_id INTEGER;
    patient1_id INTEGER;
    patient2_id INTEGER;
    patient3_id INTEGER;
    patient4_id INTEGER;
    
    -- ID врачей (берем первых двух врачей)
    doctor1_id INTEGER;
    doctor2_id INTEGER;
    
    -- ID кабинета (берем первый кабинет или создаем)
    room_id INTEGER;
BEGIN
    -- Получаем ID пользователей
    SELECT id INTO user1_id FROM users WHERE email = 'admin3@example.com';
    SELECT id INTO user2_id FROM users WHERE email = 'user@example.com';
    SELECT id INTO user3_id FROM users WHERE email = 'patient@example.com';
    SELECT id INTO user4_id FROM users WHERE email = 'test@example.com';
    
    -- Если пользователей нет, создаем их
    -- Пароль для всех тестовых пользователей: password123
    -- BCrypt хеш для "password123" (strength 12)
    IF user1_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, is_active)
        VALUES ('admin3@example.com', '+79991234567', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Админ', 'Третий', 'Тестовый', true)
        RETURNING id INTO user1_id;
    END IF;
    
    IF user2_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, is_active)
        VALUES ('user@example.com', '+79991234568', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Пользователь', 'Тестовый', 'Обычный', true)
        RETURNING id INTO user2_id;
    END IF;
    
    IF user3_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, is_active)
        VALUES ('patient@example.com', '+79991234569', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Пациент', 'Тестовый', 'ДляОчереди', true)
        RETURNING id INTO user3_id;
    END IF;
    
    IF user4_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, is_active)
        VALUES ('test@example.com', '+79991234570', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Тест', 'Пользователь', 'Четвертый', true)
        RETURNING id INTO user4_id;
    END IF;
    
    -- Создаем пациентов, если их нет
    SELECT id INTO patient1_id FROM patients WHERE user_id = user1_id;
    IF patient1_id IS NULL THEN
        INSERT INTO patients (user_id, birth_date, gender, insurance_number)
        VALUES (user1_id, '1990-01-15', 1, '123-456-789-01')
        RETURNING id INTO patient1_id;
    END IF;
    
    SELECT id INTO patient2_id FROM patients WHERE user_id = user2_id;
    IF patient2_id IS NULL THEN
        INSERT INTO patients (user_id, birth_date, gender, insurance_number)
        VALUES (user2_id, '1985-05-20', 1, '123-456-789-02')
        RETURNING id INTO patient2_id;
    END IF;
    
    SELECT id INTO patient3_id FROM patients WHERE user_id = user3_id;
    IF patient3_id IS NULL THEN
        INSERT INTO patients (user_id, birth_date, gender, insurance_number)
        VALUES (user3_id, '1992-08-10', 2, '123-456-789-03')
        RETURNING id INTO patient3_id;
    END IF;
    
    SELECT id INTO patient4_id FROM patients WHERE user_id = user4_id;
    IF patient4_id IS NULL THEN
        INSERT INTO patients (user_id, birth_date, gender, insurance_number)
        VALUES (user4_id, '1988-12-25', 1, '123-456-789-04')
        RETURNING id INTO patient4_id;
    END IF;
    
    -- Получаем ID врачей (берем первых двух)
    SELECT id INTO doctor1_id FROM doctors LIMIT 1;
    SELECT id INTO doctor2_id FROM doctors OFFSET 1 LIMIT 1;
    
    -- Если врачей нет, создаем тестовых (создаем пользователей для врачей)
    -- Пароль для врачей: password123
    IF doctor1_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, is_active)
        VALUES ('doctor1@example.com', '+79991234571', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Доктор', 'Первый', true)
        ON CONFLICT (email) DO NOTHING;
        
        INSERT INTO doctors (user_id, bio, experience_years)
        VALUES (
            (SELECT id FROM users WHERE email = 'doctor1@example.com'),
            'Тестовый врач для очереди',
            10
        )
        RETURNING id INTO doctor1_id;
    END IF;
    
    IF doctor2_id IS NULL THEN
        INSERT INTO users (email, phone, password_hash, first_name, last_name, is_active)
        VALUES ('doctor2@example.com', '+79991234572', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Доктор', 'Второй', true)
        ON CONFLICT (email) DO NOTHING;
        
        INSERT INTO doctors (user_id, bio, experience_years)
        VALUES (
            (SELECT id FROM users WHERE email = 'doctor2@example.com'),
            'Тестовый врач для очереди',
            15
        )
        RETURNING id INTO doctor2_id;
    END IF;
    
    -- Получаем ID кабинета (берем первый или создаем)
    SELECT id INTO room_id FROM rooms LIMIT 1;
    IF room_id IS NULL THEN
        INSERT INTO rooms (code, name)
        VALUES ('ROOM-001', 'Кабинет 1')
        RETURNING id INTO room_id;
    END IF;
    
    -- Получаем базовое время (текущее время, округленное до часа + 1 час)
    base_time := (DATE_TRUNC('hour', NOW()) + INTERVAL '1 hour')::TIMESTAMPTZ;
    
    -- Создаем времена для записей на сегодня
    -- Используем фиксированные времена для удобства тестирования
    -- Время 1: через 1 час от текущего времени
    time1 := base_time;
    -- Время 2: через 2 часа
    time2 := (base_time + INTERVAL '1 hour')::TIMESTAMPTZ;
    -- Время 3: через 3 часа
    time3 := (base_time + INTERVAL '2 hours')::TIMESTAMPTZ;
    -- Время 4: через 4 часа
    time4 := (base_time + INTERVAL '3 hours')::TIMESTAMPTZ;
    -- Время 5: через 5 часов
    time5 := (base_time + INTERVAL '4 hours')::TIMESTAMPTZ;
    -- Время 6: через 6 часов
    time6 := (base_time + INTERVAL '5 hours')::TIMESTAMPTZ;
    
    -- Удаляем старые записи на сегодня для этих пациентов (если есть)
    DELETE FROM appointments 
    WHERE patient_id IN (patient1_id, patient2_id, patient3_id, patient4_id)
    AND DATE(start_time) = today_date;
    
    -- Создаем записи для admin3@example.com (patient1_id)
    INSERT INTO appointments (doctor_id, patient_id, room_id, start_time, end_time, status, source, created_at, updated_at)
    VALUES 
        (doctor1_id, patient1_id, room_id, time1, time1 + (INTERVAL '30 minutes'), 'scheduled', 'mobile', NOW(), NOW()),
        (doctor1_id, patient1_id, room_id, time3, time3 + (INTERVAL '30 minutes'), 'confirmed', 'mobile', NOW(), NOW()),
        (doctor2_id, patient1_id, room_id, time5, time5 + (INTERVAL '30 minutes'), 'scheduled', 'web', NOW(), NOW());
    
    -- Создаем записи для user@example.com (patient2_id)
    INSERT INTO appointments (doctor_id, patient_id, room_id, start_time, end_time, status, source, created_at, updated_at)
    VALUES 
        (doctor1_id, patient2_id, room_id, time2, time2 + (INTERVAL '30 minutes'), 'scheduled', 'mobile', NOW(), NOW()),
        (doctor2_id, patient2_id, room_id, time4, time4 + (INTERVAL '30 minutes'), 'confirmed', 'web', NOW(), NOW());
    
    -- Создаем записи для patient@example.com (patient3_id)
    INSERT INTO appointments (doctor_id, patient_id, room_id, start_time, end_time, status, source, created_at, updated_at)
    VALUES 
        (doctor1_id, patient3_id, room_id, time1 + (INTERVAL '15 minutes'), time1 + (INTERVAL '45 minutes'), 'scheduled', 'mobile', NOW(), NOW()),
        (doctor2_id, patient3_id, room_id, time6, time6 + (INTERVAL '30 minutes'), 'scheduled', 'web', NOW(), NOW());
    
    -- Создаем записи для test@example.com (patient4_id)
    INSERT INTO appointments (doctor_id, patient_id, room_id, start_time, end_time, status, source, created_at, updated_at)
    VALUES 
        (doctor1_id, patient4_id, room_id, time2 + (INTERVAL '15 minutes'), time2 + (INTERVAL '45 minutes'), 'scheduled', 'mobile', NOW(), NOW());
    
    RAISE NOTICE 'Тестовые записи успешно созданы на дату: %', today_date;
    RAISE NOTICE 'Создано записей для admin3@example.com: 3';
    RAISE NOTICE 'Создано записей для user@example.com: 2';
    RAISE NOTICE 'Создано записей для patient@example.com: 2';
    RAISE NOTICE 'Создано записей для test@example.com: 1';
END $$;

