import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const IMAGE_EXT = new Set([".jpg", ".jpeg", ".png", ".webp"]);

/**
 * Каталог изображений: SEED_IMAGES_DIR или ../landing/public/images от db-seeder/
 */
export function resolveImagesDir() {
  if (process.env.SEED_IMAGES_DIR?.trim()) {
    return path.resolve(process.env.SEED_IMAGES_DIR.trim());
  }
  return path.resolve(__dirname, "..", "..", "landing", "public", "images");
}

export function listImageFiles(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }
  const names = fs.readdirSync(dir);
  return names
    .filter((n) => IMAGE_EXT.has(path.extname(n).toLowerCase()))
    .map((n) => path.join(dir, n));
}

/**
 * Round-robin: вызывать с инкрементным индексом для распределения фото по врачам
 */
export function readImageBufferAt(paths, index) {
  if (!paths.length) {
    return null;
  }
  const p = paths[index % paths.length];
  try {
    return fs.readFileSync(p);
  } catch {
    return null;
  }
}
