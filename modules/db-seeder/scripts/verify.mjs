/**
 * Проверка подключения и базового SQL (всё в транзакции с ROLLBACK).
 * Запуск: node scripts/verify.mjs
 */
import "dotenv/config";
import { faker } from "@faker-js/faker/locale/ru";

import { createPool, verifyConnection } from "../src/db.mjs";
import { seedRoles, seedRooms } from "../src/seeders.mjs";

const pool = createPool();
const ok = await verifyConnection(pool);
if (!ok) {
  console.error("Подключение не удалось");
  await pool.end();
  process.exit(1);
}
console.log("Подключение: OK");

const client = await pool.connect();
try {
  await client.query("BEGIN");
  const roles = await seedRoles(client);
  console.log("seedRoles (будет откат):", roles);
  const rooms = await seedRooms(client, faker, 2);
  console.log("seedRooms x2 (будет откат):", rooms);
  await client.query("ROLLBACK");
  console.log("ROLLBACK выполнен — данные в БД не сохранены.");
} catch (e) {
  await client.query("ROLLBACK");
  console.error("Ошибка:", e.message || e);
  process.exitCode = 1;
} finally {
  client.release();
  await pool.end();
}
