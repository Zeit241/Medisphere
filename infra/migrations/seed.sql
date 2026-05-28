-- ============================================================
-- Seed-данные для схемы NEW.txt
-- Запускать после применения миграции NEW.txt.
-- Примечание: в NEW.txt индекс idx_reviews_visible ссылается на
-- несуществующую колонку is_visible — при ошибке создания индекса
-- удалите или исправьте эту строку в миграции.
-- ============================================================

BEGIN;

-- 1. Роли
INSERT INTO roles (code, name) VALUES
    ('patient', 'Пациент'),
    ('doctor', 'Врач'),
    ('admin', 'Администратор');

-- 2. Пользователи (роль через role_id)
INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active) VALUES
    ('admin@clinic.local', '+79000000001', '$2b$10$seed_admin_hash', 'Админ', 'Системный', NULL,
     (SELECT id FROM roles WHERE code = 'admin'), true),
    ('doctor.ivanov@clinic.local', '+79000000002', '$2b$10$seed_doc1_hash', 'Иван', 'Иванов', 'Петрович',
     (SELECT id FROM roles WHERE code = 'doctor'), true),
    ('doctor.petrova@clinic.local', '+79000000003', '$2b$10$seed_doc2_hash', 'Мария', 'Петрова', 'Сергеевна',
     (SELECT id FROM roles WHERE code = 'doctor'), true),
    ('patient.sidorov@clinic.local', '+79000000004', '$2b$10$seed_pat1_hash', 'Пётр', 'Сидоров', NULL,
     (SELECT id FROM roles WHERE code = 'patient'), true),
    ('patient.kozlova@clinic.local', '+79000000005', '$2b$10$seed_pat2_hash', 'Елена', 'Козлова', 'Андреевна',
     (SELECT id FROM roles WHERE code = 'patient'), true);

-- 3. Пациенты
INSERT INTO patients (user_id, birth_date, gender, insurance_number)
SELECT u.id, DATE '1985-05-15', 1, 'POLIS-00001'
FROM users u WHERE u.email = 'patient.sidorov@clinic.local';

INSERT INTO patients (user_id, birth_date, gender, insurance_number)
SELECT u.id, DATE '1992-03-20', 2, 'POLIS-00002'
FROM users u WHERE u.email = 'patient.kozlova@clinic.local';

-- 4. Специализации
INSERT INTO specializations (code, name, description) VALUES
    ('cardio', 'Кардиология', 'Заболевания сердечно-сосудистой системы'),
    ('therapist', 'Терапия', 'Общая врачебная практика'),
    ('dent', 'Стоматология', 'Зубы и полость рта');

-- 5. Врачи
INSERT INTO doctors (user_id, hide, bio, experience_years)
SELECT u.id, false, 'Врач-кардиолог, кандидат медицинских наук.', 12
FROM users u WHERE u.email = 'doctor.ivanov@clinic.local';

INSERT INTO doctors (user_id, hide, bio, experience_years)
SELECT u.id, false, 'Врач-терапевт, ведёт приём взрослых.', 8
FROM users u WHERE u.email = 'doctor.petrova@clinic.local';

-- 6. Врач — специализация (PK включает certificate_number)
INSERT INTO doctor_specializations (doctor_id, specialization_id, valid_from, valid_to, certificate_number)
SELECT d.id, s.id, DATE '2018-01-01', DATE '2028-12-31', 'CERT-IVANOV-CARD-001'
FROM doctors d
JOIN users u ON u.id = d.user_id AND u.email = 'doctor.ivanov@clinic.local'
JOIN specializations s ON s.code = 'cardio';

INSERT INTO doctor_specializations (doctor_id, specialization_id, valid_from, valid_to, certificate_number)
SELECT d.id, s.id, DATE '2019-06-01', DATE '2029-12-31', 'CERT-PETROVA-THER-001'
FROM doctors d
JOIN users u ON u.id = d.user_id AND u.email = 'doctor.petrova@clinic.local'
JOIN specializations s ON s.code = 'therapist';

-- 7. Кабинеты
INSERT INTO rooms (code, name) VALUES
    ('101', 'Кабинет кардиологии'),
    ('102', 'Кабинет терапевта'),
    ('203', 'Стоматологический кабинет');

-- 8. Услуги
INSERT INTO services (name, code, price, duration_minutes, description) VALUES
    ('Консультация кардиолога', 'SVC-CARD-CNS', 2500.00, 30, 'Первичный или повторный приём'),
    ('ЭКГ в покое', 'SVC-CARD-EKG', 1200.00, 20, 'Снятие и расшифровка ЭКГ'),
    ('Приём терапевта', 'SVC-THER-CNS', 1800.00, 30, 'Осмотр, рекомендации'),
    ('Профессиональная гигиена', 'SVC-DENT-HYG', 4500.00, 60, 'Чистка и полировка');

-- 9. Диагнозы (МКБ-10)
INSERT INTO diagnoses (code, name, category) VALUES
    ('I10', 'Эссенциальная (первичная) гипертензия', 'IX'),
    ('K29.8', 'Дуоденит неуточнённый', 'XI'),
    ('K02.1', 'Кариес дентина', 'XI');

-- 10. Услуги по специализациям
INSERT INTO specialization_services (specialization_id, service_id, is_active)
SELECT s.id, v.id, true
FROM specializations s
CROSS JOIN services v
WHERE s.code = 'cardio' AND v.code IN ('SVC-CARD-CNS', 'SVC-CARD-EKG');

INSERT INTO specialization_services (specialization_id, service_id, is_active)
SELECT s.id, v.id, true
FROM specializations s
JOIN services v ON v.code = 'SVC-THER-CNS'
WHERE s.code = 'therapist';

INSERT INTO specialization_services (specialization_id, service_id, is_active)
SELECT s.id, v.id, true
FROM specializations s
JOIN services v ON v.code = 'SVC-DENT-HYG'
WHERE s.code = 'dent';

-- 11. Расписание
INSERT INTO doctor_schedules (doctor_id, room_id, dateAt, start_time, end_time, slot_duration_minutes)
SELECT d.id, r.id, CURRENT_DATE + 1, TIME '09:00', TIME '12:00', 30
FROM doctors d
JOIN users u ON u.id = d.user_id AND u.email = 'doctor.ivanov@clinic.local'
CROSS JOIN rooms r
WHERE r.code = '101';

INSERT INTO doctor_schedules (doctor_id, room_id, dateAt, start_time, end_time, slot_duration_minutes)
SELECT d.id, r.id, CURRENT_DATE + 1, TIME '14:00', TIME '17:00', 30
FROM doctors d
JOIN users u ON u.id = d.user_id AND u.email = 'doctor.petrova@clinic.local'
CROSS JOIN rooms r
WHERE r.code = '102';

-- 12. Приёмы
INSERT INTO appointments (
    schedule_id,
    doctor_id,
    patient_id,
    room_id,
    service_id,
    diagnosis_id,
    start_time,
    end_time,
    status,
    source,
    created_by
)
SELECT
    ds.id,
    ds.doctor_id,
    p.id,
    ds.room_id,
    srv.id,
    dx.id,
    ((ds.dateAt + ds.start_time) AT TIME ZONE 'Europe/Moscow'),
    ((ds.dateAt + ds.start_time + make_interval(mins => srv.duration_minutes)) AT TIME ZONE 'Europe/Moscow'),
    'completed',
    'reception',
    (SELECT id FROM users WHERE email = 'admin@clinic.local')
FROM doctor_schedules ds
JOIN doctors doc ON doc.id = ds.doctor_id
JOIN users udoc ON udoc.id = doc.user_id AND udoc.email = 'doctor.ivanov@clinic.local'
JOIN patients p ON p.user_id = (SELECT id FROM users WHERE email = 'patient.sidorov@clinic.local')
JOIN services srv ON srv.code = 'SVC-CARD-CNS'
JOIN diagnoses dx ON dx.code = 'I10'
LIMIT 1;

INSERT INTO appointments (
    schedule_id,
    doctor_id,
    patient_id,
    room_id,
    service_id,
    diagnosis_id,
    start_time,
    end_time,
    status,
    source,
    created_by
)
SELECT
    ds.id,
    ds.doctor_id,
    p.id,
    ds.room_id,
    srv.id,
    dx.id,
    ((ds.dateAt + ds.start_time) AT TIME ZONE 'Europe/Moscow'),
    ((ds.dateAt + ds.start_time + make_interval(mins => srv.duration_minutes)) AT TIME ZONE 'Europe/Moscow'),
    'confirmed',
    'web',
    (SELECT id FROM users WHERE email = 'admin@clinic.local')
FROM doctor_schedules ds
JOIN doctors doc ON doc.id = ds.doctor_id
JOIN users udoc ON udoc.id = doc.user_id AND udoc.email = 'doctor.petrova@clinic.local'
JOIN patients p ON p.user_id = (SELECT id FROM users WHERE email = 'patient.kozlova@clinic.local')
JOIN services srv ON srv.code = 'SVC-THER-CNS'
JOIN diagnoses dx ON dx.code = 'K29.8'
LIMIT 1;

-- 13. Отзыв (один на приём — к завершённому приёму)
INSERT INTO reviews (appointment_id, doctor_id, patient_id, rating, review_text)
SELECT a.id, a.doctor_id, a.patient_id, 5, 'Внимательный врач, всё объяснил понятно.'
FROM appointments a
JOIN services s ON s.id = a.service_id AND s.code = 'SVC-CARD-CNS'
WHERE a.status = 'completed'
LIMIT 1;

COMMIT;
