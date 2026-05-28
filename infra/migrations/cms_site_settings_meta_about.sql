-- Расширение singleton cms_site_settings: SEO-описания и тексты «О нас» (редактируются в Directus).
--   psql -U login -d clinic_db -f "sql migration/cms_site_settings_meta_about.sql"

ALTER TABLE cms_site_settings
  ADD COLUMN IF NOT EXISTS meta_description_doctors TEXT,
  ADD COLUMN IF NOT EXISTS meta_description_services TEXT,
  ADD COLUMN IF NOT EXISTS meta_description_about TEXT,
  ADD COLUMN IF NOT EXISTS about_mission_rich TEXT,
  ADD COLUMN IF NOT EXISTS about_vision_rich TEXT,
  ADD COLUMN IF NOT EXISTS nav_label_about TEXT,
  ADD COLUMN IF NOT EXISTS nav_label_services TEXT,
  ADD COLUMN IF NOT EXISTS nav_label_doctors TEXT;

COMMENT ON COLUMN cms_site_settings.meta_description_doctors IS 'Meta description для страницы /doctors';
COMMENT ON COLUMN cms_site_settings.meta_description_services IS 'Meta description для страницы /services';
COMMENT ON COLUMN cms_site_settings.meta_description_about IS 'Meta description для страницы /about';
COMMENT ON COLUMN cms_site_settings.about_mission_rich IS 'Текст блока «Миссия» на /about';
COMMENT ON COLUMN cms_site_settings.about_vision_rich IS 'Текст блока «Видение» на /about';
COMMENT ON COLUMN cms_site_settings.nav_label_about IS 'Подпись пункта меню «О клинике»';
COMMENT ON COLUMN cms_site_settings.nav_label_services IS 'Подпись пункта меню «Услуги»';
COMMENT ON COLUMN cms_site_settings.nav_label_doctors IS 'Подпись пункта меню «Врачи»';
