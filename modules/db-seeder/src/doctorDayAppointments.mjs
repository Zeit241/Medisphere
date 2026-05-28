import { confirm, input, select } from "@inquirer/prompts";
import { pathToFileURL } from "node:url";
import { createPool, verifyConnection } from "./db.mjs";
import { faker } from "@faker-js/faker/locale/ru";

/**
 * Границы календарного дня в локальной TZ процесса Node (согласуйте с TZ JVM бэкенда).
 * @returns {[string, string]} ISO UTC для start >= и end <
 */
function localDayRangeIso(dateYmd) {
  const [y, m, d] = dateYmd.split("-").map((x) => Number.parseInt(x, 10));
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) {
    throw new Error(`Некорректная дата: ${dateYmd}`);
  }
  const start = new Date(y, m - 1, d, 0, 0, 0, 0);
  const end = new Date(y, m - 1, d + 1, 0, 0, 0, 0);
  return [start.toISOString(), end.toISOString()];
}

/**
 * Времена начала приёмов на календарный день. Сначала только будущие (относительно now);
 * если не хватает — любые слоты 9–17 с шагом 15 мин; если всё ещё мало — шаг 8 мин с 7:00 до 22:00;
 * затем уникальные минуты от 9:00 (для очень большого count).
 * @returns {{ start: Date, end: Date }[]}
 */
function buildSlotsForCalendarDay(dateYmd, count, fakerInstance, nowMs = Date.now()) {
  const [y, m, d] = dateYmd.split("-").map((x) => Number.parseInt(x, 10));
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) {
    throw new Error(`Некорректная дата: ${dateYmd}`);
  }

  const uniqueStarts = [];
  const seen = new Set();

  const add = (dt) => {
    const k = dt.getTime();
    if (seen.has(k)) return;
    seen.add(k);
    uniqueStarts.push(new Date(dt));
  };

  const grid915 = () => {
    for (let hour = 9; hour <= 17; hour++) {
      for (const min of [0, 15, 30, 45]) {
        if (hour === 17 && min > 0) continue;
        add(new Date(y, m - 1, d, hour, min, 0, 0));
      }
    }
  };

  for (let hour = 9; hour <= 17; hour++) {
    for (const min of [0, 15, 30, 45]) {
      if (hour === 17 && min > 0) continue;
      const s = new Date(y, m - 1, d, hour, min, 0, 0);
      if (s.getTime() >= nowMs) add(s);
    }
  }

  if (uniqueStarts.length < count) {
    console.warn(
      `[doctor-day] На ${dateYmd} будущих слотов мало (${uniqueStarts.length}), добавляю слоты на весь день 9–17 (в т.ч. прошедшее время). Живая очередь Redis покажет только приёмы с start_time > now.`,
    );
    grid915();
  }

  if (uniqueStarts.length < count) {
    let t = new Date(y, m - 1, d, 7, 0, 0, 0);
    const lim = new Date(y, m - 1, d, 22, 0, 0, 0);
    while (t.getTime() <= lim.getTime() && uniqueStarts.length < count) {
      add(new Date(t));
      t = new Date(t.getTime() + 8 * 60 * 1000);
    }
  }

  if (uniqueStarts.length < count) {
    let n = 0;
    const base = new Date(y, m - 1, d, 9, 0, 0, 0);
    while (uniqueStarts.length < count && n < 24 * 60) {
      add(new Date(base.getTime() + n * 60 * 1000));
      n++;
    }
  }

  if (uniqueStarts.length < count) {
    throw new Error(
      `Не удалось сгенерировать ${count} уникальных времён начала на ${dateYmd} (макс. уникальных: ${uniqueStarts.length}). Уменьшите число приёмов.`,
    );
  }

  const picked = fakerInstance.helpers.shuffle([...uniqueStarts]).slice(0, count);
  picked.sort((a, b) => a.getTime() - b.getTime());
  return picked.map((start) => ({
    start,
    end: new Date(start.getTime() + 30 * 60 * 1000),
  }));
}

async function resolveDoctorId(client, raw) {
  const s = String(raw).trim();
  if (!s) {
    throw new Error("Пустой идентификатор врача");
  }
  if (/^\d+$/.test(s)) {
    const id = Number.parseInt(s, 10);
    const r = await client.query("SELECT id FROM doctors WHERE id = $1", [id]);
    if (!r.rows[0]) {
      throw new Error(`Врач с id=${id} не найден в doctors`);
    }
    return r.rows[0].id;
  }
  const r = await client.query(
    `SELECT d.id FROM doctors d
     INNER JOIN users u ON u.id = d.user_id
     WHERE LOWER(TRIM(u.email)) = LOWER(TRIM($1))`,
    [s],
  );
  if (!r.rows[0]) {
    throw new Error(`Врач с email «${s}» не найден (нет users/doctors)`);
  }
  return r.rows[0].id;
}

/**
 * @param {import('pg').PoolClient} client
 */
export async function clearDoctorAppointmentsForDate(client, doctorId, dateYmd) {
  const [startIso, endIso] = localDayRangeIso(dateYmd);
  const sub = `SELECT id FROM appointments WHERE doctor_id = $1 AND start_time >= $2::timestamptz AND start_time < $3::timestamptz`;
  const delRev = await client.query(`DELETE FROM reviews WHERE appointment_id IN (${sub})`, [
    doctorId,
    startIso,
    endIso,
  ]);
  const delApp = await client.query(
    `DELETE FROM appointments WHERE doctor_id = $1 AND start_time >= $2::timestamptz AND start_time < $3::timestamptz`,
    [doctorId, startIso, endIso],
  );
  return {
    reviewsDeleted: delRev.rowCount ?? 0,
    appointmentsDeleted: delApp.rowCount ?? 0,
  };
}

/**
 * @param {import('pg').PoolClient} client
 */
export async function seedDoctorAppointmentsForDate(client, fakerInstance, doctorId, dateYmd, count) {
  const patientsR = await client.query(
    `SELECT id FROM patients ORDER BY random() LIMIT $1`,
    [count],
  );
  if (patientsR.rows.length < count) {
    throw new Error(
      `Нужно ${count} пациентов, в БД выбрано только ${patientsR.rows.length}. Добавьте patients или уменьшите число.`,
    );
  }

  let scheduleRow = await client.query(
    `SELECT id, room_id FROM doctor_schedules WHERE doctor_id = $1 AND dateAt = $2::date LIMIT 1`,
    [doctorId, dateYmd],
  );

  let scheduleId = scheduleRow.rows[0]?.id ?? null;
  let roomId = scheduleRow.rows[0]?.room_id ?? null;

  if (scheduleId == null) {
    const roomPick = await client.query(`SELECT id FROM rooms ORDER BY random() LIMIT 1`);
    roomId = roomPick.rows[0]?.id ?? null;
    const insSch = await client.query(
      `INSERT INTO doctor_schedules (doctor_id, room_id, dateAt, start_time, end_time, slot_duration_minutes)
       VALUES ($1, $2, $3::date, '09:00:00'::time, '18:00:00'::time, 30)
       ON CONFLICT (doctor_id, dateAt, start_time, end_time) DO NOTHING
       RETURNING id, room_id`,
      [doctorId, roomId, dateYmd],
    );
    if (insSch.rows[0]) {
      scheduleId = insSch.rows[0].id;
      roomId = insSch.rows[0].room_id ?? roomId;
    } else {
      const again = await client.query(
        `SELECT id, room_id FROM doctor_schedules WHERE doctor_id = $1 AND dateAt = $2::date LIMIT 1`,
        [doctorId, dateYmd],
      );
      scheduleId = again.rows[0]?.id ?? null;
      roomId = again.rows[0]?.room_id ?? roomId;
    }
  }

  const services = await client.query(`SELECT id FROM services ORDER BY random() LIMIT 5`);
  const diagnoses = await client.query(`SELECT id FROM diagnoses ORDER BY random() LIMIT 5`);

  const slots = buildSlotsForCalendarDay(dateYmd, count, fakerInstance);

  let inserted = 0;
  for (let i = 0; i < count; i++) {
    const patientId = patientsR.rows[i].id;
    const { start, end } = slots[i];
    const serviceId =
      services.rows.length && fakerInstance.datatype.boolean({ probability: 0.7 })
        ? fakerInstance.helpers.arrayElement(services.rows).id
        : null;
    const diagnosisId =
      diagnoses.rows.length && fakerInstance.datatype.boolean({ probability: 0.3 })
        ? fakerInstance.helpers.arrayElement(diagnoses.rows).id
        : null;

    await client.query(
      `INSERT INTO appointments (
         schedule_id, doctor_id, patient_id, room_id, service_id, diagnosis_id,
         start_time, end_time, status, source, created_by, cancel_reason
       ) VALUES ($1, $2, $3, $4, $5, $6, $7::timestamptz, $8::timestamptz, 'confirmed', 'admin', NULL, NULL)`,
      [
        scheduleId,
        doctorId,
        patientId,
        roomId,
        serviceId,
        diagnosisId,
        start.toISOString(),
        end.toISOString(),
      ],
    );
    inserted += 1;
  }

  return { inserted, scheduleId };
}

async function optionalTriggerLiveQueueRebuild(doctorId, dateYmd) {
  const base = process.env.API_BASE_URL?.trim();
  const email = process.env.SEEDER_LOGIN_EMAIL?.trim();
  const password = process.env.SEEDER_LOGIN_PASSWORD?.trim();
  if (!base || !email || !password) {
    return;
  }
  try {
    const loginUrl = `${base.replace(/\/$/, "")}/api/auth/login`;
    const res = await fetch(loginUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json().catch(() => ({}));
    const token = json?.data?.token ?? json?.token;
    if (!token) {
      console.warn("optionalTriggerLiveQueueRebuild: токен не получен, пропуск.");
      return;
    }
    const qUrl = `${base.replace(/\/$/, "")}/api/queue/live/doctor/${doctorId}?date=${encodeURIComponent(dateYmd)}`;
    await fetch(qUrl, { headers: { Authorization: `Bearer ${token}` } });
    console.log("Запрос live-очереди отправлен (пересбор Redis на бэкенде).");
  } catch (e) {
    console.warn("optionalTriggerLiveQueueRebuild:", e.message || e);
  }
}

export async function runDoctorDayAppointmentsTool(pool, fakerInstance) {
  const action = await select({
    message: "Действие",
    choices: [
      { name: "Засеять приёмы на дату", value: "seed" },
      { name: "Очистить приёмы врача на дату", value: "clear" },
    ],
  });

  const doctorRaw = await input({
    message: "ID врача (doctors.id) или email пользователя врача",
    validate: (v) => (String(v).trim() ? true : "Укажите id или email"),
  });

  const dateYmd = await input({
    message: "Дата (YYYY-MM-DD)",
    validate: (v) => (/^\d{4}-\d{2}-\d{2}$/.test(String(v).trim()) ? true : "Формат YYYY-MM-DD"),
  });

  const client = await pool.connect();
  let doctorIdForHook = null;
  const dateTrim = dateYmd.trim();
  try {
    const doctorId = await resolveDoctorId(client, doctorRaw);
    doctorIdForHook = doctorId;

    if (action === "clear") {
      const ok = await confirm({
        message: `Удалить все приёмы (и отзывы) врача ${doctorId} за ${dateTrim}?`,
        default: false,
      });
      if (!ok) {
        console.log("Отменено.");
        return;
      }
      await client.query("BEGIN");
      try {
        const r = await clearDoctorAppointmentsForDate(client, doctorId, dateTrim);
        await client.query("COMMIT");
        console.log("Готово:", r);
      } catch (e) {
        await client.query("ROLLBACK");
        throw e;
      }
    } else {
      const rawN = await input({
        message: "Сколько приёмов создать?",
        default: "5",
        validate: (v) => {
          const n = Number.parseInt(String(v).trim(), 10);
          if (!Number.isFinite(n) || n < 1 || n > 50) {
            return "Целое число от 1 до 50";
          }
          return true;
        },
      });
      const n = Number.parseInt(String(rawN).trim(), 10);
      await client.query("BEGIN");
      try {
        const r = await seedDoctorAppointmentsForDate(client, fakerInstance, doctorId, dateTrim, n);
        await client.query("COMMIT");
        console.log("Готово:", r);
      } catch (e) {
        await client.query("ROLLBACK");
        throw e;
      }
    }
  } finally {
    client.release();
  }

  console.log(
    "\nЖивая очередь Redis: откройте кабинет врача на эту дату или GET /api/queue/live/doctor/{id}?date=...\n" +
      "Опционально в .env: API_BASE_URL, SEEDER_LOGIN_EMAIL, SEEDER_LOGIN_PASSWORD — тогда после сидера будет HTTP-запрос на пересбор очереди.\n",
  );

  if (doctorIdForHook != null) {
    await optionalTriggerLiveQueueRebuild(doctorIdForHook, dateTrim);
  }
}

async function standaloneCli() {
  console.log("\n=== Приёмы врача на дату (Postgres) ===\n");
  const pool = createPool();
  try {
    const ok = await verifyConnection(pool);
    if (!ok) {
      throw new Error("Нет подключения к БД");
    }
    await runDoctorDayAppointmentsTool(pool, faker);
  } finally {
    await pool.end();
  }
}

const isMain = import.meta.url === pathToFileURL(process.argv[1]).href;
if (isMain) {
  standaloneCli().catch((e) => {
    console.error(e.message || e);
    process.exitCode = 1;
  });
}
