-- SQL запрос для проверки appointments пользователя
-- Замените 'admin3@example.com' на нужный email

SELECT 
    u.email,
    a.id as appointment_id,
    a.start_time,
    a.end_time,
    a.status,
    a.patient_id,
    trim(concat_ws(' ', du.last_name, du.first_name, du.middle_name)) as doctor_name,
    CASE 
        WHEN a.start_time < NOW() - INTERVAL '20 minutes' THEN 'ПРОШЛО (более 20 мин)'
        WHEN a.start_time < NOW() THEN 'ПРОШЛО (менее 20 мин)'
        ELSE 'БУДУЩЕЕ'
    END as time_status,
    CASE 
        WHEN a.status IN ('completed', 'cancelled') THEN 'ИСКЛЮЧЕНО'
        ELSE 'АКТИВНО'
    END as status_check
FROM appointments a
LEFT JOIN patients p ON a.patient_id = p.id
LEFT JOIN users u ON p.user_id = u.id
LEFT JOIN doctors d ON a.doctor_id = d.id
LEFT JOIN users du ON d.user_id = du.id
WHERE u.email = 'admin3@example.com'  -- ЗАМЕНИТЕ НА НУЖНЫЙ EMAIL
ORDER BY a.start_time;

-- Проверка всех appointments на сегодня
SELECT 
    u.email,
    COUNT(*) as total_appointments,
    COUNT(CASE WHEN a.start_time >= NOW() - INTERVAL '20 minutes' THEN 1 END) as future_appointments,
    COUNT(CASE WHEN a.status NOT IN ('completed', 'cancelled') THEN 1 END) as active_appointments
FROM appointments a
LEFT JOIN patients p ON a.patient_id = p.id
LEFT JOIN users u ON p.user_id = u.id
WHERE u.email IN ('admin3@example.com', 'user@example.com', 'patient@example.com', 'test@example.com')
GROUP BY u.email;

