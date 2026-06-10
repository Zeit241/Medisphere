# syntax=docker/dockerfile:1.6
# Обёртка над upstream FreeQwenApi: код берётся из submodule modules/qwen-proxy.
FROM node:20-slim AS base

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      chromium fonts-liberation libatk-bridge2.0-0 libatk1.0-0 \
      libcups2 libdrm2 libgbm1 libnss3 libxcomposite1 \
      libxdamage1 libxrandr2 xdg-utils ca-certificates gosu patch \
 && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    CHROME_PATH=/usr/bin/chromium \
    NODE_ENV=production

WORKDIR /app

COPY modules/qwen-proxy/package.json ./
RUN npm install --omit=dev

COPY modules/qwen-proxy/ ./

COPY infra/qwen-proxy/patches/ /tmp/qwen-patches/
COPY infra/qwen-proxy/apply-patches.sh /tmp/apply-qwen-patches.sh
RUN chmod +x /tmp/apply-qwen-patches.sh \
 && sed -i 's/\r$//' /tmp/apply-qwen-patches.sh \
 && /tmp/apply-qwen-patches.sh /tmp/qwen-patches /app

RUN mkdir -p /app/session /app/logs /app/uploads \
 && useradd -m appuser \
 && chown -R appuser:appuser /app

COPY infra/qwen-proxy/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh \
 && chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 3264

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["node", "index.js"]
