# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml ./
RUN mvn -q -e -B -DskipTests dependency:go-offline

# Build source
COPY src ./src
RUN mvn -q -e -B -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV JAVA_OPTS=""

RUN mkdir -p /app/config

# Copy built jar
COPY --from=build /app/target/*.jar /app/app.jar

RUN printf '%s\n' \
    '#!/bin/sh' \
    'set -eu' \
    '' \
    'UPLOAD_DIR="${UPLOAD_DIR:-/data/uploads}"' \
    'mkdir -p /app/config "${UPLOAD_DIR}"' \
    '' \
    'cat > /app/config/application.properties <<EOF' \
    'spring.application.name=ems-backend' \
    'server.port=8090' \
    '' \
    'spring.datasource.url=${DB_URL:-jdbc:mysql://mysql:3306/employee?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}' \
    'spring.datasource.username=${DB_USER:-ems_user}' \
    'spring.datasource.password=${DB_PASSWORD:-ems_pass}' \
    'spring.jpa.hibernate.ddl-auto=${HIBERNATE_DDL_AUTO:-update}' \
    'spring.jpa.show-sql=${SHOW_SQL:-false}' \
    'spring.jpa.properties.hibernate.format_sql=${FORMAT_SQL:-false}' \
    '' \
    'app.jwt.secret=${JWT_SECRET}' \
    'app.jwt.ttl-ms=${JWT_TTL_MS:-3600000}' \
    'app.cors.allowed-origin-patterns=${CORS_ALLOWED_ORIGIN_PATTERNS:-http://localhost:5173}' \
    '' \
    'app.payroll.epf-employee-percent=${EPF_EMPLOYEE_PERCENT:-8}' \
    'app.payroll.epf-employer-percent=${EPF_EMPLOYER_PERCENT:-12}' \
    '' \
    'app.upload.dir=${UPLOAD_DIR}' \
    'app.upload.max-bytes=${UPLOAD_MAX_BYTES:-10485760}' \
    'app.upload.allowed-mime-types=${UPLOAD_ALLOWED_MIME_TYPES:-application/pdf,image/png,image/jpeg}' \
    'spring.servlet.multipart.max-file-size=${MAX_FILE_SIZE:-10MB}' \
    'spring.servlet.multipart.max-request-size=${MAX_REQUEST_SIZE:-10MB}' \
    'EOF' \
    '' \
    'exec java $JAVA_OPTS -jar /app/app.jar --spring.config.additional-location=file:/app/config/' \
    > /app/entrypoint.sh \
    && chmod +x /app/entrypoint.sh

EXPOSE 8090
ENTRYPOINT ["/app/entrypoint.sh"]