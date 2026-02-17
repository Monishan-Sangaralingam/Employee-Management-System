# Employee Management System

Full‑stack employee management system with a Spring Boot (Java 17) backend, a React (Vite) frontend served by Nginx, and MySQL for persistence. Docker Compose orchestrates the services for local runs.

## Project Overview
- Backend: REST API + JWT auth + payroll/leave/attendance logic
- Frontend: React SPA (Vite build) served by Nginx
- Database: MySQL 8
- Orchestration: Docker Compose (MySQL healthcheck + optional Adminer)

## Architecture

```
                ┌───────────────────────────────┐
Browser         │  React (Vite) SPA             │
http://localhost│  served by Nginx              │
5173            └───────────────┬───────────────┘
                                │ HTTP
                                ▼
                         ┌───────────────┐
                         │ Spring Boot   │
                         │ ems-backend   │
                         │ :8090         │
                         └───────┬───────┘
                                 │ JDBC
                                 ▼
                         ┌───────────────┐
                         │ MySQL 8       │
                         │ employee DB   │
                         └───────────────┘

Auth: JWT (Bearer token) between Browser ↔ Backend
Compose: mysql + redis + ems-backend + ems-frontend (+ adminer optional)
Uploads: host ./data/uploads → container /data/uploads
```

## Quickstart (Docker Compose)

Create a `.env` file in the repo root (required for secrets):

```env
JWT_SECRET=change-me-to-a-long-random-secret
MYSQL_ROOT_PASSWORD=change-me
MYSQL_PASSWORD=change-me
# optional:
# MYSQL_USER=ems_user
# MYSQL_DATABASE=employee
```

Build + run everything:

```powershell
docker compose up -d --build
docker compose ps
```

Open:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8090`

Optional (DB browser via Adminer):

```powershell
docker compose --profile tools up -d --build
```

Adminer UI:
- `http://localhost:8081`
- Server: `mysql`
- Username: value of `MYSQL_USER` (default `ems_user`)
- Password: value of `MYSQL_PASSWORD`
- Database: value of `MYSQL_DATABASE` (default `employee`)

Stop:

```powershell
docker compose down
```

## Local Development (non-Docker)

Backend:

```powershell
cd ems-backend/ems-backend
./mvnw spring-boot:run
```

Frontend:

```powershell
cd ems-fullstack
npm install
npm run dev
```

## Major API Endpoints

Full list: see ENDPOINTS.md.

- Auth: `POST /api/auth/login`, `POST /api/auth/register`
- Employees: `GET/POST /api/emp`, `GET/PUT/DELETE /api/emp/{id}`, `GET /api/emp/email-id/{mail}`
- Documents: `POST /api/emp/{id}/documents`, `GET /api/emp/{id}/documents/{docId}`
- Attendance: `POST /api/attendance/me/check-in`, `POST /api/attendance/me/check-out`, `GET /api/attendance/me/monthly`
- Leave: `POST /api/leave/me/apply`, `GET /api/leave/me`, `GET /api/leave/pending`, `PUT /api/leave/{id}/decide`
- Payroll: `POST /api/payroll/employee/{employeeId}/generate`, `GET /api/payroll/{id}/pdf`
- Dashboard: `GET /api/dashboard`
- Audit: `GET /api/audit` (admin)

### Example curl

Login (gets JWT token):

```bash
curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret"}'
```

List employees:

```bash
curl -s http://localhost:8090/api/emp
```

Create employee (requires roles like ADMIN/HR):

```bash
TOKEN="<paste-jwt>"
curl -s -X POST http://localhost:8090/api/emp \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com"}'
```

Employee check-in (authenticated user with linked employee):

```bash
TOKEN="<paste-jwt>"
curl -s -X POST http://localhost:8090/api/attendance/me/check-in \
  -H "Authorization: Bearer $TOKEN"
```

## Environment Variables

Backend reads configuration from environment variables (see `application.properties`). Docker Compose sets these for local runs.

| Variable | Example | Used For |
|---|---:|---|
| `DB_URL` | `jdbc:mysql://mysql:3306/employee?...` | MySQL JDBC URL |
| `DB_USER` | `ems_user` | DB username (from `MYSQL_USER` in Compose) |
| `DB_PASSWORD` | `***` | DB password (from `MYSQL_PASSWORD` in Compose) |
| `JWT_SECRET` | `change-me-...` | JWT signing secret |
| `JWT_TTL_MS` | `3600000` | JWT TTL in ms |
| `EPF_EMPLOYEE_PERCENT` | `8` | EPF employee % |
| `EPF_EMPLOYER_PERCENT` | `12` | EPF employer % |
| `UPLOAD_DIR` | `/data/uploads` | Upload storage path |
| `MAX_FILE_SIZE` | `10MB` | Max file size |
| `MAX_REQUEST_SIZE` | `10MB` | Max multipart request size |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM tuning |

Uploads are persisted on the host:
- `./data/uploads` is mounted into the backend at `/data/uploads`.

## Production Deployment

### CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/docker-images.yml`) runs automatically on pushes to `main`:

```
GitHub Push → Build & Test (parallel) → SonarCloud → Docker Build & Push → Smoke Test → Deploy
```

### Server Prerequisites

- Linux server (Ubuntu 22.04+ recommended) with Docker and Docker Compose v2
- SSH access configured for the deploying user
- Firewall: open ports 80 (HTTP), 443 (HTTPS), 8090 (backend API)

### GitHub Repository Setup

1. **Secrets** (Settings → Secrets and variables → Actions):
   - `DEPLOY_SSH_KEY` — private SSH key for the deploy user

2. **Variables** (Settings → Secrets and variables → Actions → Variables):
   - `DEPLOY_HOST` — server IP or hostname
   - `DEPLOY_USER` — SSH username (default: `deploy`)
   - `DEPLOY_PATH` — project path on server (default: `/opt/ems`)

3. **Environment**: create a `production` environment (Settings → Environments) for deploy approval gates.

4. **Optional**: add `SONAR_TOKEN` secret for SonarCloud static analysis.

### Manual Deployment

```bash
# On the production server:
cd /opt/ems
cp .env.example .env      # edit with production values
chmod +x scripts/deploy.sh
./scripts/deploy.sh        # pulls images, starts services, checks health
```

### Docker Compose (Production)

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

This applies production overrides: restart policies, resource limits, health checks, and log rotation.

### Monitoring (Optional)

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.monitoring.yml \
  up -d
```

| Service | URL | Default Credentials |
|---|---|---|
| Prometheus | `http://<host>:9090` | — |
| Grafana | `http://<host>:3000` | admin / admin |

Alerts are configured for: backend down, MySQL down, high JVM memory, slow HTTP responses.

## Troubleshooting (Minimal)

- Docker not running: start Docker Desktop (Windows) and retry `docker compose up -d --build`.
- Ports in use: ensure `8090`, `5173`, and (optional) `8081` are free.
- MySQL takes time to start: backend waits for MySQL healthcheck; use `docker compose logs -f mysql`.
- Login succeeds but attendance/leave endpoints fail with “No employee linked…”: the logged-in `User` must be linked to an `Employee`.

## Documentation

- ENDPOINTS.md: full API reference and curl examples.
- SECURITY.md: security hardening notes and audit commands.
