#!/usr/bin/env node
/**
 * Генератор infra/migrations/seed_demo.sql и обогащённого map_geojson.json
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
  SPECIALIZATION_TEMPLATES,
  SERVICE_TEMPLATES,
  specializationNameForKey,
} from "../modules/db-seeder/src/ruClinicalData.mjs";
import {
  DEMO_PASSWORD_HASH,
  RU_TEXT,
  DOCTOR_NAMES,
  PATIENT_NAMES,
  ROOM_PURPOSES,
  DIAGNOSIS_POOLS,
  SPEC_DIAGNOSIS_POOL,
} from "./seedDemoClinicalData.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const GEOJSON_PATH = join(
  ROOT,
  "modules/mobile-android/app/src/main/assets/map_geojson.json",
);
const SQL_OUT = join(ROOT, "infra/migrations/seed_demo.sql");

const DOCTOR_COUNT = 35;
const PATIENT_COUNT = 35;
const APPOINTMENTS_PER_DOCTOR = 6;
const SOURCES = ["mobile", "web", "reception", "admin"];

function sqlStr(v) {
  if (v == null) return "NULL";
  return `'${String(v).replace(/'/g, "''")}'`;
}

function midPrice(tpl) {
  return ((tpl.priceMin + tpl.priceMax) / 2).toFixed(2);
}

function parseGeojsonRaw(text) {
  const fixed = text.replace(
    /"properties":\{"featureid":"caa2bbca1a57498c53b80e875361f6f1","geometry":/,
    '"properties":{"featureid":"caa2bbca1a57498c53b80e875361f6f1"},' +
      '"geometry":',
  );
  return JSON.parse(fixed);
}

function extractRoomPolygons(geojson) {
  return geojson.features.filter((f) => {
    if (f.geometry?.type !== "Polygon") return false;
    const color = f.styledetails?.styledata?.fillcolor;
    return color === "#10CAF0";
  });
}

function buildDiagnoses(specs) {
  const usedCodes = new Set();
  const rows = [];
  for (const spec of specs) {
    const poolKey = SPEC_DIAGNOSIS_POOL[spec.key] ?? "default";
    const pool = DIAGNOSIS_POOLS[poolKey] ?? DIAGNOSIS_POOLS.default;
    let added = 0;
    for (const [code, name] of pool) {
      if (added >= 20) break;
      let uniqueCode = code;
      let suffix = 0;
      while (usedCodes.has(uniqueCode)) {
        suffix++;
        uniqueCode = `${code}-S${suffix}`;
      }
      usedCodes.add(uniqueCode);
      rows.push({ code: uniqueCode, name, category: spec.name });
      added++;
    }
    while (added < 20) {
      const base = pool[added % pool.length];
      let uniqueCode = `${base[0]}-X${spec.key}-${added}`;
      while (usedCodes.has(uniqueCode)) uniqueCode += "a";
      usedCodes.add(uniqueCode);
      rows.push({
        code: uniqueCode,
        name: base[1],
        category: spec.name,
      });
      added++;
    }
  }
  return rows;
}

function buildServicesForSpecs(specs) {
  const serviceRows = [];
  const specServiceLinks = [];
  const nameToId = new Map();
  let svcId = 0;

  for (const spec of specs) {
    const matched = SERVICE_TEMPLATES.filter((s) =>
      (s.specKeys ?? []).includes(spec.key),
    );
    let services = matched.slice(0, 8);
    if (services.length < 1) {
      services = [
        {
          name: `Консультация (${spec.name})`,
          description: `Первичный приём врача специальности «${spec.name}»`,
          duration_minutes: 30,
          priceMin: 1500,
          priceMax: 3500,
        },
      ];
    }
    for (const tpl of services) {
      if (!nameToId.has(tpl.name)) {
        svcId++;
        const code = `SVC-${spec.key.toUpperCase().replace(/[^A-Z0-9]/g, "_")}-${svcId}`;
        serviceRows.push({
          id: svcId,
          name: tpl.name,
          code,
          price: midPrice(tpl),
          duration_minutes: tpl.duration_minutes,
          description: tpl.description,
        });
        nameToId.set(tpl.name, svcId);
      }
      specServiceLinks.push({
        specialization_id: spec.id,
        service_id: nameToId.get(tpl.name),
      });
    }
  }
  return { serviceRows, specServiceLinks };
}

function assignDoctorSpecs(specs) {
  const links = [];
  const specKeys = specs.map((s) => s.key);
  const keyToId = Object.fromEntries(specs.map((s) => [s.key, s.id]));

  // Врачи 1 и 2 — оба кардиологи
  links.push({ doctor_id: 1, specialization_id: keyToId.cardiology });
  links.push({ doctor_id: 2, specialization_id: keyToId.cardiology });

  // Каждая специализация минимум 1 врач
  for (let i = 0; i < specs.length; i++) {
    const doctorId = (i % DOCTOR_COUNT) + 1;
    const link = { doctor_id: doctorId, specialization_id: specs[i].id };
    if (
      !links.some(
        (l) =>
          l.doctor_id === link.doctor_id &&
          l.specialization_id === link.specialization_id,
      )
    ) {
      links.push(link);
    }
  }

  // ~12 врачей с 2-й специализацией
  const dualPairs = [
    [1, "therapy"],
    [3, "functional_dx"],
    [4, "gastroenterology"],
    [5, "endocrinology"],
    [6, "psychiatry"],
    [7, "ultrasound_dx"],
    [8, "family_medicine"],
    [9, "rheumatology"],
    [10, "allergology"],
    [11, "hematology"],
    [12, "physiotherapy"],
  ];
  for (const [docId, key] of dualPairs) {
    if (keyToId[key]) {
      const link = { doctor_id: docId, specialization_id: keyToId[key] };
      if (
        !links.some(
          (l) =>
            l.doctor_id === link.doctor_id &&
            l.specialization_id === link.specialization_id,
        )
      ) {
        links.push(link);
      }
    }
  }

  return links;
}

function enrichGeojson(geojson, rooms) {
  const roomByCode = Object.fromEntries(rooms.map((r) => [r.code, r]));
  for (const f of geojson.features) {
    const id = f.id ?? f.properties?.featureid;
    const room = roomByCode[id];
    if (room) {
      f.properties = f.properties ?? {};
      f.properties.featureid = id;
      f.properties.room = String(room.id);
      f.properties.name = room.name;
    }
    if (id === "93af267439362d24f63e654c438f8d96") {
      f.properties = f.properties ?? {};
      f.properties.featureid = id;
      f.properties.name = "Регистратура";
      f.properties.room = "reg";
    }
    if (f.id === "caa2bbca1a57498c53b80e875361f6f1" && !f.geometry?.coordinates) {
      const nested = f.properties?.geometry;
      if (nested) {
        f.geometry = nested;
        delete f.properties.geometry;
      }
    }
  }
  return geojson;
}

function generateSql({ specs, rooms, diagnoses, serviceRows, specServiceLinks, doctorSpecLinks }) {
  const lines = [];
  const push = (s = "") => lines.push(s);

  push("-- ============================================================");
  push("-- Демо-данные для схемы NEW.txt (сгенерировано generate-seed-demo.mjs)");
  push("-- Загрузка: psql -U clinic_user -d clinic_db -f infra/migrations/seed_demo.sql");
  push("-- Пароль всех пользователей: тот, для которого сгенерирован DEMO_PASSWORD_HASH");
  push("-- ============================================================");
  push("");
  push("BEGIN;");
  push("");
  push("TRUNCATE TABLE");
  push("  reviews, appointments, doctor_schedules, specialization_services,");
  push("  doctor_specializations, doctors, patients, users, diagnoses, services,");
  push("  rooms, specializations, roles");
  push("RESTART IDENTITY CASCADE;");
  push("");

  push("-- 1. Роли");
  push("INSERT INTO roles (id, code, name) VALUES");
  push("  (1, 'patient', 'Пациент'),");
  push("  (2, 'doctor', 'Врач'),");
  push("  (3, 'admin', 'Администратор');");
  push("SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));");
  push("");

  push("-- 2. Пользователи");
  const userValues = [];
  let userId = 0;
  for (let i = 0; i < DOCTOR_COUNT; i++) {
    userId++;
    const n = DOCTOR_NAMES[i];
    const phone = `+7996${String(41753630 + i).padStart(8, "0")}`;
    userValues.push(
      `(${userId}, ${sqlStr(`doctor${i + 1}@clinic.local`)}, ${sqlStr(phone)}, ${sqlStr(DEMO_PASSWORD_HASH)}, ${sqlStr(n.first)}, ${sqlStr(n.last)}, ${sqlStr(n.middle)}, 2, true)`,
    );
  }
  for (let i = 0; i < PATIENT_COUNT; i++) {
    userId++;
    const n = PATIENT_NAMES[i];
    const phone = `+7997${String(64175360 + i).padStart(8, "0")}`;
    userValues.push(
      `(${userId}, ${sqlStr(`patient${i + 1}@clinic.local`)}, ${sqlStr(phone)}, ${sqlStr(DEMO_PASSWORD_HASH)}, ${sqlStr(n.first)}, ${sqlStr(n.last)}, ${sqlStr(n.middle)}, 1, true)`,
    );
  }
  userId++;
  userValues.push(
    `(${userId}, 'admin@clinic.local', '+799600000001', ${sqlStr(DEMO_PASSWORD_HASH)}, 'Админ', 'Системный', 'Админович', 3, true)`,
  );
  push(
    "INSERT INTO users (id, email, phone, password_hash, first_name, last_name, middle_name, role_id, is_active) VALUES",
  );
  push(userValues.join(",\n") + ";");
  push(`SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));`);
  push("");

  push("-- 3. Специализации");
  push("INSERT INTO specializations (id, code, name, description) VALUES");
  push(
    specs
      .map(
        (s) =>
          `(${s.id}, ${sqlStr(s.key)}, ${sqlStr(s.name)}, ${sqlStr(s.description)})`,
      )
      .join(",\n") + ";",
  );
  push("SELECT setval('specializations_id_seq', (SELECT MAX(id) FROM specializations));");
  push("");

  push("-- 4. Кабинеты (code = feature id из map_geojson.json)");
  push("INSERT INTO rooms (id, code, name) VALUES");
  push(
    rooms
      .map((r) => `(${r.id}, ${sqlStr(r.code)}, ${sqlStr(r.name)})`)
      .join(",\n") + ";",
  );
  push("SELECT setval('rooms_id_seq', (SELECT MAX(id) FROM rooms));");
  push("");

  push("-- 5. Услуги");
  push(
    "INSERT INTO services (id, name, code, price, duration_minutes, description) VALUES",
  );
  push(
    serviceRows
      .map(
        (s) =>
          `(${s.id}, ${sqlStr(s.name)}, ${sqlStr(s.code)}, ${s.price}, ${s.duration_minutes}, ${sqlStr(s.description)})`,
      )
      .join(",\n") + ";",
  );
  push("SELECT setval('services_id_seq', (SELECT MAX(id) FROM services));");
  push("");

  push("-- 6. Диагнозы (category = специализация)");
  push("INSERT INTO diagnoses (id, code, name, category) VALUES");
  diagnoses.forEach((d, i) => {
    d.id = i + 1;
  });
  push(
    diagnoses
      .map(
        (d) =>
          `(${d.id}, ${sqlStr(d.code)}, ${sqlStr(d.name)}, ${sqlStr(d.category)})`,
      )
      .join(",\n") + ";",
  );
  push("SELECT setval('diagnoses_id_seq', (SELECT MAX(id) FROM diagnoses));");
  push("");

  push("-- 7. Пациенты");
  push("INSERT INTO patients (id, user_id, birth_date, gender, insurance_number) VALUES");
  const patVals = [];
  for (let i = 0; i < PATIENT_COUNT; i++) {
    const n = PATIENT_NAMES[i];
    const ins = String(5914805555555550n + BigInt(i));
    patVals.push(
      `(${i + 1}, ${DOCTOR_COUNT + i + 1}, ${sqlStr(n.birth)}, ${n.gender}, ${sqlStr(ins)})`,
    );
  }
  push(patVals.join(",\n") + ";");
  push("SELECT setval('patients_id_seq', (SELECT MAX(id) FROM patients));");
  push("");

  push("-- 8. Врачи");
  push("INSERT INTO doctors (id, user_id, hide, bio, experience_years) VALUES");
  const docVals = [];
  for (let i = 0; i < DOCTOR_COUNT; i++) {
    const bio = RU_TEXT.bio[i % RU_TEXT.bio.length];
    const exp = 3 + (i % 23);
    docVals.push(`(${i + 1}, ${i + 1}, false, ${sqlStr(bio)}, ${exp})`);
  }
  push(docVals.join(",\n") + ";");
  push("SELECT setval('doctors_id_seq', (SELECT MAX(id) FROM doctors));");
  push("");

  push("-- 9. Врач — специализация");
  push("INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES");
  push(
    doctorSpecLinks
      .map((l) => `(${l.doctor_id}, ${l.specialization_id})`)
      .join(",\n") + ";",
  );
  push("");

  push("-- 10. Услуги по специализациям");
  const uniqLinks = [];
  const seen = new Set();
  for (const l of specServiceLinks) {
    const k = `${l.specialization_id}-${l.service_id}`;
    if (seen.has(k)) continue;
    seen.add(k);
    uniqLinks.push(l);
  }
  push(
    "INSERT INTO specialization_services (specialization_id, service_id, is_active) VALUES",
  );
  push(
    uniqLinks.map((l) => `(${l.specialization_id}, ${l.service_id}, true)`).join(",\n") +
      ";",
  );
  push("");

  // Diagnosis lookup by category name
  const dxByCategory = new Map();
  for (const d of diagnoses) {
    if (!dxByCategory.has(d.category)) dxByCategory.set(d.category, []);
    dxByCategory.get(d.category).push(d.id);
  }

  const doctorPrimarySpec = new Map();
  for (const l of doctorSpecLinks) {
    if (!doctorPrimarySpec.has(l.doctor_id)) {
      const spec = specs.find((s) => s.id === l.specialization_id);
      doctorPrimarySpec.set(l.doctor_id, spec);
    }
  }

  const serviceBySpec = new Map();
  for (const l of uniqLinks) {
    if (!serviceBySpec.has(l.specialization_id)) serviceBySpec.set(l.specialization_id, []);
    serviceBySpec.get(l.specialization_id).push(l.service_id);
  }

  push("-- 11. Расписание");
  let schedId = 0;
  const scheduleRows = [];
  for (let d = 1; d <= DOCTOR_COUNT; d++) {
    schedId++;
    const roomId = ((d - 1) % rooms.length) + 1;
    scheduleRows.push(
      `(${schedId}, ${d}, ${roomId}, CURRENT_DATE - 7, '09:00', '13:00', 30)`,
    );
    schedId++;
    scheduleRows.push(
      `(${schedId}, ${d}, ${roomId}, CURRENT_DATE + 7, '14:00', '18:00', 30)`,
    );
  }
  push(
    "INSERT INTO doctor_schedules (id, doctor_id, room_id, dateAt, start_time, end_time, slot_duration_minutes) VALUES",
  );
  push(scheduleRows.join(",\n") + ";");
  push("SELECT setval('doctor_schedules_id_seq', (SELECT MAX(id) FROM doctor_schedules));");
  push("");

  push("-- 12. Приёмы");
  let apptId = 0;
  const apptValues = [];
  const completedAppts = [];
  const statusPattern = ["completed", "completed", "completed", "completed", "cancelled", "no_show"];

  for (let docId = 1; docId <= DOCTOR_COUNT; docId++) {
    const spec = doctorPrimarySpec.get(docId);
    const specDx = dxByCategory.get(spec?.name) ?? [1];
    const specSvcs = serviceBySpec.get(spec?.id) ?? [1];
    const roomId = ((docId - 1) % rooms.length) + 1;
    const schedBase = (docId - 1) * 2 + 1;

    for (let j = 0; j < APPOINTMENTS_PER_DOCTOR; j++) {
      apptId++;
      const patId = ((docId + j) % PATIENT_COUNT) + 1;
      const status = statusPattern[j];
      const dayOffset = 5 + docId + j * 3;
      const hour = 9 + (j % 4);
      const svcId = specSvcs[j % specSvcs.length];
      const svc = serviceRows.find((s) => s.id === svcId);
      const dur = svc?.duration_minutes ?? 30;
      const dxId =
        status === "completed" ? specDx[j % specDx.length] : "NULL";
      const cancel =
        status === "cancelled"
          ? sqlStr(RU_TEXT.cancelReasons[j % RU_TEXT.cancelReasons.length])
          : "NULL";
      const source = sqlStr(SOURCES[j % SOURCES.length]);
      const createdBy = 71;
      const timeStart = `${String(hour).padStart(2, "0")}:00:00`;

      apptValues.push(
        `(${apptId}, ${schedBase}, ${docId}, ${patId}, ${roomId}, ${svcId}, ${dxId}, ` +
          `((CURRENT_DATE - ${dayOffset})::date + TIME '${timeStart}')::timestamptz, ` +
          `((CURRENT_DATE - ${dayOffset})::date + TIME '${timeStart}' + make_interval(mins => ${dur}))::timestamptz, ` +
          `${sqlStr(status)}, ${source}, ${createdBy}, ${cancel}, ` +
          `${status === "completed" ? sqlStr(RU_TEXT.complaints[j % RU_TEXT.complaints.length]) : "NULL"}, ` +
          `${status === "completed" ? sqlStr(RU_TEXT.anamnesis[j % RU_TEXT.anamnesis.length]) : "NULL"}, ` +
          `${status === "completed" ? sqlStr(RU_TEXT.recommendations[j % RU_TEXT.recommendations.length]) : "NULL"})`,
      );
      if (status === "completed") {
        completedAppts.push({ apptId, docId, patId });
      }
    }
  }

  push(
    "INSERT INTO appointments (id, schedule_id, doctor_id, patient_id, room_id, service_id, diagnosis_id, " +
      "start_time, end_time, status, source, created_by, cancel_reason, complaints, anamnesis, recommendations) VALUES",
  );
  push(apptValues.join(",\n") + ";");
  push("SELECT setval('appointments_id_seq', (SELECT MAX(id) FROM appointments));");
  push("");

  push("-- 13. Отзывы (на каждый completed приём)");
  let revId = 0;
  const revVals = [];
  for (const row of completedAppts) {
    revId++;
    const rating = 3 + (revId % 3);
    const text = sqlStr(RU_TEXT.reviews[revId % RU_TEXT.reviews.length]);
    revVals.push(
      `(${revId}, ${row.apptId}, ${row.docId}, ${row.patId}, ${rating}, ${text})`,
    );
  }
  push("INSERT INTO reviews (id, appointment_id, doctor_id, patient_id, rating, review_text) VALUES");
  push(revVals.join(",\n") + ";");
  push("SELECT setval('reviews_id_seq', (SELECT MAX(id) FROM reviews));");
  push("");

  push("-- Проверка объёмов");
  push(`DO $$ BEGIN
  IF (SELECT COUNT(*) FROM roles) <> 3 THEN RAISE EXCEPTION 'roles count'; END IF;
  IF (SELECT COUNT(*) FROM doctors) < 35 THEN RAISE EXCEPTION 'doctors count'; END IF;
  IF (SELECT COUNT(*) FROM patients) < 35 THEN RAISE EXCEPTION 'patients count'; END IF;
  IF (SELECT COUNT(*) FROM specializations) < 40 THEN RAISE EXCEPTION 'specializations count'; END IF;
  IF (SELECT COUNT(*) FROM diagnoses) < 800 THEN RAISE EXCEPTION 'diagnoses count'; END IF;
  IF (SELECT COUNT(*) FROM reviews) <> (SELECT COUNT(*) FROM appointments WHERE status = 'completed') THEN
    RAISE EXCEPTION 'reviews mismatch';
  END IF;
END $$;`);
  push("");
  push("COMMIT;");

  return lines.join("\n");
}

function main() {
  const allSpecs = SPECIALIZATION_TEMPLATES.map((s, i) => ({
    ...s,
    id: i + 1,
  }));

  const geoRaw = readFileSync(GEOJSON_PATH, "utf8");
  const geojson = parseGeojsonRaw(geoRaw);
  const polygons = extractRoomPolygons(geojson);

  const rooms = polygons.map((f, i) => {
    const code = f.id ?? f.properties?.featureid;
    const purpose = ROOM_PURPOSES[i % ROOM_PURPOSES.length];
    return {
      id: i + 1,
      code,
      name: `Кабинет №${201 + i} — ${purpose}`,
    };
  });

  const diagnoses = buildDiagnoses(allSpecs);
  const { serviceRows, specServiceLinks } = buildServicesForSpecs(allSpecs);
  const doctorSpecLinks = assignDoctorSpecs(allSpecs);

  const enriched = enrichGeojson(geojson, rooms);
  writeFileSync(GEOJSON_PATH, JSON.stringify(enriched), "utf8");

  const sql = generateSql({
    specs: allSpecs,
    rooms,
    diagnoses,
    serviceRows,
    specServiceLinks,
    doctorSpecLinks,
  });
  writeFileSync(SQL_OUT, sql, "utf8");

  console.log(`Generated ${SQL_OUT}`);
  console.log(`Updated ${GEOJSON_PATH}`);
  console.log(
    `Stats: ${allSpecs.length} specs, ${rooms.length} rooms, ${diagnoses.length} diagnoses, ` +
      `${serviceRows.length} services, ${DOCTOR_COUNT * APPOINTMENTS_PER_DOCTOR} appointments`,
  );
}

main();
