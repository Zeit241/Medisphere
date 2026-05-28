import { randomBytes } from "node:crypto";
import { readImageBufferAt } from "./images.mjs";
import {
  createShuffledPool,
  pickAt,
  SPECIALIZATION_TEMPLATES,
  SERVICE_TEMPLATES,
  DIAGNOSIS_TEMPLATES,
  RU_TEXT,
  ROOM_PURPOSES,
  specializationNameForKey,
  DEMO_PSYCHIATRY_SPEC_KEY,
} from "./ruClinicalData.mjs";

/** Первый id по имени (при дубликатах имён в БД). */
function firstIdByName(rows) {
  const m = new Map();
  for (const row of rows) {
    if (!m.has(row.name)) {
      m.set(row.name, row.id);
    }
  }
  return m;
}

/** Хеш bcrypt для демо-входа; пароль — тот, для которого сгенерирован этот хеш на стороне клиента. */
const DEMO_PASSWORD_HASH =
  "$2a$12$fCLZhQ67hHzppGqVg74jUuEKEW0wrhbsuNMWhVd/idr8kTW0toTK.";

const APPOINTMENT_STATUSES = [
  "scheduled",
  "confirmed",
  "in_progress",
  "completed",
  "cancelled",
  "no_show",
];
const APPOINTMENT_SOURCES = ["mobile", "reception", "web", "admin"];

async function roleIdByCode(client, code) {
  const r = await client.query("SELECT id FROM roles WHERE code = $1", [code]);
  return r.rows[0]?.id ?? null;
}

async function requireMinCount(client, table, min, message) {
  const r = await client.query(`SELECT COUNT(*)::int AS c FROM ${table}`);
  const c = r.rows[0]?.c ?? 0;
  if (c < min) {
    throw new Error(message);
  }
}

async function randomRowId(client, sql, params = []) {
  const r = await client.query(sql, params);
  return r.rows[0]?.id ?? null;
}

function uniqueRunPrefix() {
  return randomBytes(4).toString("hex");
}

let doctorsPhotoDataTypeCache;

/** @param {import('pg').Client | import('pg').PoolClient} client */
async function getDoctorsPhotoDataType(client) {
  if (doctorsPhotoDataTypeCache) {
    return doctorsPhotoDataTypeCache;
  }
  const r = await client.query(
    `SELECT data_type::text AS dt
     FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'doctors' AND column_name = 'photo'`,
  );
  doctorsPhotoDataTypeCache = r.rows[0]?.dt ?? "text";
  return doctorsPhotoDataTypeCache;
}

/** Значение DATE из pg → YYYY-MM-DD */
function pgDateToYmd(v) {
  if (v == null) return null;
  if (v instanceof Date) {
    const t = v.getTime();
    if (!Number.isFinite(t)) return null;
    return v.toISOString().slice(0, 10);
  }
  const s = String(v).trim();
  const iso = s.match(/^(\d{4}-\d{2}-\d{2})/);
  if (iso) return iso[1];
  const d = new Date(s);
  return Number.isFinite(d.getTime()) ? d.toISOString().slice(0, 10) : null;
}

/** Значение TIME из pg → HH:mm:ss */
function pgTimeToHms(v) {
  if (v == null) return null;
  const s = String(v).trim();
  const m = s.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/);
  if (!m) return null;
  const hh = m[1].padStart(2, "0");
  const mm = m[2];
  const ss = (m[3] ?? "00").padStart(2, "0");
  return `${hh}:${mm}:${ss}`;
}

function isValidDate(d) {
  return d instanceof Date && Number.isFinite(d.getTime());
}

function pickDurationMinutes(servicesRows, faker, fallback = 30) {
  if (!servicesRows.length) return fallback;
  const raw = faker.helpers.arrayElement(servicesRows).duration_minutes;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

/** Идемпотентно: patient, doctor, admin */
export async function seedRoles(client) {
  const rows = [
    ["patient", "Пациент"],
    ["doctor", "Врач"],
    ["admin", "Администратор"],
  ];
  let inserted = 0;
  for (const [code, name] of rows) {
    const r = await client.query(
      `INSERT INTO roles (code, name) VALUES ($1, $2)
       ON CONFLICT (code) DO NOTHING
       RETURNING id`,
      [code, name],
    );
    if (r.rowCount) inserted += r.rowCount;
  }
  return { inserted, total: rows.length };
}

export async function seedUsers(client, faker, count) {
  await requireMinCount(
    client,
    "roles",
    1,
    "Нужна хотя бы одна роль в roles. Сначала выберите roles.",
  );
  const roles = await client.query("SELECT id, code FROM roles");
  if (!roles.rows.length) {
    throw new Error("Таблица roles пуста.");
  }
  const pickRoleId = () => {
    const w = roles.rows;
    const r = faker.helpers.arrayElement(w);
    return r.id;
  };
  const run = uniqueRunPrefix();
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const email = `u_${run}_${i}_${faker.string.alphanumeric(6)}@seed.test`;
    const phone = `+79${faker.string.numeric(9)}${(i % 10).toString()}`;
    const hash = `$2b$10$${faker.string.alphanumeric(53)}`;
    const first = faker.person.firstName();
    const last = faker.person.lastName();
    const middle = faker.datatype.boolean() ? faker.person.middleName() : null;
    const role_id = pickRoleId();
    const r = await client.query(
      `INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, true)`,
      [email, phone, hash, first, last, middle, role_id],
    );
    inserted += r.rowCount ?? 1;
  }
  return { inserted };
}

export async function seedSpecializations(client, faker, count) {
  const run = uniqueRunPrefix();
  const shuffled = createShuffledPool(faker, SPECIALIZATION_TEMPLATES);
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const code = `spec_${run}_${i}_${faker.string.alphanumeric(4)}`.slice(0, 80);
    const t = pickAt(shuffled, i);
    const name = t.name;
    const description = t.description;
    try {
      const r = await client.query(
        `INSERT INTO specializations (code, name, description) VALUES ($1, $2, $3)`,
        [code, name, description],
      );
      inserted += r.rowCount ?? 1;
    } catch (e) {
      if (e.code === "23505") {
        i--;
        continue;
      }
      throw e;
    }
  }
  return { inserted };
}

export async function seedRooms(client, faker, count) {
  const run = uniqueRunPrefix();
  const purposes = createShuffledPool(faker, ROOM_PURPOSES);
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const code = `room_${run}_${i}`;
    const name = `Кабинет №${101 + i} — ${pickAt(purposes, i)}`;
    try {
      const r = await client.query(
        `INSERT INTO rooms (code, name) VALUES ($1, $2)`,
        [code, name],
      );
      inserted += r.rowCount ?? 1;
    } catch (e) {
      if (e.code === "23505") {
        i--;
        continue;
      }
      throw e;
    }
  }
  return { inserted };
}

export async function seedServices(client, faker, count) {
  const run = uniqueRunPrefix();
  const shuffled = createShuffledPool(faker, SERVICE_TEMPLATES);
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const code = `svc_${run}_${i}_${faker.string.alphanumeric(5)}`;
    const t = pickAt(shuffled, i);
    const name = t.name;
    const price = faker.number.float({
      min: t.priceMin,
      max: t.priceMax,
      fractionDigits: 2,
    });
    const duration_minutes = t.duration_minutes;
    const description = t.description;
    try {
      const r = await client.query(
        `INSERT INTO services (name, code, price, duration_minutes, description)
         VALUES ($1, $2, $3, $4, $5)`,
        [name, code, price, duration_minutes, description],
      );
      inserted += r.rowCount ?? 1;
    } catch (e) {
      if (e.code === "23505") {
        i--;
        continue;
      }
      throw e;
    }
  }
  return { inserted };
}

export async function seedDiagnoses(client, faker, count) {
  const run = uniqueRunPrefix();
  const shuffled = createShuffledPool(faker, DIAGNOSIS_TEMPLATES);
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const t = pickAt(shuffled, i);
    let code = `${t.code}.${run.slice(0, 4)}.${i}`;
    if (code.length > 32) {
      code = code.slice(0, 32);
    }
    const name = t.name;
    const category = t.category;
    try {
      const r = await client.query(
        `INSERT INTO diagnoses (code, name, category) VALUES ($1, $2, $3)`,
        [code, name, category],
      );
      inserted += r.rowCount ?? 1;
    } catch (e) {
      if (e.code === "23505") {
        i--;
        continue;
      }
      throw e;
    }
  }
  return { inserted };
}

export async function seedPatients(client, faker, count) {
  const rid = await roleIdByCode(client, "patient");
  if (!rid) {
    throw new Error("В roles нет кода patient. Сначала выполните seed roles.");
  }
  const run = uniqueRunPrefix();
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const email = `p_${run}_${i}_${faker.string.alphanumeric(5)}@seed.test`;
    const phone = `+78${faker.string.numeric(9)}${(i % 10).toString()}`;
    const hash = `$2b$10$${faker.string.alphanumeric(53)}`;
    const first = faker.person.firstName();
    const last = faker.person.lastName();
    const middle = faker.datatype.boolean() ? faker.person.middleName() : null;
    const u = await client.query(
      `INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, true) RETURNING id`,
      [email, phone, hash, first, last, middle, rid],
    );
    const userId = u.rows[0].id;
    const birth_date = faker.date.birthdate({ min: 18, max: 85, mode: "age" });
    const gender = faker.helpers.arrayElement([1, 2]);
    const insurance_number = `INS-${run}-${i}`;
    await client.query(
      `INSERT INTO patients (user_id, birth_date, gender, insurance_number) VALUES ($1, $2, $3, $4)`,
      [userId, birth_date.toISOString().slice(0, 10), gender, insurance_number],
    );
    inserted += 1;
  }
  return { inserted };
}

export async function seedDoctors(client, faker, count, imagePaths) {
  const rid = await roleIdByCode(client, "doctor");
  if (!rid) {
    throw new Error("В roles нет кода doctor. Сначала выполните seed roles.");
  }
  const run = uniqueRunPrefix();
  const photoColType = await getDoctorsPhotoDataType(client);
  const useByteaPhoto = photoColType === "bytea";
  if (!useByteaPhoto && imagePaths?.length > 0) {
    console.warn(
      "doctors.photo в БД не bytea (часто TEXT для Directus/URL) — бинарные файлы из каталога изображений не пишем; photo = NULL.",
    );
  }
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const email = `d_${run}_${i}_${faker.string.alphanumeric(5)}@seed.test`;
    const phone = `+77${faker.string.numeric(9)}${(i % 10).toString()}`;
    const hash = `$2b$10$${faker.string.alphanumeric(53)}`;
    const first = faker.person.firstName();
    const last = faker.person.lastName();
    const middle = faker.datatype.boolean() ? faker.person.middleName() : null;
    const u = await client.query(
      `INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, true) RETURNING id`,
      [email, phone, hash, first, last, middle, rid],
    );
    const userId = u.rows[0].id;
    const hide = faker.datatype.boolean({ probability: 0.05 });
    const bio = faker.helpers.arrayElement(RU_TEXT.bio);
    const experience_years = faker.number.int({ min: 0, max: 40 });
    const buf =
      useByteaPhoto && imagePaths?.length > 0
        ? readImageBufferAt(imagePaths, i)
        : null;
    const photo = useByteaPhoto ? buf : null;
    await client.query(
      `INSERT INTO doctors (user_id, hide, bio, experience_years, photo) VALUES ($1, $2, $3, $4, $5)`,
      [userId, hide, bio, experience_years, photo],
    );
    inserted += 1;
  }
  return { inserted };
}

/**
 * Пользователь с ролью doctor без строки в doctors — кабинет врача в приложении не откроется.
 * Идемпотентно создаёт минимальные карточки для всех таких users.
 */
export async function ensureDoctorProfilesForDoctorRoleUsers(client) {
  const r = await client.query(`
    INSERT INTO doctors (user_id, hide, bio, experience_years, photo)
    SELECT u.id, false, 'Профиль врача', 0, NULL
    FROM users u
    INNER JOIN roles r ON r.id = u.role_id AND lower(r.code) = 'doctor'
    WHERE NOT EXISTS (SELECT 1 FROM doctors d WHERE d.user_id = u.id)
  `);
  return { inserted: r.rowCount ?? 0 };
}

/**
 * Пользователь с ролью patient без строки в patients — фронт/токен с patientId=null («не является пациентом»).
 * Идемпотентно создаёт минимальные карточки (как ensureDoctorProfiles для врачей).
 * Также закрывает дыру seedUsers: там роль patient может выпасть случайно без INSERT в patients.
 */
export async function ensurePatientProfilesForPatientRoleUsers(client) {
  const r = await client.query(`
    INSERT INTO patients (user_id, birth_date, gender, insurance_number)
    SELECT u.id, DATE '1970-01-01', 1, 'SEED-AUTO-' || u.id::text
    FROM users u
    INNER JOIN roles r ON r.id = u.role_id AND lower(r.code) = 'patient'
    WHERE NOT EXISTS (SELECT 1 FROM patients p WHERE p.user_id = u.id)
  `);
  return { inserted: r.rowCount ?? 0 };
}

export async function seedDoctorSpecializations(client, faker, count) {
  await requireMinCount(client, "doctors", 1, "Нужен хотя бы один врач (doctors).");
  await requireMinCount(client, "specializations", 1, "Нужна хотя бы одна specializations.");
  let inserted = 0;
  let attempts = 0;
  const maxAttempts = count * 20;
  while (inserted < count && attempts < maxAttempts) {
    attempts++;
    const doctor_id = await randomRowId(
      client,
      "SELECT id FROM doctors ORDER BY random() LIMIT 1",
    );
    const specialization_id = await randomRowId(
      client,
      "SELECT id FROM specializations ORDER BY random() LIMIT 1",
    );
    const cert = `CERT-${uniqueRunPrefix()}-${faker.string.alphanumeric(8)}`;
    const valid_from = faker.date.past({ years: 5 });
    const valid_to = faker.date.future({ years: 5, refDate: valid_from });
    try {
      const r = await client.query(
        `INSERT INTO doctor_specializations (doctor_id, specialization_id, valid_from, valid_to, certificate_number)
         VALUES ($1, $2, $3::date, $4::date, $5)`,
        [
          doctor_id,
          specialization_id,
          valid_from.toISOString().slice(0, 10),
          valid_to.toISOString().slice(0, 10),
          cert,
        ],
      );
      if (r.rowCount) inserted += r.rowCount;
    } catch (e) {
      if (e.code === "23505") {
        continue;
      }
      throw e;
    }
  }
  if (inserted < count) {
    console.warn(
      `doctor_specializations: добавлено только ${inserted} из ${count} (много дубликатов пар врач-специализация-сертификат).`,
    );
  }
  return { inserted };
}

export async function seedSpecializationServices(client, _faker, count) {
  if (count > 0) {
    console.warn(
      "specialization_services: count из CLI не используется — связи строятся по specKeys в ruClinicalData.mjs.",
    );
  }
  await requireMinCount(client, "specializations", 1, "Нужна хотя бы одна specializations.");
  await requireMinCount(client, "services", 1, "Нужна хотя бы одна services.");

  const specRows = (await client.query(`SELECT id, name FROM specializations ORDER BY id`)).rows;
  const svcRows = (await client.query(`SELECT id, name FROM services ORDER BY id`)).rows;
  const specNameToId = firstIdByName(specRows);
  const svcNameToId = firstIdByName(svcRows);

  let inserted = 0;
  let skipped = 0;
  for (const svcTpl of SERVICE_TEMPLATES) {
    const serviceId = svcNameToId.get(svcTpl.name);
    if (serviceId == null) {
      skipped++;
      continue;
    }
    for (const specKey of svcTpl.specKeys ?? []) {
      const specName = specializationNameForKey(specKey);
      if (!specName) {
        skipped++;
        continue;
      }
      const specializationId = specNameToId.get(specName);
      if (specializationId == null) {
        skipped++;
        continue;
      }
      const r = await client.query(
        `INSERT INTO specialization_services (specialization_id, service_id, is_active)
         VALUES ($1, $2, true)
         ON CONFLICT (specialization_id, service_id) DO NOTHING`,
        [specializationId, serviceId],
      );
      inserted += r.rowCount ?? 0;
    }
  }
  return { inserted, skippedPairs: skipped };
}

/**
 * Идемпотентно: adm@e.co, doc@e.co, pat@e.co с DEMO_PASSWORD_HASH; пациент и врач; doc — специализация «Психиатрия».
 */
export async function seedFixedDemoUsers(client) {
  const ridAdmin = await roleIdByCode(client, "admin");
  const ridDoctor = await roleIdByCode(client, "doctor");
  const ridPatient = await roleIdByCode(client, "patient");
  if (!ridAdmin || !ridDoctor || !ridPatient) {
    throw new Error(
      "Демо-аккаунты: нет ролей admin/doctor/patient. Сначала выполните seed таблицы roles.",
    );
  }

  const accounts = [
    {
      email: "adm@e.co",
      phone: "+79000001001",
      roleId: ridAdmin,
      first: "Админ",
      last: "Демо",
      middle: null,
    },
    {
      email: "doc@e.co",
      phone: "+79000001002",
      roleId: ridDoctor,
      first: "Иван",
      last: "Психиатров",
      middle: "Демович",
    },
    {
      email: "pat@e.co",
      phone: "+79000001003",
      roleId: ridPatient,
      first: "Пётр",
      last: "Демов",
      middle: null,
    },
  ];

  for (const a of accounts) {
    await client.query(
      `INSERT INTO users (email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, true)
       ON CONFLICT (email) DO UPDATE SET
         phone = EXCLUDED.phone,
         password_hash = EXCLUDED.password_hash,
         role_id = EXCLUDED.role_id,
         first_name = EXCLUDED.first_name,
         last_name = EXCLUDED.last_name,
         middle_name = EXCLUDED.middle_name,
         is_active = true`,
      [a.email, a.phone, DEMO_PASSWORD_HASH, a.first, a.last, a.middle, a.roleId],
    );
  }

  await client.query(
    `INSERT INTO patients (user_id, birth_date, gender, insurance_number)
     SELECT u.id, DATE '1992-06-15', 1, 'DEMO-PAT-001'
     FROM users u WHERE u.email = 'pat@e.co'
     ON CONFLICT (user_id) DO UPDATE SET
       birth_date = EXCLUDED.birth_date,
       gender = EXCLUDED.gender,
       insurance_number = EXCLUDED.insurance_number`,
  );

  await client.query(
    `INSERT INTO doctors (user_id, hide, bio, experience_years, photo)
     SELECT u.id, false, 'Врач-психиатр, демо-аккаунт (doc@e.co).', 12, NULL
     FROM users u WHERE u.email = 'doc@e.co'
     ON CONFLICT (user_id) DO UPDATE SET
       bio = EXCLUDED.bio,
       experience_years = EXCLUDED.experience_years,
       hide = false`,
  );

  const specName = specializationNameForKey(DEMO_PSYCHIATRY_SPEC_KEY);
  const specR = await client.query(
    `SELECT id FROM specializations WHERE name = $1 ORDER BY id LIMIT 1`,
    [specName],
  );
  const docR = await client.query(
    `SELECT d.id FROM doctors d
     INNER JOIN users u ON u.id = d.user_id AND LOWER(TRIM(u.email)) = LOWER(TRIM($1))`,
    ["doc@e.co"],
  );
  let psychLinked = false;
  if (specR.rows[0] && docR.rows[0]) {
    await client.query(
      `INSERT INTO doctor_specializations (doctor_id, specialization_id, valid_from, valid_to, certificate_number)
       VALUES ($1, $2, DATE '2020-01-01', DATE '2030-12-31', 'DEMO-DOC-PSYCH-V1')
       ON CONFLICT (doctor_id, specialization_id, certificate_number) DO NOTHING`,
      [docR.rows[0].id, specR.rows[0].id],
    );
    const ex = await client.query(
      `SELECT 1 FROM doctor_specializations
       WHERE doctor_id = $1 AND specialization_id = $2 AND certificate_number = $3`,
      [docR.rows[0].id, specR.rows[0].id, "DEMO-DOC-PSYCH-V1"],
    );
    psychLinked = ex.rows.length > 0;
  }

  return {
    accounts: accounts.map((a) => a.email),
    demoDoctorPsychiatryLinked: psychLinked,
  };
}

export async function seedDoctorSchedules(client, faker, count) {
  await requireMinCount(client, "doctors", 1, "Нужен хотя бы один doctors.");
  const rooms = await client.query("SELECT id FROM rooms");
  const roomIds = rooms.rows.map((x) => x.id);
  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const doctor_id = await randomRowId(
      client,
      "SELECT id FROM doctors ORDER BY random() LIMIT 1",
    );
    const room_id =
      roomIds.length && faker.datatype.boolean({ probability: 0.85 })
        ? faker.helpers.arrayElement(roomIds)
        : null;
    const dayOffset = faker.number.int({ min: 0, max: 60 });
    const dateAt = new Date();
    dateAt.setDate(dateAt.getDate() + dayOffset);
    const dateStr = dateAt.toISOString().slice(0, 10);
    const startH = faker.number.int({ min: 8, max: 14 });
    const startM = faker.helpers.arrayElement([0, 15, 30, 45]);
    const duration = faker.number.int({ min: 2, max: 6 }) * 30;
    const slot_duration_minutes = faker.helpers.arrayElement([15, 20, 30]);
    let startMin = startH * 60 + startM;
    let endMin = startMin + duration;
    const toTime = (m) => {
      const h = Math.floor(m / 60);
      const mm = m % 60;
      return `${String(h).padStart(2, "0")}:${String(mm).padStart(2, "0")}:00`;
    };
    let start_time = toTime(startMin);
    let end_time = toTime(endMin);
    let suffix = 0;
    for (;;) {
      try {
        const r = await client.query(
          `INSERT INTO doctor_schedules (doctor_id, room_id, dateAt, start_time, end_time, slot_duration_minutes)
           VALUES ($1, $2, $3::date, $4::time, $5::time, $6)`,
          [doctor_id, room_id, dateStr, start_time, end_time, slot_duration_minutes],
        );
        if (r.rowCount) inserted += r.rowCount;
        break;
      } catch (e) {
        if (e.code === "23505") {
          suffix++;
          startMin += 30;
          endMin += 30;
          if (endMin > 20 * 60) {
            endMin = 20 * 60;
            startMin = endMin - duration;
          }
          start_time = toTime(startMin);
          end_time = toTime(endMin);
          if (suffix > 40) {
            break;
          }
          continue;
        }
        throw e;
      }
    }
  }
  return { inserted };
}

export async function seedAppointments(client, faker, count) {
  await requireMinCount(client, "doctors", 1, "Нужен хотя бы один doctors.");
  await requireMinCount(client, "patients", 1, "Нужен хотя бы один patients.");
  const users = await client.query("SELECT id FROM users LIMIT 200");
  const userIds = users.rows.map((r) => r.id);
  const schedules = await client.query(
    `SELECT id, doctor_id, room_id, dateAt, start_time
     FROM doctor_schedules
     WHERE dateAt IS NOT NULL AND start_time IS NOT NULL`,
  );
  const rooms = await client.query("SELECT id FROM rooms");
  const roomIds = rooms.rows.map((r) => r.id);
  const services = await client.query("SELECT id, duration_minutes FROM services");
  const diagnoses = await client.query("SELECT id FROM diagnoses");

  let inserted = 0;
  for (let i = 0; i < count; i++) {
    let attempt = 0;
    let ok = false;
    while (!ok && attempt < 15) {
      attempt++;
      const doctor_id = await randomRowId(
        client,
        "SELECT id FROM doctors ORDER BY random() LIMIT 1",
      );
      const patient_id = await randomRowId(
        client,
        "SELECT id FROM patients ORDER BY random() LIMIT 1",
      );
      const matchingSchedules = schedules.rows.filter(
        (s) => s.doctor_id === doctor_id && s.dateAt != null && s.start_time != null,
      );
      const useSchedule =
        matchingSchedules.length > 0 &&
        faker.datatype.boolean({ probability: 0.5 });
      let schedule_id = null;
      let room_id = null;
      let start;
      let end;
      if (useSchedule) {
        const sch = faker.helpers.arrayElement(matchingSchedules);
        if (sch) {
          const d = pgDateToYmd(sch.dateAt);
          const t = pgTimeToHms(sch.start_time);
          if (d && t) {
            const candidate = new Date(`${d}T${t}`);
            if (isValidDate(candidate)) {
              schedule_id = sch.id;
              room_id = sch.room_id;
              start = candidate;
              const dur = pickDurationMinutes(services.rows, faker);
              end = new Date(start.getTime() + dur * 60 * 1000);
            }
          }
        }
      }
      if (!isValidDate(start) || !isValidDate(end)) {
        schedule_id = null;
        const dayOffset = faker.number.int({ min: 0, max: 45 });
        const base = new Date();
        base.setDate(base.getDate() + dayOffset);
        base.setHours(
          faker.number.int({ min: 9, max: 16 }),
          faker.helpers.arrayElement([0, 30]),
          0,
          0,
        );
        start = base;
        const dur = pickDurationMinutes(services.rows, faker);
        end = new Date(start.getTime() + dur * 60 * 1000);
        room_id =
          roomIds.length && faker.datatype.boolean({ probability: 0.7 })
            ? faker.helpers.arrayElement(roomIds)
            : null;
      }
      if (!isValidDate(start) || !isValidDate(end)) {
        continue;
      }
      const service_id =
        services.rows.length && faker.datatype.boolean({ probability: 0.75 })
          ? faker.helpers.arrayElement(services.rows).id
          : null;
      const diagnosis_id =
        diagnoses.rows.length && faker.datatype.boolean({ probability: 0.4 })
          ? faker.helpers.arrayElement(diagnoses.rows).id
          : null;
      const status = faker.helpers.arrayElement(APPOINTMENT_STATUSES);
      const source = faker.helpers.arrayElement(APPOINTMENT_SOURCES);
      const created_by =
        userIds.length && faker.datatype.boolean({ probability: 0.8 })
          ? faker.helpers.arrayElement(userIds)
          : null;
      const cancel_reason =
        status === "cancelled"
          ? faker.helpers.arrayElement(RU_TEXT.cancelReasons)
          : null;

      try {
        if (end <= start) {
          end = new Date(start.getTime() + 30 * 60 * 1000);
        }
        if (!isValidDate(end)) {
          continue;
        }
        const r = await client.query(
          `INSERT INTO appointments (
             schedule_id, doctor_id, patient_id, room_id, service_id, diagnosis_id,
             start_time, end_time, status, source, created_by, cancel_reason
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
          [
            schedule_id,
            doctor_id,
            patient_id,
            room_id,
            service_id,
            diagnosis_id,
            start.toISOString(),
            end.toISOString(),
            status,
            source,
            created_by,
            cancel_reason,
          ],
        );
        inserted += r.rowCount ?? 1;
        ok = true;
      } catch (e) {
        if (e.code === "23503" || e.code === "23514") {
          continue;
        }
        throw e;
      }
    }
  }
  return { inserted };
}

export async function seedReviews(client, faker, count) {
  const avail = await client.query(
    `SELECT a.id AS appointment_id, a.doctor_id, a.patient_id
     FROM appointments a
     LEFT JOIN reviews r ON r.appointment_id = a.id
     WHERE r.id IS NULL
     ORDER BY random()
     LIMIT $1`,
    [count],
  );
  let inserted = 0;
  for (const row of avail.rows) {
    const rating = faker.number.int({ min: 1, max: 5 });
    const review_text = faker.datatype.boolean({ probability: 0.85 })
      ? faker.helpers.arrayElement(RU_TEXT.reviews)
      : null;
    await client.query(
      `INSERT INTO reviews (appointment_id, doctor_id, patient_id, rating, review_text)
       VALUES ($1, $2, $3, $4, $5)`,
      [row.appointment_id, row.doctor_id, row.patient_id, rating, review_text],
    );
    inserted += 1;
  }
  if (avail.rows.length < count) {
    console.warn(
      `reviews: свободных приёмов без отзыва было ${avail.rows.length}, запрошено ${count}.`,
    );
  }
  return { inserted };
}

export const SEED_HANDLERS = {
  roles: (client, _faker, _count, _imagePaths) => seedRoles(client),
  users: (client, faker, count, _imagePaths) => seedUsers(client, faker, count),
  specializations: (client, faker, count, _imagePaths) =>
    seedSpecializations(client, faker, count),
  rooms: (client, faker, count, _imagePaths) => seedRooms(client, faker, count),
  services: (client, faker, count, _imagePaths) => seedServices(client, faker, count),
  diagnoses: (client, faker, count, _imagePaths) => seedDiagnoses(client, faker, count),
  patients: (client, faker, count, _imagePaths) => seedPatients(client, faker, count),
  doctors: (client, faker, count, imagePaths) =>
    seedDoctors(client, faker, count, imagePaths),
  doctor_specializations: (client, faker, count, _imagePaths) =>
    seedDoctorSpecializations(client, faker, count),
  specialization_services: (client, faker, count, _imagePaths) =>
    seedSpecializationServices(client, faker, count),
  doctor_schedules: (client, faker, count, _imagePaths) =>
    seedDoctorSchedules(client, faker, count),
  appointments: (client, faker, count, _imagePaths) =>
    seedAppointments(client, faker, count),
  reviews: (client, faker, count, _imagePaths) => seedReviews(client, faker, count),
};
