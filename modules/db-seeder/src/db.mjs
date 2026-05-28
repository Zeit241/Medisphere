import pg from "pg";

const { Pool } = pg;

export function createPool() {
  return new Pool({
    host: process.env.PGHOST ?? "127.0.0.1",
    port: Number(process.env.PGPORT ?? 5432),
    user: process.env.PGUSER ?? "login",
    password: process.env.PGPASSWORD ?? "pass",
    database: process.env.PGDATABASE ?? "clinic_db",
  });
}

export async function verifyConnection(pool) {
  const r = await pool.query("SELECT 1 AS ok");
  return r.rows[0]?.ok === 1;
}
