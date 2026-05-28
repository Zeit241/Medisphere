-- Directus плохо открывает обычный Content-раздел для таблиц со составным PRIMARY KEY.
-- Добавляем суррогатный id, а старые бизнес-ключи сохраняем UNIQUE-ограничениями.
-- Также выравниваем doctor_specializations с JPA-сущностью DoctorSpecialization (id + doctor/specialization).

BEGIN;

-- ---------------------------------------------------------------------------
-- doctor_specializations: Java entity has @Id id, Directus needs a single PK.
-- ---------------------------------------------------------------------------
ALTER TABLE doctor_specializations ADD COLUMN IF NOT EXISTS id BIGINT;

CREATE SEQUENCE IF NOT EXISTS doctor_specializations_id_seq;
ALTER SEQUENCE doctor_specializations_id_seq OWNED BY doctor_specializations.id;

UPDATE doctor_specializations
SET id = nextval('doctor_specializations_id_seq')
WHERE id IS NULL;

SELECT setval(
  'doctor_specializations_id_seq',
  GREATEST((SELECT COALESCE(MAX(id), 0) FROM doctor_specializations), 1),
  true
);

ALTER TABLE doctor_specializations
  ALTER COLUMN id SET DEFAULT nextval('doctor_specializations_id_seq'),
  ALTER COLUMN id SET NOT NULL;

WITH ranked AS (
  SELECT
    ctid,
    ROW_NUMBER() OVER (
      PARTITION BY doctor_id, specialization_id
      ORDER BY id
    ) AS rn
  FROM doctor_specializations
)
DELETE FROM doctor_specializations ds
USING ranked r
WHERE ds.ctid = r.ctid
  AND r.rn > 1;

ALTER TABLE doctor_specializations DROP CONSTRAINT IF EXISTS doctor_specializations_pkey;

ALTER TABLE doctor_specializations
  ALTER COLUMN valid_to DROP NOT NULL,
  ALTER COLUMN certificate_number DROP NOT NULL;

ALTER TABLE doctor_specializations
  ADD CONSTRAINT doctor_specializations_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_doctor_specializations_doctor_specialization
  ON doctor_specializations (doctor_id, specialization_id);

-- ---------------------------------------------------------------------------
-- specialization_services: Spring still uses EmbeddedId, but Directus gets id.
-- ---------------------------------------------------------------------------
ALTER TABLE specialization_services ADD COLUMN IF NOT EXISTS id BIGINT;

CREATE SEQUENCE IF NOT EXISTS specialization_services_id_seq;
ALTER SEQUENCE specialization_services_id_seq OWNED BY specialization_services.id;

UPDATE specialization_services
SET id = nextval('specialization_services_id_seq')
WHERE id IS NULL;

SELECT setval(
  'specialization_services_id_seq',
  GREATEST((SELECT COALESCE(MAX(id), 0) FROM specialization_services), 1),
  true
);

ALTER TABLE specialization_services
  ALTER COLUMN id SET DEFAULT nextval('specialization_services_id_seq'),
  ALTER COLUMN id SET NOT NULL;

ALTER TABLE specialization_services DROP CONSTRAINT IF EXISTS specialization_services_pkey;

ALTER TABLE specialization_services
  ADD CONSTRAINT specialization_services_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_specialization_services_specialization_service
  ON specialization_services (specialization_id, service_id);

COMMIT;
