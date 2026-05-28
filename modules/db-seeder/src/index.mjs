import "dotenv/config";
import { checkbox, input, select, confirm } from "@inquirer/prompts";
import { faker } from "@faker-js/faker/locale/ru";

import { createPool, verifyConnection } from "./db.mjs";
import { listImageFiles, resolveImagesDir } from "./images.mjs";
import {
  SEED_ORDER,
  TABLE_LABELS,
  CLEAR_ORDER,
  CLEAR_TABLE_LABELS,
  sortSelected,
} from "./tables.mjs";
import {
  expandClearSelection,
  orderExpandedForClear,
  truncateTables,
} from "./clearDb.mjs";
import {
  SEED_HANDLERS,
  ensureDoctorProfilesForDoctorRoleUsers,
  ensurePatientProfilesForPatientRoleUsers,
  seedFixedDemoUsers,
} from "./seeders.mjs";
import { runDoctorDayAppointmentsTool } from "./doctorDayAppointments.mjs";

function parsePositiveInt(s, fallback) {
  const n = Number.parseInt(String(s).trim(), 10);
  if (!Number.isFinite(n) || n < 0) {
    return fallback;
  }
  return n;
}

async function main() {
  console.log("\n=== Сидер клиники (Faker + Postgres) ===\n");

  const pool = createPool();
  try {
    const ok = await verifyConnection(pool);
    if (!ok) {
      throw new Error("Не удалось выполнить SELECT 1");
    }
  } catch (e) {
    console.error(
      "Ошибка подключения к БД. Убедитесь, что Docker Postgres запущен (docker compose up -d db) и в .env указаны PGHOST=127.0.0.1 и учётные данные.",
    );
    console.error(e.message || e);
    await pool.end();
    process.exitCode = 1;
    return;
  }

  const entryMode = await select({
    message: "Режим работы",
    choices: [
      { name: "Сидер таблиц (как раньше)", value: "tables" },
      { name: "Очистка таблиц (TRUNCATE)", value: "clear" },
      { name: "Приёмы врача на дату — засеять или очистить", value: "doctor_day" },
    ],
  });

  if (entryMode === "clear") {
    const clearMode = await select({
      message: "Что очистить?",
      choices: [
        { name: "Все таблицы сразу", value: "all" },
        { name: "Выбрать таблицы вручную", value: "pick" },
      ],
    });

    let initialClear = new Set();
    if (clearMode === "all") {
      initialClear = new Set(CLEAR_ORDER);
    } else {
      const pickedClear = await checkbox({
        message:
          "Отметьте таблицы (пробел — да/нет). Зависимые таблицы будут добавлены автоматически.",
        choices: SEED_ORDER.map((value) => ({
          value,
          name: CLEAR_TABLE_LABELS[value] ?? TABLE_LABELS[value] ?? value,
          checked: false,
        })),
        loop: false,
      });
      if (!pickedClear.length) {
        console.log("Ничего не выбрано — выход.");
        await pool.end();
        return;
      }
      initialClear = new Set(pickedClear);
    }

    const expandedClear = expandClearSelection(initialClear);
    const orderedClear = orderExpandedForClear(expandedClear);
    const addedByDeps = [...expandedClear].filter((t) => !initialClear.has(t));

    console.log("\nБудут очищены таблицы (порядок TRUNCATE):");
    for (const t of orderedClear) {
      console.log(`  - ${CLEAR_TABLE_LABELS[t] ?? t}`);
    }
    if (addedByDeps.length) {
      console.log(
        "\nДобавлено из‑за внешних ключей:\n" +
          addedByDeps
            .map((t) => `  - ${CLEAR_TABLE_LABELS[t] ?? t}`)
            .join("\n"),
      );
    }
    console.log(
      "\nИспользуется TRUNCATE … CASCADE: при наличии в БД других таблиц с внешними ключами " +
        "на перечисленные (например queue_entries, notifications, user_roles в initdb/schema.sql), " +
        "они будут очищены автоматически.\n",
    );

    const goClear = await confirm({
      message:
        "Удалить все строки в этих таблицах и сбросить SERIAL? Действие необратимо.",
      default: false,
    });
    if (!goClear) {
      console.log("Отменено.");
      await pool.end();
      return;
    }

    const clearClient = await pool.connect();
    try {
      await clearClient.query("BEGIN");
      const clearResult = await truncateTables(clearClient, orderedClear);
      await clearClient.query("COMMIT");
      console.log("\n✓ Очистка:", clearResult);
    } catch (e) {
      await clearClient.query("ROLLBACK");
      console.error("\n✗ Очистка:", e.message || e);
      if (e.code) {
        console.error("  code:", e.code);
      }
      process.exitCode = 1;
    } finally {
      clearClient.release();
    }

    console.log("\nГотово.\n");
    await pool.end();
    return;
  }

  if (entryMode === "doctor_day") {
    try {
      await runDoctorDayAppointmentsTool(pool, faker);
    } catch (e) {
      console.error(e.message || e);
      process.exitCode = 1;
    }
    await pool.end();
    return;
  }

  const imagesDir = resolveImagesDir();
  const imagePaths = listImageFiles(imagesDir);
  console.log(`Каталог изображений: ${imagesDir}`);
  console.log(
    imagePaths.length
      ? `Найдено файлов для doctors.photo: ${imagePaths.length}`
      : "Изображения не найдены — поле photo будет NULL.\n",
  );

  const mode = await select({
    message: "Режим выбора таблиц",
    choices: [
      { name: "Все таблицы", value: "all" },
      { name: "Выбрать таблицы вручную", value: "pick" },
    ],
  });

  let selected = new Set(SEED_ORDER);

  if (mode === "pick") {
    const picked = await checkbox({
      message: "Отметьте таблицы (пробел — да/нет, enter — готово)",
      choices: SEED_ORDER.map((value) => ({
        value,
        name: TABLE_LABELS[value] ?? value,
        checked: false,
      })),
      loop: false,
    });
    if (!picked.length) {
      console.log("Ничего не выбрано — выход.");
      await pool.end();
      return;
    }
    selected = new Set(picked);
  }

  let ordered = sortSelected(selected);
  const needsRoles = ordered.some((t) =>
    ["users", "patients", "doctors"].includes(t),
  );
  if (needsRoles && !selected.has("roles")) {
    console.log(
      "В начало добавлена таблица roles (нужны коды patient/doctor/admin).\n",
    );
    ordered = ["roles", ...ordered.filter((t) => t !== "roles")];
  }

  const counts = {};

  if (mode === "all") {
    const raw = await input({
      message:
        "Количество записей для каждой таблицы (кроме roles — там фиксированные роли)",
      default: "10",
      validate: (v) => {
        const n = parsePositiveInt(v, -1);
        if (n < 1) {
          return "Введите целое число ≥ 1";
        }
        return true;
      },
    });
    const n = parsePositiveInt(raw, 10);
    for (const key of ordered) {
      if (key === "roles") {
        continue;
      }
      counts[key] = n;
    }
  } else {
    for (const key of ordered) {
      if (key === "roles") {
        continue;
      }
      const raw = await input({
        message: `Сколько записей для «${key}»?`,
        default: "10",
        validate: (v) => {
          const num = parsePositiveInt(v, -1);
          if (num < 1) {
            return "Введите целое число ≥ 1";
          }
          return true;
        },
      });
      counts[key] = parsePositiveInt(raw, 10);
    }
  }

  const summaryLines = ordered.map((k) =>
    k === "roles" ? `${k}: базовые роли (идемпотентно)` : `${k}: ${counts[k] ?? "—"}`,
  );
  console.log("\nПлан:\n" + summaryLines.join("\n") + "\n");

  const go = await confirm({ message: "Выполнить вставки?", default: true });
  if (!go) {
    await pool.end();
    return;
  }

  for (const table of ordered) {
    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const handler = SEED_HANDLERS[table];
      if (!handler) {
        throw new Error(`Нет обработчика для таблицы ${table}`);
      }
      const count = table === "roles" ? 0 : counts[table] ?? 0;
      const result = await handler(client, faker, count, imagePaths);
      await client.query("COMMIT");
      console.log(`✓ ${table}:`, result);
    } catch (e) {
      await client.query("ROLLBACK");
      console.error(`✗ ${table}:`, e.message || e);
      if (e.code) {
        console.error("  code:", e.code);
      }
    } finally {
      client.release();
    }
  }

  {
    const fixClient = await pool.connect();
    try {
      await fixClient.query("BEGIN");
      const fixDoc = await ensureDoctorProfilesForDoctorRoleUsers(fixClient);
      const fixPat = await ensurePatientProfilesForPatientRoleUsers(fixClient);
      await fixClient.query("COMMIT");
      if (fixDoc.inserted > 0) {
        console.log(
          `\n✓ Карточки врачей (doctors): добавлено ${fixDoc.inserted} для пользователей с ролью doctor без строки в doctors.\n`,
        );
      }
      if (fixPat.inserted > 0) {
        console.log(
          `\n✓ Карточки пациентов (patients): добавлено ${fixPat.inserted} для пользователей с ролью patient без строки в patients.\n`,
        );
      }
    } catch (e) {
      await fixClient.query("ROLLBACK");
      console.warn(
        "ensureDoctorProfiles / ensurePatientProfiles:",
        e.message || e,
      );
    } finally {
      fixClient.release();
    }
  }

  {
    const demoClient = await pool.connect();
    try {
      await demoClient.query("BEGIN");
      const demo = await seedFixedDemoUsers(demoClient);
      await demoClient.query("COMMIT");
      console.log("\n✓ Демо-аккаунты (adm@e.co, doc@e.co, pat@e.co):", demo);
    } catch (e) {
      await demoClient.query("ROLLBACK");
      console.warn("seedFixedDemoUsers:", e.message || e);
    } finally {
      demoClient.release();
    }
  }

  console.log("\nГотово.\n");
  await pool.end();
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
