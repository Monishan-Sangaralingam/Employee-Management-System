# Security Policy & Hardening Guide

This repository contains a full-stack Employee Management System (Spring Boot backend + React frontend). This document summarizes the key security controls in the codebase and how to run vulnerability checks.

## Reporting a Vulnerability

- If you discover a security issue, do not open a public issue with exploit details.
- Share a minimal reproduction and impact description with the project owner/maintainers.

## Configuration & Secrets

### Required secrets

- `JWT_SECRET` (required): used to sign JWTs.
  - Use a long, random value.
  - Do not commit it to the repo.

### Database credentials

- MySQL credentials should be provided via environment variables (for Docker Compose: via a `.env` file).
- Avoid committing default passwords in `docker-compose.yml`.

## Production Hardening Recommendations

### Hibernate DDL

- The backend uses `spring.jpa.hibernate.ddl-auto` via `HIBERNATE_DDL_AUTO`.
- For production, prefer `validate` (or `none`) rather than `create`/`create-drop`.

### CORS

- CORS allowed origins are configured via `app.cors.allowed-origin-patterns`.
- For production, set this to your exact frontend origin(s) (example: `https://ems.example.com`).

### Default admin bootstrap

- Default user creation is controlled by `CREATE_DEFAULT_USERS=true`.
- Recommendation:
  - Development only: enable to bootstrap a first admin.
  - Production: keep disabled, and provision users via controlled admin processes.

### Upload validation

Uploads are validated server-side (size + MIME allow-list).

- `app.upload.max-bytes`: maximum file size in bytes.
- `app.upload.allowed-mime-types`: comma-separated allow-list.

Recommendation: keep the allow-list tight (e.g., PDFs and common image types only).

### Login rate limiting

- `POST /api/auth/login` is rate-limited per IP (Bucket4j).
- When limit is exceeded, the API returns `429` with a JSON error.

## Dependency & Vulnerability Audits

### Backend (Maven)

The backend uses OWASP Dependency-Check.

Run (Windows):

```powershell
cd ems-backend/ems-backend
./mvnw.cmd -DskipTests org.owasp:dependency-check-maven:check
```

Notes:

- For best results, provide an NVD API key (otherwise updates may be very slow).
- A fast/offline run can be done with updates disabled:

```powershell
./mvnw.cmd -DskipTests -DautoUpdate=false org.owasp:dependency-check-maven:check
```

Report output:

- `ems-backend/ems-backend/target/dependency-check-report.html`

### Frontend (npm)

Run (Windows PowerShell):

```powershell
cd ems-fullstack
npm.cmd audit --omit=dev
```

If your PowerShell execution policy blocks `npm` (the `.ps1` shim), using `npm.cmd` avoids that.

## Operational Checklist

- Set `JWT_SECRET` in the runtime environment (required).
- Set MySQL passwords via environment variables / secret manager.
- Set `HIBERNATE_DDL_AUTO=validate` (or `none`) for production.
- Restrict `app.cors.allowed-origin-patterns` to known origins.
- Keep `CREATE_DEFAULT_USERS` disabled in production.
