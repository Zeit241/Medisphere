import { CLEAR_ORDER } from "./tables.mjs";

/**
 * Родительская таблица → дочерние, у которых есть FK на родителя (схема NEW.txt).
 * Чтобы очистить родителя, сначала нужно очистить всех детей.
 */
export const REFERENCED_BY_CHILDREN = {
  roles: ["users"],
  users: ["patients", "doctors", "appointments"],
  specializations: ["doctor_specializations", "specialization_services"],
  rooms: ["doctor_schedules", "appointments"],
  services: ["specialization_services", "appointments"],
  diagnoses: ["appointments"],
  patients: ["appointments", "reviews"],
  doctors: ["doctor_specializations", "doctor_schedules", "appointments", "reviews"],
  doctor_schedules: ["appointments"],
  appointments: ["reviews"],
};

/**
 * Расширяет выбор: добавляет все таблицы, которые ссылаются на уже выбранные
 * (иначе TRUNCATE родителя невозможен из‑за FK).
 * @param {Set<string>|Iterable<string>} selected
 * @returns {Set<string>}
 */
export function expandClearSelection(selected) {
  const expanded = new Set(selected);
  let changed = true;
  while (changed) {
    changed = false;
    for (const parent of [...expanded]) {
      for (const child of REFERENCED_BY_CHILDREN[parent] ?? []) {
        if (!expanded.has(child)) {
          expanded.add(child);
          changed = true;
        }
      }
    }
  }
  return expanded;
}

/**
 * Порядок TRUNCATE: от листьев к корням (совпадает с CLEAR_ORDER в tables.mjs).
 * @param {Set<string>} expanded
 * @returns {string[]}
 */
export function orderExpandedForClear(expanded) {
  return CLEAR_ORDER.filter((t) => expanded.has(t));
}

function quoteIdent(name) {
  return `"${String(name).replace(/"/g, '""')}"`;
}

/**
 * Очищает таблицы одной командой TRUNCATE … RESTART IDENTITY CASCADE.
 * CASCADE нужен, если в БД есть таблицы вне списка сидера с FK на них
 * (например queue_entries, notifications, user_roles в initdb/schema.sql).
 * @param {import('pg').PoolClient} client
 * @param {string[]} orderedTableNames
 */
export async function truncateTables(client, orderedTableNames) {
  if (!orderedTableNames.length) {
    return { truncated: 0, tables: [] };
  }
  const list = orderedTableNames.map(quoteIdent).join(", ");
  await client.query(`TRUNCATE TABLE ${list} RESTART IDENTITY CASCADE`);
  return { truncated: orderedTableNames.length, tables: [...orderedTableNames] };
}
