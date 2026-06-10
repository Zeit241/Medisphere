#!/bin/sh
set -e

PATCH_DIR="${1:-/tmp/qwen-patches}"
APP_DIR="${2:-/app}"

if [ ! -d "$PATCH_DIR" ]; then
  echo "qwen-proxy patches: каталог не найден ($PATCH_DIR), пропуск"
  exit 0
fi

# Windows checkout может принести CRLF в submodule — нормализуем перед patch.
find "$APP_DIR" -type f -name "*.js" -exec sed -i 's/\r$//' {} + 2>/dev/null || true
find "$PATCH_DIR" -type f -name "*.patch" -exec sed -i 's/\r$//' {} + 2>/dev/null || true

patch_count=0
for patch_file in "$PATCH_DIR"/*.patch; do
  [ -f "$patch_file" ] || continue
  echo "qwen-proxy patches: применяю $(basename "$patch_file")"
  cd "$APP_DIR"
  patch -p1 --forward --batch -l --input="$patch_file"
  patch_count=$((patch_count + 1))
done

if [ "$patch_count" -eq 0 ]; then
  echo "qwen-proxy patches: файлов не найдено"
else
  echo "qwen-proxy patches: применено $patch_count"
fi
