#!/bin/sh
set -e

# Bind-mounts с хоста (Windows/Linux) часто приходят без прав на запись для appuser.
mkdir -p /app/session/accounts /app/logs /app/uploads
chown -R appuser:appuser /app/session /app/logs /app/uploads 2>/dev/null || true
chmod -R a+rwX /app/session /app/logs /app/uploads 2>/dev/null || true

if gosu appuser test -w /app/session 2>/dev/null; then
  exec gosu appuser "$@"
fi

# Windows bind-mount: chown не работает — запускаем от root, иначе EACCES на session/accounts.
exec "$@"
