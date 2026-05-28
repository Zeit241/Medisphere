-- Настройки лендинга: одна строка id=1. Остальная CMS-логика — в операционных таблицах
-- (doctors, services, reviews, …) и в Directus; отдельных cms_*-карточек и представлений v_site_* нет.
--
-- Для БД, куда раньше накатывали «толстый» cms_overlay: скрипт сначала снимает триггеры,
-- представления и старые таблицы, затем гарантирует cms_site_settings.
-- Для чистой БД блоки DROP — no-op (IF EXISTS).
--
--   psql -U login -d clinic_db -f "sql migration/cms_overlay.sql"

BEGIN;

-- ---------------------------------------------------------------------------
-- Снять устаревший слой (views → triggers → functions → tables)
-- ---------------------------------------------------------------------------
DROP VIEW IF EXISTS v_site_doctors CASCADE;
DROP VIEW IF EXISTS v_site_services CASCADE;
DROP VIEW IF EXISTS v_site_reviews CASCADE;

DO $$
BEGIN
  IF to_regclass('public.doctors') IS NOT NULL THEN
    DROP TRIGGER IF EXISTS trg_cms_doctor_after_insert ON doctors;
  END IF;
  IF to_regclass('public.services') IS NOT NULL THEN
    DROP TRIGGER IF EXISTS trg_cms_service_after_insert ON services;
  END IF;
END
$$;

DROP FUNCTION IF EXISTS cms_trg_ensure_doctor_profile();
DROP FUNCTION IF EXISTS cms_trg_ensure_service_card();

DROP TABLE IF EXISTS cms_gallery_item;
DROP TABLE IF EXISTS cms_review_site;
DROP TABLE IF EXISTS cms_service_card;
DROP TABLE IF EXISTS cms_doctor_profile;

-- ---------------------------------------------------------------------------
-- Только настройки сайта (строка с id = 1)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cms_site_settings (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  clinic_name TEXT,
  phone TEXT,
  email TEXT,
  address TEXT,
  booking_url TEXT,
  hero_title TEXT,
  hero_subtitle TEXT,
  hero_badge TEXT,
  social_links JSONB DEFAULT '{}'::jsonb,
  meta_description_doctors TEXT,
  meta_description_services TEXT,
  meta_description_about TEXT,
  about_mission_rich TEXT,
  about_vision_rich TEXT,
  nav_label_about TEXT,
  nav_label_services TEXT,
  nav_label_doctors TEXT,
  hero_years_experience_label TEXT,
  section_1_photo TEXT,
  section_2_photo TEXT,
  home_feature_1_title TEXT,
  home_feature_1_desc TEXT,
  home_feature_2_title TEXT,
  home_feature_2_desc TEXT,
  home_feature_3_title TEXT,
  home_feature_3_desc TEXT,
  home_stat_1_value TEXT,
  home_stat_1_label TEXT,
  home_stat_2_value TEXT,
  home_stat_2_label TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO cms_site_settings (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE cms_site_settings IS 'Тексты/контакты лендинга; правки через Directus (одна запись id=1)';

COMMIT;
