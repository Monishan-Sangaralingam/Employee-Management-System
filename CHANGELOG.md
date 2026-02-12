# Changelog

All notable changes to this project will be documented in this file.

## v1.0.0-ems-core (2026-02-12)

### Added
- Authentication: JWT-based login (`/api/auth/login`) and registration (`/api/auth/register`).
- Roles/authorization: `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE` enforced via `@PreAuthorize`.
- Employee management: CRUD APIs, lookup by email, and role-based access rules.
- Attendance: employee self check-in/check-out and monthly views; HR/Admin reporting endpoints.
- Leave management: employee apply + self listing; HR/Manager pending queue and approval/decision flow.
- Payroll: payroll generation endpoints and downloadable PDF payslip.
- Employee documents: upload and download endpoints with access control.
- Dashboard endpoint providing summary data.
- Audit logging: service-level actions recorded to an audit log and browsable via admin endpoint.
- Login rate limiting: per-IP throttling for `/api/auth/login` returning HTTP 429 on excess attempts.

### DevOps / Tooling
- Docker: multi-stage backend image, Nginx frontend container, and Docker Compose orchestration (MySQL + Redis + optional Adminer).
- CI/CD: Jenkins pipeline stages for backend/frontend build+tests, Docker build/tag/push, and optional deployment.

### Testing
- Backend: unit tests for core services; integration tests using MySQL Testcontainers (auto-skip when Docker unavailable) and H2 test profile.
- Frontend: Vitest + React Testing Library unit test setup.
