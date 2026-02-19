#!/bin/bash
# ========================================================================
#  EMS — EC2 User Data Bootstrap Script
#  Installs Docker + Docker Compose and pulls the EMS application
# ========================================================================
set -euo pipefail

exec > /var/log/ems-bootstrap.log 2>&1
echo "=== EMS Bootstrap started at $(date) ==="

# ── Install Docker ────────────────────────────────────────────────────────
apt-get update -y
apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

# ── Create deployment directory ───────────────────────────────────────────
mkdir -p /opt/ems
cd /opt/ems

# ── Write environment file ────────────────────────────────────────────────
cat > .env <<'ENVEOF'
DOCKER_REGISTRY=${docker_registry}
IMAGE_TAG=latest

MYSQL_DATABASE=${db_name}
MYSQL_USER=${db_username}
MYSQL_PASSWORD=${db_password}
MYSQL_ROOT_PASSWORD=${db_password}

DB_URL=jdbc:mysql://${db_host}:${db_port}/${db_name}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=${db_username}
DB_PASSWORD=${db_password}

JWT_SECRET=${jwt_secret}
JWT_TTL_MS=3600000
EPF_EMPLOYEE_PERCENT=8
EPF_EMPLOYER_PERCENT=12

CORS_ALLOWED_ORIGIN_PATTERNS=*
ENVEOF

chmod 600 .env

# ── Write docker-compose for deployment ───────────────────────────────────
cat > docker-compose.yml <<'COMPOSEEOF'
services:
  ems-backend:
    image: $${DOCKER_REGISTRY}/ems-backend:$${IMAGE_TAG:-latest}
    container_name: ems-backend
    restart: unless-stopped
    env_file: .env
    environment:
      DB_URL: $${DB_URL}
      DB_USER: $${DB_USER}
      DB_PASSWORD: $${DB_PASSWORD}
      JWT_SECRET: $${JWT_SECRET}
      JWT_TTL_MS: $${JWT_TTL_MS:-3600000}
      EPF_EMPLOYEE_PERCENT: $${EPF_EMPLOYEE_PERCENT:-8}
      EPF_EMPLOYER_PERCENT: $${EPF_EMPLOYER_PERCENT:-12}
      CORS_ALLOWED_ORIGIN_PATTERNS: $${CORS_ALLOWED_ORIGIN_PATTERNS:-*}
      JAVA_OPTS: -Xms256m -Xmx512m
    volumes:
      - ./data/uploads:/data/uploads
    ports:
      - "8090:8090"
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8090/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  ems-frontend:
    image: $${DOCKER_REGISTRY}/ems-frontend:$${IMAGE_TAG:-latest}
    container_name: ems-frontend
    restart: unless-stopped
    depends_on:
      - ems-backend
    ports:
      - "80:80"
      - "443:443"
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:80/ || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
COMPOSEEOF

# ── Pull and start ────────────────────────────────────────────────────────
docker compose pull
docker compose up -d

echo "=== EMS Bootstrap completed at $(date) ==="
