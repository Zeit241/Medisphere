-- Главная страница лендинга: тексты/фото из cms_site_settings (singleton id=1).
--   psql -U login -d clinic_db -f "sql migration/cms_site_settings_home.sql"

ALTER TABLE cms_site_settings
  ADD COLUMN IF NOT EXISTS hero_years_experience_label TEXT,
  ADD COLUMN IF NOT EXISTS section_1_photo TEXT,
  ADD COLUMN IF NOT EXISTS section_2_photo TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_1_title TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_1_desc TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_2_title TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_2_desc TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_3_title TEXT,
  ADD COLUMN IF NOT EXISTS home_feature_3_desc TEXT,
  ADD COLUMN IF NOT EXISTS home_stat_1_value TEXT,
  ADD COLUMN IF NOT EXISTS home_stat_1_label TEXT,
  ADD COLUMN IF NOT EXISTS home_stat_2_value TEXT,
  ADD COLUMN IF NOT EXISTS home_stat_2_label TEXT;

COMMENT ON COLUMN cms_site_settings.hero_years_experience_label IS 'Главная: подпись «лет опыта» (напр. 15+)';
COMMENT ON COLUMN cms_site_settings.section_1_photo IS 'UUID файла Directus — большое фото в hero';
COMMENT ON COLUMN cms_site_settings.section_2_photo IS 'UUID файла Directus — фото блока услуг';
COMMENT ON COLUMN cms_site_settings.home_feature_1_title IS 'Главная: блок преимуществ 1 — заголовок';
