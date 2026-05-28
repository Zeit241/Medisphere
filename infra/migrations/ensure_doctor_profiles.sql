-- У пользователя роль doctor, но нет строки в doctors → вход есть, кабинет врача нет.
-- Запуск в psql / DBeaver (идемпотентно, безопасно повторять).

INSERT INTO doctors (user_id, hide, bio, experience_years, photo)
SELECT u.id, false, 'Профиль врача', 0, NULL
FROM users u
INNER JOIN roles r ON r.id = u.role_id AND lower(r.code) = 'doctor'
WHERE NOT EXISTS (SELECT 1 FROM doctors d WHERE d.user_id = u.id);
