# syntax=docker/dockerfile:1.6
# Сборка без git submodule: исходники клонируются на этапе build.

FROM alpine:3.20 AS source
RUN apk add --no-cache git
ARG GIT_REPO=https://github.com/Zeit241/kursovaya_4_kurs_backend.git
ARG GIT_REF=master
WORKDIR /src
RUN git clone --depth 1 --branch "${GIT_REF}" "${GIT_REPO}" app \
 || (git clone "${GIT_REPO}" app && cd app && git checkout "${GIT_REF}")

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY --from=source /src/app/pom.xml .
RUN mvn -B dependency:go-offline
COPY --from=source /src/app/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runner
RUN apk add --no-cache wget \
 && addgroup -S spring \
 && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/target/kursovaya-*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8085
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8085/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
