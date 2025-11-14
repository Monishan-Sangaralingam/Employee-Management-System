# Employee Management System (Spring Boot + React + Docker + GHCR)

Full‑stack employee management CRUD application built with Spring Boot (Java 17), React (Vite), MySQL, and containerized with Docker. Images are published to GitHub Container Registry (GHCR) via GitHub Actions.

## Features
- Create, list, update, delete employees
- Unique email constraint
- REST API under `/api/emp`
- React SPA with routing and Bootstrap styling
- Multi‑stage Docker builds (backend & frontend)
- `docker-compose` orchestration with MySQL healthcheck
- GitHub Actions workflow builds & pushes images to GHCR

## Technology Stack
- Backend: Spring Boot 3.2 + Spring Data JPA + Hibernate
- Frontend: React 18 + Vite + Axios + React Router
- Database: MySQL 8
- Container: Docker / Docker Compose
- CI/CD: GitHub Actions + GHCR

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
Visit: `http://localhost:5173`

## Docker (Local)
Build & run everything:
```powershell
docker compose up -d --build
docker compose ps
```
Stop:
```powershell
docker compose down
```

## GitHub Container Registry (GHCR)
Images (after successful workflow run):
```
ghcr.io/<owner-lowercase>/employee-management-system-backend:latest
ghcr.io/<owner-lowercase>/employee-management-system-frontend:latest
```

### Pulling Images
If packages are private, authenticate first:
```powershell
echo YOUR_PAT | docker login ghcr.io -u <owner-lowercase> --password-stdin
```
Pull:
```powershell
docker pull ghcr.io/<owner-lowercase>/employee-management-system-backend:latest
docker pull ghcr.io/<owner-lowercase>/employee-management-system-frontend:latest
```

Run backend (using a local MySQL):
```powershell
docker run --rm -p 8080:8080 ^
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/employee?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" ^
  -e DB_USER=Monishan ^
  -e DB_PASSWORD=Moni@998130 ^
  ghcr.io/<owner-lowercase>/employee-management-system-backend:latest
```
Run frontend:
```powershell
docker run --rm -p 5173:80 ghcr.io/<owner-lowercase>/employee-management-system-frontend:latest
```

### Making Packages Public
GitHub → Repository → Packages → Select package → Package Settings → Change visibility to Public.

## CI Workflow Summary
Workflow file: `.github/workflows/docker-images.yml`
- Builds backend & frontend separately.
- Tags: `latest` and `sha-<fullsha>`.
- Requires no extra secrets (uses `GITHUB_TOKEN`).

## Environment Variables (Backend)
Configured in `application.properties` via placeholders:
```
DB_URL, DB_USER, DB_PASSWORD
```
Defaults still support local dev if env vars are missing.

## Next Improvements (Optional)
- Remove deprecated dialect property (Hibernate auto-detects).
- Add integration tests for controller.
- Add seed data via `data.sql` for demo.
- Add security (Spring Security / JWT) if authentication required.
- Add Docker build cache with `cache-from` / `cache-to` in workflow.

---
Generated & maintained with automated assistance.
