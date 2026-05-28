-- Фото врача: только ссылка (UUID файла Directus или полный URL), не BYTEA.
-- Выполнить в clinic_db до/вместе с деплоем бэкенда с новой сущностью Doctor.photo (String).
-- Старые BYTEA-данные в колонке photo не переносятся — при необходимости выгрузите вручную в Directus Files и проставьте UUID.

BEGIN;

ALTER TABLE doctors DROP COLUMN IF EXISTS photo;
ALTER TABLE doctors ADD COLUMN photo TEXT;

COMMENT ON COLUMN doctors.photo IS 'UUID файла в directus_files или абсолютный URL к ассету';

COMMIT;
