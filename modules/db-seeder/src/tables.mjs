/** Порядок вставок по внешним ключам (sql migration/NEW.txt) */
export const SEED_ORDER = [
  "roles",
  "users",
  "specializations",
  "rooms",
  "services",
  "diagnoses",
  "patients",
  "doctors",
  "doctor_specializations",
  "specialization_services",
  "doctor_schedules",
  "appointments",
  "reviews",
];

export const TABLE_LABELS = {
  roles: "roles — базовые роли (patient, doctor, admin)",
  users: "users — произвольные пользователи (случайная роль)",
  specializations: "specializations",
  rooms: "rooms",
  services: "services",
  diagnoses: "diagnoses",
  patients: "patients (+ отдельный user с ролью patient)",
  doctors: "doctors (+ user с ролью doctor, photo из images)",
  doctor_specializations: "doctor_specializations",
  specialization_services: "specialization_services",
  doctor_schedules: "doctor_schedules",
  appointments: "appointments",
  reviews: "reviews (только приёмы без отзыва)",
};

export function sortSelected(selectedSet) {
  return SEED_ORDER.filter((k) => selectedSet.has(k));
}

/** Порядок TRUNCATE: сначала зависимые (листья), в конце корни — см. sql migration/NEW.txt */
export const CLEAR_ORDER = [
  "reviews",
  "appointments",
  "doctor_schedules",
  "specialization_services",
  "doctor_specializations",
  "doctors",
  "patients",
  "users",
  "diagnoses",
  "services",
  "rooms",
  "specializations",
  "roles",
];

export const CLEAR_TABLE_LABELS = {
  reviews: "reviews — отзывы на приёмы",
  appointments: "appointments — записи на приём",
  doctor_schedules: "doctor_schedules — расписание врачей",
  specialization_services: "specialization_services — услуги по специализациям",
  doctor_specializations: "doctor_specializations — специализации врачей",
  doctors: "doctors — карточки врачей",
  patients: "patients — карточки пациентов",
  users: "users — учётные записи",
  diagnoses: "diagnoses — диагнозы (МКБ-10)",
  services: "services — услуги",
  rooms: "rooms — кабинеты",
  specializations: "specializations — специализации",
  roles: "roles — роли (patient, doctor, admin)",
};
