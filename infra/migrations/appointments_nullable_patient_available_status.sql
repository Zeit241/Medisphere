-- Миграция: свободные слоты расписания без пациента (онлайн-запись / генерация из ScheduleService).
-- Выполните один раз на существующей БД PostgreSQL, если видите:
--   ERROR: null value in column "patient_id" of relation "appointments" violates not-null constraint

-- 1) Разрешить пустой patient_id у свободных слотов
ALTER TABLE appointments
    ALTER COLUMN patient_id DROP NOT NULL;

-- 2) Добавить статус 'available' в CHECK (имя ограничения в вашей БД может отличаться)
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
ALTER TABLE appointments
    ADD CONSTRAINT appointments_status_check CHECK (
        status IN (
            'available',
            'scheduled',
            'confirmed',
            'in_progress',
            'completed',
            'cancelled',
            'no_show'
        )
    );

-- Если шаг 2 выдал ошибку «constraint does not exist» на DROP — оставьте только ADD
-- или найдите имя: SELECT conname FROM pg_constraint WHERE conrelid = 'appointments'::regclass;
