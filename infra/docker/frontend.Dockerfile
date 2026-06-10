# syntax=docker/dockerfile:1.6

FROM alpine:3.20 AS source
RUN apk add --no-cache git
ARG GIT_REPO=https://github.com/Zeit241/kursovaya_4_kurs_frontend.git
ARG GIT_REF=main
WORKDIR /src
RUN git clone --depth 1 --branch "${GIT_REF}" "${GIT_REPO}" app \
 || (git clone "${GIT_REPO}" app && cd app && git checkout "${GIT_REF}")

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=source /src/app/package.json /src/app/package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY --from=source /src/app/ .
ARG VITE_API_URL
ARG VITE_DIRECTUS_URL=/__directus
ARG VITE_DIRECTUS_PUBLIC_URL
ARG VITE_DIRECTUS_STATIC_TOKEN
ENV VITE_API_URL=$VITE_API_URL \
    VITE_DIRECTUS_URL=$VITE_DIRECTUS_URL \
    VITE_DIRECTUS_PUBLIC_URL=$VITE_DIRECTUS_PUBLIC_URL \
    VITE_DIRECTUS_STATIC_TOKEN=$VITE_DIRECTUS_STATIC_TOKEN
RUN npm run build

FROM nginx:1.27-alpine
COPY infra/docker/frontend.nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
